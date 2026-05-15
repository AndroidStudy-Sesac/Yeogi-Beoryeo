package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.repository.RegionalDisposalGuideRepository
import javax.inject.Inject

/**
 * 선택된 행정구역(Region)을 기반으로 지역별 배출 가이드를 가져오는 UseCase
 */
class GetRegionalDisposalGuideUseCase @Inject constructor(
    private val repository: RegionalDisposalGuideRepository
) {
    suspend operator fun invoke(region: Region): RegionalDisposalGuide? {
        return repository.getRegionalDisposalGuide(region)
    }
}