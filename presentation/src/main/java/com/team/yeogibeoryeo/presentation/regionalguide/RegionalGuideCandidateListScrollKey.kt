package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionSearchCandidateUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideCandidateUiModel

internal fun RegionalGuideUiState.Ambiguous.candidateListScrollKey(): String =
    candidates.candidateListScrollKey { candidate -> candidate.stableKey }

internal fun RegionalGuideUiState.GuideCandidates.candidateListScrollKey(): String =
    candidates.candidateListScrollKey { candidate -> candidate.stableKey }

internal fun List<RegionSearchCandidateUiModel>.regionSearchCandidateListScrollKey(): String =
    candidateListScrollKey { candidate -> candidate.stableKey }

internal fun List<RegionalGuideCandidateUiModel>.regionalGuideCandidateListScrollKey(): String =
    candidateListScrollKey { candidate -> candidate.stableKey }

private fun <T> List<T>.candidateListScrollKey(key: (T) -> Any): String =
    mapIndexed { index, candidate -> "${key(candidate)}#$index" }
        .joinToString(separator = "|")
