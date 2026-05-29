package com.team.yeogibeoryeo.domain.region.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import com.team.yeogibeoryeo.domain.region.repository.RegionRepository
import javax.inject.Inject

class ResolveRegionFromKeywordUseCase @Inject constructor(
    private val repository: RegionRepository,
    private val regionOptionsRepository: RegionOptionsRepository
) {
    suspend operator fun invoke(keyword: String): ResolveRegionFromKeywordResult {
        val parsedRegion = repository.resolveRegionFromKeyword(keyword)

        if (parsedRegion?.hasUpperRegion() == true) {
            return ResolveRegionFromKeywordResult.Resolved(parsedRegion)
        }

        val eupmyeondongKeyword = parsedRegion?.eupmyeondong ?: keyword
        val candidates = regionOptionsRepository.findRegionsByEupmyeondongKeyword(eupmyeondongKeyword)

        return when {
            candidates.size == 1 -> ResolveRegionFromKeywordResult.Resolved(candidates.first())
            candidates.size > 1 -> ResolveRegionFromKeywordResult.Ambiguous
            parsedRegion != null -> ResolveRegionFromKeywordResult.Resolved(parsedRegion)
            else -> ResolveRegionFromKeywordResult.NotFound
        }
    }

    private fun Region.hasUpperRegion(): Boolean =
        !sido.isNullOrBlank() || !sigungu.isNullOrBlank()
}

sealed interface ResolveRegionFromKeywordResult {
    data class Resolved(
        val region: Region
    ) : ResolveRegionFromKeywordResult

    data object Ambiguous : ResolveRegionFromKeywordResult

    data object NotFound : ResolveRegionFromKeywordResult
}
