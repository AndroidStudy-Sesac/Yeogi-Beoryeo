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
        val errorType: RegionalGuideErrorType,
        val canRestoreCandidates: Boolean = false,
    ) : RegionalGuideUiState
}

enum class RegionalGuideErrorType(
    @param:StringRes val messageResId: Int,
) {
    KEYWORD_SEARCH(R.string.regional_guide_error_keyword_search_message),
    ADDRESS_SEARCH(R.string.regional_guide_error_address_search_message),
    DATA(R.string.regional_guide_error_favorite_restore_message),
    SELECTED_REGION(R.string.regional_guide_error_selected_region_message),
    NETWORK(R.string.regional_guide_error_network_message),
    API(R.string.regional_guide_error_api_message),
    UNKNOWN(R.string.regional_guide_error_unknown_message),
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
