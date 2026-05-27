package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.repository.RegionalDisposalGuideRepository
import javax.inject.Inject

class GetRegionalDisposalGuideUseCase @Inject constructor(
    private val repository: RegionalDisposalGuideRepository
) {
    suspend operator fun invoke(region: Region): RegionalDisposalGuide? {
        return repository.getRegionalDisposalGuide(region)
    }
}
