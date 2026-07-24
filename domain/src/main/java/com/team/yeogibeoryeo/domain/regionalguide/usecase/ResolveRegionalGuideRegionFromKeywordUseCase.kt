package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.model.RegionCandidateComparator
import com.team.yeogibeoryeo.domain.region.model.RegionSidoAliasPolicy
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import com.team.yeogibeoryeo.domain.region.usecase.ResolveRegionFromKeywordResult
import com.team.yeogibeoryeo.domain.region.usecase.ResolveRegionFromKeywordUseCase
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideEupmyeondongNamePolicy
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLegacyRegionCompatibilityPolicy
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideRegionKeyNormalizer
import javax.inject.Inject

class ResolveRegionalGuideRegionFromKeywordUseCase @Inject constructor(
    private val resolveRegionFromKeywordUseCase: ResolveRegionFromKeywordUseCase,
    private val regionOptionsRepository: RegionOptionsRepository,
) {
    suspend operator fun invoke(keyword: String): ResolveRegionFromKeywordResult {
        val resolvedResult = when (val result = resolveRegionFromKeywordUseCase(keyword)) {
            is ResolveRegionFromKeywordResult.Resolved ->
                result.resolveWithRegionalGuideLegacyCompatibility(keyword)

            is ResolveRegionFromKeywordResult.Ambiguous ->
                result.resolveWithRegionalGuideLegacyCompatibility(keyword)

            ResolveRegionFromKeywordResult.NotFound ->
                RegionalGuideLegacyRegionCompatibilityPolicy
                    .keywordReplacementRegions(keyword)
                    .toResolveResult()
        }

        return resolvedResult
            .replaceLegalDongAliasesWithRegionalGuideCandidates(
                aliasLookup = resolvedResult.findRegionalGuideAliasLookup()
            )
            .filterUnavailableRegionalGuideCandidates()
            .toRegionalGuideDisplayResult()
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

    private suspend fun ResolveRegionFromKeywordResult.findRegionalGuideAliasLookup():
        RegionalGuideAliasLookup {
        val aliasSourceRegions = candidates()
            .filter { region ->
                RegionalGuideEupmyeondongNamePolicy.isNumberOmittedDongKeyword(
                    region.eupmyeondong
                )
            }
        if (aliasSourceRegions.isEmpty()) return RegionalGuideAliasLookup.Empty

        val aliasKeywords = aliasSourceRegions
            .mapNotNull { region -> region.eupmyeondong?.trim()?.takeIf(String::isNotBlank) }
            .toSet()
        val regionalGuideCandidates = aliasKeywords
            .flatMap { aliasKeyword ->
                regionOptionsRepository
                    .findAvailableRegionalGuideRegionsByEupmyeondongKeyword(aliasKeyword)
            }
            .filter { candidate ->
                aliasSourceRegions.any { source ->
                    candidate.isSameRegionalGuideScope(source)
                }
            }

        return RegionalGuideAliasLookup(
            aliasKeywords = aliasKeywords,
            candidates = regionalGuideCandidates,
        )
    }

    private fun ResolveRegionFromKeywordResult.replaceLegalDongAliasesWithRegionalGuideCandidates(
        aliasLookup: RegionalGuideAliasLookup,
    ): ResolveRegionFromKeywordResult {
        val aliasScopeCandidates = aliasLookup.candidates
            .filter { region ->
                aliasLookup.aliasKeywords.any { aliasKeyword ->
                    RegionalGuideEupmyeondongNamePolicy.isNumberedDongAliasOf(
                        name = region.eupmyeondong,
                        keyword = aliasKeyword,
                    )
                }
            }
        if (aliasScopeCandidates.isEmpty()) return this

        val legalDongCandidates = candidates()
            .filterNot { region ->
                aliasScopeCandidates.any { aliasCandidate ->
                    region.isSameRegionalGuideScope(aliasCandidate)
                } &&
                    aliasLookup.aliasKeywords.any { aliasKeyword ->
                        RegionalGuideEupmyeondongNamePolicy.isSameName(
                            first = region.eupmyeondong,
                            second = aliasKeyword,
                        )
                    }
            }

        return (legalDongCandidates + aliasLookup.candidates)
            .distinctRegions()
            .toResolveResult()
    }

    private suspend fun ResolveRegionFromKeywordResult.filterUnavailableRegionalGuideCandidates():
        ResolveRegionFromKeywordResult {
        val candidates = candidates()
        val selectableCandidates = candidates.filter { region ->
            !region.eupmyeondong.isNullOrBlank()
        }
        if (selectableCandidates.isEmpty()) return this

        val availableCandidates = regionOptionsRepository
            .filterAvailableRegionalGuideRegions(selectableCandidates)
        val scopeCandidates = candidates - selectableCandidates.toSet()

        return (scopeCandidates + availableCandidates)
            .distinctRegions()
            .toResolveResult()
    }

    private fun ResolveRegionFromKeywordResult.candidates(): List<Region> =
        when (this) {
            is ResolveRegionFromKeywordResult.Resolved -> listOf(region)
            is ResolveRegionFromKeywordResult.Ambiguous -> candidates
            ResolveRegionFromKeywordResult.NotFound -> emptyList()
        }

    private fun Region.isSameRegionalGuideScope(other: Region): Boolean {
        return RegionSidoAliasPolicy.isSameSido(
            requestedSido = sido,
            requestedSigungu = sigungu,
            candidateSido = other.sido,
            candidateSigungu = other.sigungu,
        ) &&
            sigungu?.let(RegionalGuideRegionKeyNormalizer::normalizeSigungu) ==
            other.sigungu?.let(RegionalGuideRegionKeyNormalizer::normalizeSigungu)
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
        val REGION_CANDIDATE_COMPARATOR = RegionCandidateComparator
    }

    private data class RegionalGuideAliasLookup(
        val aliasKeywords: Set<String>,
        val candidates: List<Region>,
    ) {
        companion object {
            val Empty = RegionalGuideAliasLookup(
                aliasKeywords = emptySet(),
                candidates = emptyList(),
            )
        }
    }
}
