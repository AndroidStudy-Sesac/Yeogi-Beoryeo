package com.team.yeogibeoryeo.data.regionalguide.remote

import com.team.yeogibeoryeo.data.core.key.AppKeyProvider
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemDto
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideFailureReason
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupException
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

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
            val response = apiService.getRegionalGuides(
                serviceKey = keyProvider.publicDataServiceKey,
                sigunguName = sigunguName
            )

            if (response.isSuccessful) {
                val items = response.body()?.response?.body?.items?.item ?: emptyList()
                Result.success(items)
            } else {
                Result.failure(
                    RegionalGuideLookupException(
                        reason = RegionalGuideFailureReason.API
                    )
                )
            }
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
}
