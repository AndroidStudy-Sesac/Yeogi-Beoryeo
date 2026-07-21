package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideCandidateLookupReason
import com.team.yeogibeoryeo.presentation.regionalguide.mapper.toUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideCandidateUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.regionalGuideCandidateDisplayComparator
import com.team.yeogibeoryeo.presentation.regionalguide.model.withDuplicateDisplayDisambiguation

internal object RegionalGuideCandidatePresentationPolicy {

    fun toUiState(
        query: String,
        guides: List<RegionalDisposalGuide>,
        lookupReason: RegionalGuideCandidateLookupReason,
        isFavoriteRestore: Boolean,
        canRestoreCandidates: Boolean,
    ): RegionalGuideUiState.GuideCandidates =
        RegionalGuideUiState.GuideCandidates(
            query = query,
            reason = if (isFavoriteRestore) {
                RegionalGuideCandidateReason.FAVORITE_RESTORE_AMBIGUOUS
            } else {
                lookupReason.toUiReason()
            },
            canRestoreCandidates = canRestoreCandidates,
            candidates = guides
                .map(RegionalGuideRegionSelectionPolicy::guideWithSelectableEupmyeondong)
                .map { guide ->
                    RegionalGuideCandidateUiModel(
                        guide = guide.toUiModel(),
                        sido = guide.region.sido,
                        sigungu = guide.region.sigungu,
                        eupmyeondong = guide.region.eupmyeondong,
                    )
                }
                .withDuplicateDisplayDisambiguation()
                .sortedWith(regionalGuideCandidateDisplayComparator),
        )

    private fun RegionalGuideCandidateLookupReason.toUiReason(): RegionalGuideCandidateReason =
        when (this) {
            RegionalGuideCandidateLookupReason.MULTIPLE_CANDIDATES ->
                RegionalGuideCandidateReason.MULTIPLE_CANDIDATES

            RegionalGuideCandidateLookupReason.MULTIPLE_EXACT_MATCHES ->
                RegionalGuideCandidateReason.MULTIPLE_EXACT_MATCHES

            RegionalGuideCandidateLookupReason.FALLBACK_BECAUSE_DIRECT_MATCH_NOT_FOUND ->
                RegionalGuideCandidateReason.FALLBACK_BECAUSE_DIRECT_MATCH_NOT_FOUND
        }
}
