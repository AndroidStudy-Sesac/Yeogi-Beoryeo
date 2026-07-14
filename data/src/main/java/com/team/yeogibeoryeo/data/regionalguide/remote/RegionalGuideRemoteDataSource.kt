package com.team.yeogibeoryeo.data.regionalguide.remote

import com.team.yeogibeoryeo.data.core.key.AppKeyProvider
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemDto
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideFailureReason
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupException
import kotlinx.coroutines.CancellationException
import java.io.IOException
import javax.inject.Inject

/**
 * 데이터 소스를 추상화한 인터페이스.
 */
interface RegionalGuideDataSource {
    suspend fun fetchRegionalGuides(sigunguName: String): Result<List<RegionalGuideItemDto>>
}

/**
 * 행정안전부 지역별 배출 가이드 데이터 패치를 담당하는 원격 데이터 소스(Remote DataSource).
 */
class RegionalGuideRemoteDataSource @Inject constructor(
    private val apiService: RegionalGuideApiService,
    private val keyProvider: AppKeyProvider
) : RegionalGuideDataSource {

    override suspend fun fetchRegionalGuides(sigunguName: String): Result<List<RegionalGuideItemDto>> {
        return try {
            val firstPage = fetchRegionalGuidePage(
                sigunguName = sigunguName,
                pageNo = FIRST_PAGE_NO,
                numOfRows = DEFAULT_NUM_OF_ROWS,
            )
            val totalPages = firstPage.totalPages()
            val items = firstPage.items.toMutableList()

            for (nextPageNo in (firstPage.pageNo + 1)..totalPages) {
                val nextPage = fetchRegionalGuidePage(
                    sigunguName = sigunguName,
                    pageNo = nextPageNo,
                    numOfRows = DEFAULT_NUM_OF_ROWS,
                )

                items += nextPage.items
            }

            Result.success(items)
        } catch (e: IOException) {
            Result.failure(
                RegionalGuideLookupException(
                    reason = RegionalGuideFailureReason.NETWORK,
                    cause = e
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchRegionalGuidePage(
        sigunguName: String,
        pageNo: Int,
        numOfRows: Int,
    ): RegionalGuidePage {
        val response = apiService.getRegionalGuides(
            serviceKey = keyProvider.publicDataServiceKey,
            pageNo = pageNo,
            numOfRows = numOfRows,
            sigunguName = sigunguName,
        )

        if (!response.isSuccessful) {
            throw RegionalGuideLookupException(
                reason = RegionalGuideFailureReason.API,
            )
        }

        val body = response.body()?.response?.body
            ?: throw RegionalGuideLookupException(
                reason = RegionalGuideFailureReason.API,
            )

        return RegionalGuidePage(
            items = body.items?.item.orEmpty(),
            pageNo = pageNo,
            numOfRows = body.numOfRows
                ?.coerceAtMost(numOfRows)
                ?: numOfRows,
            totalCount = body.totalCount,
        )
    }

    private fun RegionalGuidePage.totalPages(): Int {
        if (totalCount == null || totalCount <= 0 || numOfRows <= 0) {
            return pageNo
        }

        return ((totalCount + numOfRows - 1) / numOfRows)
            .coerceAtLeast(pageNo)
    }

    private data class RegionalGuidePage(
        val items: List<RegionalGuideItemDto>,
        val pageNo: Int,
        val numOfRows: Int,
        val totalCount: Int?,
    )

    private companion object {
        const val FIRST_PAGE_NO = 1
        const val DEFAULT_NUM_OF_ROWS = 100
    }
}
