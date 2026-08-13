package com.team.yeogibeoryeo.data.regionalguide.remote

import com.team.yeogibeoryeo.data.core.key.AppKeyProvider
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemDto
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideFailureReason
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.IOException
import javax.inject.Inject

/**
 * 데이터 소스를 추상화한 인터페이스.
 */
interface RegionalGuideDataSource {
    suspend fun fetchRegionalGuides(sigunguName: String): Result<RegionalGuideFetchResult>
}

data class RegionalGuideFetchResult(
    val items: List<RegionalGuideItemDto>,
    val partialReason: RegionalGuidePartialResultReason? = null,
) {
    val isPartial: Boolean
        get() = partialReason != null
}

enum class RegionalGuidePartialResultReason {
    PAGE_LIMIT,
    TIMEOUT,
    NETWORK,
    API,
    INCONSISTENT_RESPONSE,
    UNKNOWN,
}

/**
 * 행정안전부 지역별 배출 가이드 데이터 패치를 담당하는 원격 데이터 소스(Remote DataSource).
 */
class RegionalGuideRemoteDataSource @Inject constructor(
    private val apiService: RegionalGuideApiService,
    private val keyProvider: AppKeyProvider
) : RegionalGuideDataSource {

    override suspend fun fetchRegionalGuides(sigunguName: String): Result<RegionalGuideFetchResult> {
        val items = mutableListOf<RegionalGuideItemDto>()
        var firstPageFetched = false

        return try {
            val result = withTimeout(TOTAL_FETCH_TIMEOUT_MILLIS) {
                val firstPage = fetchRegionalGuidePageWithTimeout(
                    sigunguName = sigunguName,
                    pageNo = FIRST_PAGE_NO,
                    numOfRows = DEFAULT_NUM_OF_ROWS,
                )
                items += firstPage.items
                firstPageFetched = true

                if (firstPage.hasMoreItemsThanTotalCount()) {
                    return@withTimeout items.toPartialResult(
                        RegionalGuidePartialResultReason.INCONSISTENT_RESPONSE,
                    )
                }

                val lastPageNo = firstPage.lastPageNo()
                for (nextPageNo in (firstPage.pageNo + 1)..lastPageNo) {
                    val nextPage = try {
                        fetchRegionalGuidePageWithTimeout(
                            sigunguName = sigunguName,
                            pageNo = nextPageNo,
                            numOfRows = DEFAULT_NUM_OF_ROWS,
                        )
                    } catch (_: TimeoutCancellationException) {
                        return@withTimeout items.toPartialResult(
                            RegionalGuidePartialResultReason.TIMEOUT,
                        )
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        return@withTimeout items.toPartialResult(exception.toPartialResultReason())
                    }

                    items += nextPage.items

                    if (firstPage.hasMoreItemsThanTotalCount(items.size)) {
                        return@withTimeout items.toPartialResult(
                            RegionalGuidePartialResultReason.INCONSISTENT_RESPONSE,
                        )
                    }

                    if (items.size == firstPage.totalCount.orZero()) {
                        return@withTimeout RegionalGuideFetchResult(items)
                    }

                    if (nextPage.items.isEmpty()) {
                        return@withTimeout items.toPartialResult(
                            RegionalGuidePartialResultReason.INCONSISTENT_RESPONSE,
                        )
                    }
                }

                when {
                    firstPage.reachesPageLimit() -> items.toPartialResult(
                        RegionalGuidePartialResultReason.PAGE_LIMIT,
                    )
                    items.size < firstPage.totalCount.orZero() -> items.toPartialResult(
                        RegionalGuidePartialResultReason.INCONSISTENT_RESPONSE,
                    )
                    else -> RegionalGuideFetchResult(items)
                }
            }

            Result.success(result)
        } catch (e: TimeoutCancellationException) {
            if (firstPageFetched) {
                Result.success(items.toPartialResult(RegionalGuidePartialResultReason.TIMEOUT))
            } else {
                Result.failure(e.toLookupException())
            }
        } catch (e: IOException) {
            Result.failure(e.toLookupException())
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

    private suspend fun fetchRegionalGuidePageWithTimeout(
        sigunguName: String,
        pageNo: Int,
        numOfRows: Int,
    ): RegionalGuidePage = withTimeout(PAGE_FETCH_TIMEOUT_MILLIS) {
        fetchRegionalGuidePage(sigunguName, pageNo, numOfRows)
    }

    private fun RegionalGuidePage.lastPageNo(): Int {
        val totalPages = totalPages() ?: return pageNo

        return minOf(totalPages.coerceAtLeast(pageNo), pageNo + MAX_PAGE_COUNT - 1)
    }

    private fun RegionalGuidePage.totalPages(): Int? {
        if (totalCount == null || totalCount <= 0 || numOfRows <= 0) {
            return null
        }

        return (totalCount / numOfRows) + if (totalCount % numOfRows == 0) 0 else 1
    }

    private fun RegionalGuidePage.reachesPageLimit(): Boolean =
        totalPages()?.let { totalPages -> totalPages > MAX_PAGE_COUNT } == true

    private fun RegionalGuidePage.hasMoreItemsThanTotalCount(): Boolean =
        hasMoreItemsThanTotalCount(items.size)

    private fun RegionalGuidePage.hasMoreItemsThanTotalCount(itemCount: Int): Boolean =
        totalCount != null && itemCount > totalCount

    private fun Int?.orZero(): Int = this ?: 0

    private fun List<RegionalGuideItemDto>.toPartialResult(
        reason: RegionalGuidePartialResultReason,
    ): RegionalGuideFetchResult = RegionalGuideFetchResult(this.toList(), reason)

    private fun Throwable.toPartialResultReason(): RegionalGuidePartialResultReason =
        when (this) {
            is IOException -> RegionalGuidePartialResultReason.NETWORK
            is RegionalGuideLookupException ->
                when (reason) {
                    RegionalGuideFailureReason.NETWORK -> RegionalGuidePartialResultReason.NETWORK
                    RegionalGuideFailureReason.API -> RegionalGuidePartialResultReason.API
                    RegionalGuideFailureReason.UNKNOWN -> RegionalGuidePartialResultReason.UNKNOWN
                }
            else -> RegionalGuidePartialResultReason.UNKNOWN
        }

    private fun Throwable.toLookupException(): RegionalGuideLookupException =
        RegionalGuideLookupException(
            reason = if (this is IOException) {
                RegionalGuideFailureReason.NETWORK
            } else {
                RegionalGuideFailureReason.UNKNOWN
            },
            cause = this,
        )

    private data class RegionalGuidePage(
        val items: List<RegionalGuideItemDto>,
        val pageNo: Int,
        val numOfRows: Int,
        val totalCount: Int?,
    )

    private companion object {
        const val FIRST_PAGE_NO = 1
        const val DEFAULT_NUM_OF_ROWS = 100
        const val MAX_PAGE_COUNT = 5
        const val PAGE_FETCH_TIMEOUT_MILLIS = 2_000L
        const val TOTAL_FETCH_TIMEOUT_MILLIS = 5_000L
    }
}
