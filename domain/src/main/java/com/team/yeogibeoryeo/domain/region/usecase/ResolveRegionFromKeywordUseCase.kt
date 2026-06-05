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
        val eupmyeondongCandidates = regionOptionsRepository.findRegionsByEupmyeondongKeyword(
            eupmyeondongKeyword
        )
        val sigunguCandidates = regionOptionsRepository.findRegionsBySigunguKeyword(keyword)
        val candidates = (sigunguCandidates + eupmyeondongCandidates)
            .distinctBy { region ->
                listOf(
                    region.sido.orEmpty(),
                    region.sigungu.orEmpty(),
                    region.eupmyeondong.orEmpty()
                )
            }
            .sortedWith(REGION_CANDIDATE_COMPARATOR)

        return when {
            candidates.size == 1 -> ResolveRegionFromKeywordResult.Resolved(candidates.first())
            candidates.size > 1 -> ResolveRegionFromKeywordResult.Ambiguous(candidates)
            parsedRegion != null -> ResolveRegionFromKeywordResult.Resolved(parsedRegion)
            else -> ResolveRegionFromKeywordResult.NotFound
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
            sigungu.isAdministrativeDistrictName() -> ResolveRegionFromKeywordResult.NotFound
            else -> ResolveRegionFromKeywordResult.Resolved(parsedRegion)
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

    private fun String.isAdministrativeDistrictName(): Boolean =
        endsWith(ADMINISTRATIVE_DISTRICT_SUFFIX)

    private companion object {
        const val ADMINISTRATIVE_DISTRICT_SUFFIX = "구"

        val REGION_CANDIDATE_COMPARATOR = compareBy<Region>(
            { region -> region.sido.orEmpty() },
            { region -> region.sigungu.orEmpty() },
            { region -> region.eupmyeondong.orEmpty() }
        )
    }
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
