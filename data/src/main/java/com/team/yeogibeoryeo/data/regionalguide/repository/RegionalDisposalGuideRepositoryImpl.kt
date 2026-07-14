package com.team.yeogibeoryeo.data.regionalguide.repository

import com.team.yeogibeoryeo.data.regionalguide.mapper.RegionalGuideMapper
import com.team.yeogibeoryeo.data.regionalguide.remote.RegionalGuideDataSource
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemDto
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideQuery
import com.team.yeogibeoryeo.domain.regionalguide.repository.RegionalDisposalGuideRepository
import javax.inject.Inject

/**
 * [RegionalDisposalGuideRepository]의 Data 계층 구현체.
 * RemoteDataSource를 통해 데이터를 패치하고 DTO를 domain 후보 목록으로 변환합니다.
 */
class RegionalDisposalGuideRepositoryImpl @Inject constructor(
    private val remoteDataSource: RegionalGuideDataSource
) : RegionalDisposalGuideRepository {

    private val candidatesCache = mutableMapOf<String, List<RegionalGuideItemDto>>()

    override suspend fun getRegionalDisposalGuideCandidates(
        query: RegionalGuideQuery
    ): Result<List<RegionalDisposalGuide>> {
        return fetchRegionalGuideItems(query.sigunguQuery)
            .map { dtoList ->
                dtoList.map { dto ->
                    RegionalGuideMapper.mapToDomain(
                        baseRegion = query.displayRegion.copy(
                            sido = null,
                            sigungu = null
                        ),
                        dto = dto
                    )
                }
            }
    }

    private suspend fun fetchRegionalGuideItems(
        sigunguQuery: String
    ): Result<List<RegionalGuideItemDto>> {
        candidatesCache[sigunguQuery]?.let { cachedItems ->
            return Result.success(cachedItems)
        }

        return remoteDataSource.fetchRegionalGuides(sigunguQuery)
            .onSuccess { items ->
                candidatesCache[sigunguQuery] = items
            }
    }
}
