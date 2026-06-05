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

        if (parsedRegion?.hasSido() == true) {
            return ResolveRegionFromKeywordResult.Resolved(parsedRegion)
        }

        if (parsedRegion?.hasOnlySigungu() == true) {
            return resolveSigunguOnlyRegion(parsedRegion)
        }

        val eupmyeondongKeyword = parsedRegion?.eupmyeondong ?: keyword
        val candidates = regionOptionsRepository.findRegionsByEupmyeondongKeyword(eupmyeondongKeyword)

        return when {
            candidates.size == 1 -> ResolveRegionFromKeywordResult.Resolved(candidates.first())
            candidates.size > 1 -> ResolveRegionFromKeywordResult.Ambiguous(candidates)
            else -> resolveSigunguKeyword(
                keyword = keyword,
                fallbackRegion = parsedRegion
            )
        }
    }

    private suspend fun resolveSigunguOnlyRegion(
        parsedRegion: Region
    ): ResolveRegionFromKeywordResult {
        val sigungu = parsedRegion.sigungu.orEmpty()
        val candidates = regionOptionsRepository.findRegionsBySigunguKeyword(sigungu)

        return when {
            candidates.size == 1 -> ResolveRegionFromKeywordResult.Resolved(candidates.first())
            candidates.hasExactSigunguMatch(sigungu) -> ResolveRegionFromKeywordResult.Ambiguous(candidates)
            else -> ResolveRegionFromKeywordResult.Resolved(parsedRegion)
        }
    }

    private suspend fun resolveSigunguKeyword(
        keyword: String,
        fallbackRegion: Region? = null
    ): ResolveRegionFromKeywordResult {
        val candidates = regionOptionsRepository.findRegionsBySigunguKeyword(keyword)

        return when {
            candidates.size == 1 -> ResolveRegionFromKeywordResult.Resolved(candidates.first())
            candidates.size > 1 -> ResolveRegionFromKeywordResult.Ambiguous(candidates)
            fallbackRegion != null -> ResolveRegionFromKeywordResult.Resolved(fallbackRegion)
            else -> ResolveRegionFromKeywordResult.NotFound
        }
    }

    private fun List<Region>.hasExactSigunguMatch(
        sigungu: String
    ): Boolean =
        any { region -> region.sigungu == sigungu }

    private fun Region.hasSido(): Boolean =
        !sido.isNullOrBlank()

    private fun Region.hasOnlySigungu(): Boolean =
        sido.isNullOrBlank() &&
            !sigungu.isNullOrBlank() &&
            eupmyeondong.isNullOrBlank()
}

sealed interface ResolveRegionFromKeywordResult {
    data class Resolved(
        val region: Region
    ) : ResolveRegionFromKeywordResult

    data class Ambiguous(
        val candidates: List<Region>
    ) : ResolveRegionFromKeywordResult

    data object NotFound : ResolveRegionFromKeywordResult
}
