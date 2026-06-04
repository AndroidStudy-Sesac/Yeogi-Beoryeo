package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupResult
import com.team.yeogibeoryeo.domain.regionalguide.repository.RegionalDisposalGuideRepository
import javax.inject.Inject

class GetRegionalDisposalGuideUseCase @Inject constructor(
    private val repository: RegionalDisposalGuideRepository,
    private val normalizeRegionalGuideQueryUseCase: NormalizeRegionalGuideQueryUseCase,
    private val selectRegionalGuideCandidateUseCase: SelectRegionalGuideCandidateUseCase
) {
    suspend operator fun invoke(region: Region): RegionalGuideLookupResult {
        val query = normalizeRegionalGuideQueryUseCase(region)
            ?: return RegionalGuideLookupResult.NotFound

        val candidates = repository.getRegionalDisposalGuideCandidates(query)
            .getOrElse { throwable ->
                return RegionalGuideLookupResult.Failure(throwable)
            }

        return selectRegionalGuideCandidateUseCase(
            candidates = candidates,
            query = query
        )
    }
}
