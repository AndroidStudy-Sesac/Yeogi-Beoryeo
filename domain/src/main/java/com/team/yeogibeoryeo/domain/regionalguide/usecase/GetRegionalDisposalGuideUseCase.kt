package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteKey
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.usecase.FindAdminDongCandidatesForLegalDongUseCase
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideCandidateLookupReason
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideFailureReason
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLegacyRegionCompatibilityPolicy
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupException
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupResult
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideQuery
import com.team.yeogibeoryeo.domain.regionalguide.repository.RegionalDisposalGuideRepository
import javax.inject.Inject

class GetRegionalDisposalGuideUseCase @Inject constructor(
    private val repository: RegionalDisposalGuideRepository,
    private val normalizeRegionalGuideQueryUseCase: NormalizeRegionalGuideQueryUseCase,
    private val selectRegionalGuideCandidateUseCase: SelectRegionalGuideCandidateUseCase,
    private val findAdminDongCandidatesForLegalDongUseCase: FindAdminDongCandidatesForLegalDongUseCase
) {
    suspend operator fun invoke(
        region: Region,
        preferredTargetRegionName: String? = null,
        preferredManagementZoneName: String? = null,
        favoriteKey: RegionalGuideFavoriteKey? = null,
    ): RegionalGuideLookupResult {
        val legacyReplacementRegions = RegionalGuideLegacyRegionCompatibilityPolicy.replacementRegions(region)
        if (legacyReplacementRegions.isNotEmpty()) {
            return lookupLegacyReplacementRegions(
                regions = legacyReplacementRegions,
                preferredTargetRegionName = preferredTargetRegionName,
                preferredManagementZoneName = preferredManagementZoneName,
                favoriteKey = favoriteKey,
                isSplitReplacement = RegionalGuideLegacyRegionCompatibilityPolicy.isSplitReplacement(region),
            )
        }

        val query = normalizeRegionalGuideQueryUseCase(region)
            ?: return RegionalGuideLookupResult.NotFound

        return lookupRegion(
            query = query,
            preferredTargetRegionName = preferredTargetRegionName,
            preferredManagementZoneName = preferredManagementZoneName,
            favoriteKey = favoriteKey,
        )
    }

    private suspend fun lookupLegacyReplacementRegions(
        regions: List<Region>,
        preferredTargetRegionName: String?,
        preferredManagementZoneName: String?,
        favoriteKey: RegionalGuideFavoriteKey?,
        isSplitReplacement: Boolean,
    ): RegionalGuideLookupResult {
        val results = regions.mapNotNull { replacementRegion ->
            val query = normalizeRegionalGuideQueryUseCase(replacementRegion)
                ?: return@mapNotNull null

            lookupRegion(
                query = query,
                preferredTargetRegionName = preferredTargetRegionName.takeUnless { isSplitReplacement },
                preferredManagementZoneName = preferredManagementZoneName.takeUnless { isSplitReplacement },
                favoriteKey = favoriteKey.takeUnless { isSplitReplacement },
            )
        }

        val failure = results.firstOrNull { result -> result is RegionalGuideLookupResult.Failure }
        if (failure != null) return failure

        val guides = results.flatMap { result ->
            when (result) {
                is RegionalGuideLookupResult.Success -> listOf(result.guide)
                is RegionalGuideLookupResult.Candidates -> result.guides
                RegionalGuideLookupResult.CandidateNotFound,
                RegionalGuideLookupResult.NotFound,
                is RegionalGuideLookupResult.Failure -> emptyList()
            }
        }

        val distinctGuides = guides.distinctBy { guide -> guide.toLegacyCandidateKey() }

        return when {
            distinctGuides.isEmpty() -> RegionalGuideLookupResult.CandidateNotFound
            isSplitReplacement -> RegionalGuideLookupResult.Candidates(
                guides = distinctGuides,
                reason = RegionalGuideCandidateLookupReason.MULTIPLE_CANDIDATES,
            )
            distinctGuides.size == 1 -> RegionalGuideLookupResult.Success(distinctGuides.first())
            else -> RegionalGuideLookupResult.Candidates(
                guides = distinctGuides,
                reason = RegionalGuideCandidateLookupReason.MULTIPLE_CANDIDATES,
            )
        }
    }

    private suspend fun lookupRegion(
        query: RegionalGuideQuery,
        preferredTargetRegionName: String?,
        preferredManagementZoneName: String?,
        favoriteKey: RegionalGuideFavoriteKey?,
    ): RegionalGuideLookupResult {
        val candidates = repository.getRegionalDisposalGuideCandidates(query)
            .getOrElse { throwable ->
                return RegionalGuideLookupResult.Failure(
                    reason = throwable.toFailureReason(),
                    throwable = throwable
                )
            }
        val adminDongCandidates = findAdminDongCandidatesForLegalDongUseCase(query.displayRegion)

        return selectRegionalGuideCandidateUseCase(
            candidates = candidates,
            query = query,
            preferredTargetRegionName = preferredTargetRegionName,
            preferredManagementZoneName = preferredManagementZoneName,
            favoriteKey = favoriteKey,
            mappedAdminDongCandidates = adminDongCandidates,
        )
    }

    private fun RegionalDisposalGuide.toLegacyCandidateKey(): List<String> =
        listOf(
            region.sido.orEmpty(),
            region.sigungu.orEmpty(),
            region.eupmyeondong.orEmpty(),
            targetRegionName.orEmpty(),
            managementZoneName.orEmpty(),
        )

    private fun Throwable.toFailureReason(): RegionalGuideFailureReason {
        return when (this) {
            is RegionalGuideLookupException -> reason
            else -> RegionalGuideFailureReason.UNKNOWN
        }
    }
}
