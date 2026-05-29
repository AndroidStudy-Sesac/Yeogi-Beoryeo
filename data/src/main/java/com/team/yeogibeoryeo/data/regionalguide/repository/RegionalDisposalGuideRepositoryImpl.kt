package com.team.yeogibeoryeo.data.regionalguide.repository

import com.team.yeogibeoryeo.data.regionalguide.mapper.RegionalGuideMapper
import com.team.yeogibeoryeo.data.regionalguide.remote.RegionalGuideDataSource
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemDto
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.repository.RegionalDisposalGuideRepository
import javax.inject.Inject

/**
 * [RegionalDisposalGuideRepository]의 Data 계층 구현체.
 * RemoteDataSource를 통해 데이터를 패치하고, 사용자 지역 정보에 가장 적합한 배출 가이드 데이터를 선별하여 반환합니다.
 */
class RegionalDisposalGuideRepositoryImpl @Inject constructor(
    private val remoteDataSource: RegionalGuideDataSource
) : RegionalDisposalGuideRepository {

    override suspend fun getRegionalDisposalGuide(region: Region): RegionalDisposalGuide? {
        val sigungu = if (region.sido == "세종특별자치시") "없음" else region.sigungu

        if (sigungu.isNullOrBlank()) return null

        val result = remoteDataSource.fetchRegionalGuides(sigungu)

        if (result.isSuccess) {
            val dtoList = result.getOrNull() ?: emptyList()
            if (dtoList.isEmpty()) return null

            val candidateDtos = dtoList.filterBySido(region.sido)
            if (candidateDtos.isEmpty()) return null

            val eupmyeondong = region.eupmyeondong

            val targetDto = if (!eupmyeondong.isNullOrBlank()) {
                candidateDtos.find { it.dongName?.trim() == eupmyeondong.trim() }
                    ?: candidateDtos.find { it.dongName?.contains(eupmyeondong) == true }
                    ?: candidateDtos.first()
            } else {
                candidateDtos.first()
            }
            return RegionalGuideMapper.mapToDomain(region, targetDto)
        } else {
            return null
        }
    }

    private fun List<RegionalGuideItemDto>.filterBySido(
        sido: String?
    ): List<RegionalGuideItemDto> {
        if (sido.isNullOrBlank()) return this

        return filter { dto ->
            dto.sidoName?.trim() == sido.trim()
        }
    }
}
