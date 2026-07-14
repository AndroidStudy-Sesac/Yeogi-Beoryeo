package com.team.yeogibeoryeo.presentation.regionalguide

internal fun RegionalGuideUiState.GuideCandidates.shouldShowCollectionTypeSelectionPanel(): Boolean =
    when (reason) {
        RegionalGuideCandidateReason.FALLBACK_BECAUSE_DIRECT_MATCH_NOT_FOUND ->
            hasOnlyCollectionTypeSelectionCandidates()

        RegionalGuideCandidateReason.MULTIPLE_CANDIDATES ->
            isOverallCollectionTypeSelection()

        RegionalGuideCandidateReason.MULTIPLE_EXACT_MATCHES,
        RegionalGuideCandidateReason.FAVORITE_RESTORE_AMBIGUOUS -> false
    }

private fun RegionalGuideUiState.GuideCandidates.isOverallCollectionTypeSelection(): Boolean =
    hasOnlyCollectionTypeSelectionCandidates() &&
        candidates.all { candidate -> candidate.isOverallCollectionTypeCandidate }

private fun RegionalGuideUiState.GuideCandidates.hasOnlyCollectionTypeSelectionCandidates(): Boolean =
    candidates.size > 1 &&
        candidates.all { candidate -> candidate.isCollectionTypeSelectionCandidate } &&
        candidates
            .mapNotNull { candidate -> candidate.guide.disposalPlaceType?.trim() }
            .distinct()
            .size > 1
