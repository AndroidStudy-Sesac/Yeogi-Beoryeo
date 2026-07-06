package com.team.yeogibeoryeo.presentation.regionalguide

import androidx.annotation.StringRes
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideCandidateUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionSearchCandidateUiModel

sealed interface RegionalGuideUiState {
    data object Idle : RegionalGuideUiState

    data class Loading(
        val query: String
    ) : RegionalGuideUiState

    data class Success(
        val query: String,
        val guide: RegionalGuideUiModel,
        val isFavorite: Boolean = false,
    ) : RegionalGuideUiState

    data class Empty(
        val query: String,
        @param:StringRes val titleResId: Int = R.string.regional_guide_empty_default_title,
        @param:StringRes val messageResId: Int = R.string.regional_guide_empty_default_message,
        val action: RegionalGuideEmptyActionUiModel? = null,
    ) : RegionalGuideUiState

    data class Ambiguous(
        val query: String,
        val message: String,
        val candidates: List<RegionSearchCandidateUiModel>
    ) : RegionalGuideUiState

    data class GuideCandidates(
        val query: String,
        val message: String,
        val candidates: List<RegionalGuideCandidateUiModel>
    ) : RegionalGuideUiState

    data class Error(
        val query: String,
        val message: String
    ) : RegionalGuideUiState
}

data class RegionalGuideEmptyActionUiModel(
    val type: RegionalGuideEmptyActionType,
    @param:StringRes val labelResId: Int,
)

enum class RegionalGuideEmptyActionType {
    SEARCH_AGAIN,
    SELECT_REGION,
}
