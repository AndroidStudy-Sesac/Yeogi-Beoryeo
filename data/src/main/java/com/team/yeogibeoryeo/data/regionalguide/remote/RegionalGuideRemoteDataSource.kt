package com.team.yeogibeoryeo.data.regionalguide.remote

import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemDto
import javax.inject.Inject

/**
 * 데이터 소스를 추상화한 인터페이스.
 */
interface RegionalGuideDataSource {
    suspend fun fetchRegionalGuides(sigunguName: String): Result<List<RegionalGuideItemDto>>
}

/**
 * 행정안전부 지역별 배출 가이드 데이터 패치를 담당하는 원격 데이터 소스(Remote DataSource).
 * Retrofit 통신 결과를 안전한 [Result] 래퍼로 변환하여 Repository 계층으로 전달합니다.
 */
class RegionalGuideRemoteDataSource @Inject constructor(
    private val apiService: RegionalGuideApiService
) : RegionalGuideDataSource {
    // TODO: BuildConfig 또는 local.properties 기반 API Key 주입 방식 적용 예정
    private val TEMP_API_KEY = "PUBLIC_DATA_SERVICE_KEY"

    /**
     * 시군구명을 기반으로 배출 가이드 리스트를 비동기 요청합니다.
     *
     * @param sigunguName 검색할 시군구명 (예: "강남구")
     * @return 성공 시 데이터 리스트를 포함한 Result.success, 실패 시 예외를 포함한 Result.failure 반환
     */
    override suspend fun fetchRegionalGuides(sigunguName: String): Result<List<RegionalGuideItemDto>> {
        return try {
            val response = apiService.getRegionalGuides(
                serviceKey = TEMP_API_KEY,
                sigunguName = sigunguName
            )

            if (response.isSuccessful) {
                val items = response.body()?.response?.body?.items?.item ?: emptyList()
                Result.success(items)
            } else {
                Result.failure(Exception("API 통신 실패 [HTTP ${response.code()}]: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}