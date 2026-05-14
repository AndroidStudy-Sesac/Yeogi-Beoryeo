package com.team.yeogibeoryeo.domain.region.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionRepository
import javax.inject.Inject

/**
 * 사용자 검색어 기반으로 행정구역을 분석하는 UseCase
 * [비동기 방식] 향후 검색 API 연동을 고려하여 코루틴 환경에서 결과를 반환합니다.
 */
class ResolveRegionFromKeywordUseCase @Inject constructor(
    private val repository: RegionRepository
) {
    suspend operator fun invoke(keyword: String): Region? {
        return repository.resolveRegionFromKeyword(keyword)
    }
}