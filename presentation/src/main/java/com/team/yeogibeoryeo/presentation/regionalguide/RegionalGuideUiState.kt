package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideUiModel

sealed interface RegionalGuideUiState {
    data object Idle : RegionalGuideUiState

    data class Loading(
        val query: String
    ) : RegionalGuideUiState

    data class Success(
        val query: String,
        val guide: RegionalGuideUiModel
    ) : RegionalGuideUiState

    data class Empty(
        val query: String,
        val message: String = "조회된 지역별 배출 가이드가 없습니다."
    ) : RegionalGuideUiState

    data class Ambiguous(
        val query: String,
        val message: String
    ) : RegionalGuideUiState

    data class Error(
        val query: String,
        val message: String
    ) : RegionalGuideUiState
}
