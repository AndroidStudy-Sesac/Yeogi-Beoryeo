package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideCandidateUiModel
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
        val message: String = "조회된 지역별 배출 가이드가 없습니다."
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
