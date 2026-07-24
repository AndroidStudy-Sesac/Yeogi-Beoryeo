package com.team.yeogibeoryeo.presentation.regionalguide

import androidx.annotation.StringRes
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionSearchCandidateUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideCandidateUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideUiModel

sealed interface RegionalGuideUiState {
    data object Idle : RegionalGuideUiState

    data class Loading(
        val query: String,
        val canRestoreCandidates: Boolean = false,
        val regionNameParts: List<String>? = null,
    ) : RegionalGuideUiState

    data class Success(
        val query: String,
        val guide: RegionalGuideUiModel,
        val isFavorite: Boolean = false,
        val canRestoreCandidates: Boolean = false,
    ) : RegionalGuideUiState

    data class Empty(
        val query: String,
        @param:StringRes val titleResId: Int = R.string.regional_guide_empty_default_title,
        @param:StringRes val messageResId: Int = R.string.regional_guide_empty_default_message,
        val action: RegionalGuideEmptyActionUiModel? = null,
    ) : RegionalGuideUiState

    data class Ambiguous(
        val query: String,
        val candidates: List<RegionSearchCandidateUiModel>,
        val candidateListScrollPosition: RegionalGuideCandidateListScrollPosition =
            RegionalGuideCandidateListScrollPosition.Initial,
    ) : RegionalGuideUiState

    data class GuideCandidates(
        val query: String,
        val reason: RegionalGuideCandidateReason,
        val candidates: List<RegionalGuideCandidateUiModel>,
        val canRestoreCandidates: Boolean = false,
        val candidateListScrollPosition: RegionalGuideCandidateListScrollPosition =
            RegionalGuideCandidateListScrollPosition.Initial,
    ) : RegionalGuideUiState

    data class Error(
        val query: String,
        val message: RegionalGuideErrorMessage,
        val canRestoreCandidates: Boolean = false,
    ) : RegionalGuideUiState
}

sealed interface RegionalGuideErrorMessage {
    data class Dynamic(
        val value: String,
    ) : RegionalGuideErrorMessage

    data class Resource(
        @param:StringRes val resId: Int,
    ) : RegionalGuideErrorMessage
}

data class RegionalGuideEmptyActionUiModel(
    val type: RegionalGuideEmptyActionType,
    @param:StringRes val labelResId: Int,
)

enum class RegionalGuideEmptyActionType {
    SEARCH_AGAIN,
    SELECT_REGION,
}

enum class RegionalGuideCandidateReason {
    MULTIPLE_CANDIDATES,
    MULTIPLE_EXACT_MATCHES,
    FALLBACK_BECAUSE_DIRECT_MATCH_NOT_FOUND,
    FAVORITE_RESTORE_AMBIGUOUS,
}

data class RegionalGuideCandidateListScrollPosition(
    val firstVisibleItemIndex: Int,
    val firstVisibleItemScrollOffset: Int,
) {
    companion object {
        val Initial = RegionalGuideCandidateListScrollPosition(
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0,
        )
    }
}
