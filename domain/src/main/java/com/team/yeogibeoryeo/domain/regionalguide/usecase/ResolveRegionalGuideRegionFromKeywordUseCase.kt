package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.usecase.ResolveRegionFromKeywordResult
import com.team.yeogibeoryeo.domain.region.usecase.ResolveRegionFromKeywordUseCase
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideEupmyeondongNamePolicy
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLegacyRegionCompatibilityPolicy
import javax.inject.Inject

class ResolveRegionalGuideRegionFromKeywordUseCase @Inject constructor(
    private val resolveRegionFromKeywordUseCase: ResolveRegionFromKeywordUseCase
) {
    suspend operator fun invoke(keyword: String): ResolveRegionFromKeywordResult {
        return when (val result = resolveRegionFromKeywordUseCase(keyword)) {
            is ResolveRegionFromKeywordResult.Resolved ->
                result.resolveWithRegionalGuideLegacyCompatibility(keyword)

            is ResolveRegionFromKeywordResult.Ambiguous ->
                result.resolveWithRegionalGuideLegacyCompatibility(keyword)

            ResolveRegionFromKeywordResult.NotFound ->
                RegionalGuideLegacyRegionCompatibilityPolicy
                    .keywordReplacementRegions(keyword)
                    .toResolveResult()
        }.toRegionalGuideDisplayResult()
    }

    private fun ResolveRegionFromKeywordResult.Resolved.resolveWithRegionalGuideLegacyCompatibility(
        keyword: String
    ): ResolveRegionFromKeywordResult {
        val replacementRegions =
            RegionalGuideLegacyRegionCompatibilityPolicy.replacementRegions(region)
                .ifEmpty {
                    RegionalGuideLegacyRegionCompatibilityPolicy.keywordReplacementRegions(keyword)
                }

        return replacementRegions
            .takeIf { regions -> regions.isNotEmpty() }
            ?.toResolveResult()
            ?: this
    }

    private fun ResolveRegionFromKeywordResult.Ambiguous.resolveWithRegionalGuideLegacyCompatibility(
        keyword: String
    ): ResolveRegionFromKeywordResult {
        val replacementRegions =
            candidates.flatMap(RegionalGuideLegacyRegionCompatibilityPolicy::replacementRegions) +
                RegionalGuideLegacyRegionCompatibilityPolicy.keywordReplacementRegions(keyword)

        return (candidates + replacementRegions)
            .distinctRegions()
            .toResolveResult()
    }

    private fun List<Region>.toResolveResult(): ResolveRegionFromKeywordResult =
        when (size) {
            0 -> ResolveRegionFromKeywordResult.NotFound
            1 -> ResolveRegionFromKeywordResult.Resolved(first())
            else -> ResolveRegionFromKeywordResult.Ambiguous(
                candidates = distinctRegions().sortedWith(REGION_CANDIDATE_COMPARATOR)
            )
        }

    private fun List<Region>.distinctRegions(): List<Region> =
        distinctBy { region ->
            listOf(
                region.sido.orEmpty(),
                region.sigungu.orEmpty(),
                region.eupmyeondong.orEmpty()
            )
        }

    private fun ResolveRegionFromKeywordResult.toRegionalGuideDisplayResult(): ResolveRegionFromKeywordResult =
        when (this) {
            is ResolveRegionFromKeywordResult.Resolved ->
                ResolveRegionFromKeywordResult.Resolved(region.toRegionalGuideDisplayRegion())

            is ResolveRegionFromKeywordResult.Ambiguous ->
                ResolveRegionFromKeywordResult.Ambiguous(
                    candidates = candidates
                        .map { region -> region.toRegionalGuideDisplayRegion() }
                        .distinctRegions()
                        .sortedWith(REGION_CANDIDATE_COMPARATOR)
                )

            ResolveRegionFromKeywordResult.NotFound -> ResolveRegionFromKeywordResult.NotFound
        }

    private fun Region.toRegionalGuideDisplayRegion(): Region =
        copy(
            eupmyeondong = RegionalGuideEupmyeondongNamePolicy
                .toApiCompatibleDisplayName(eupmyeondong)
        )

    private companion object {
        val REGION_CANDIDATE_COMPARATOR = compareBy<Region>(
            { region -> region.sido.orEmpty() },
            { region -> region.sigungu.orEmpty() },
            { region -> region.eupmyeondong.orEmpty() }
        )
    }
}
