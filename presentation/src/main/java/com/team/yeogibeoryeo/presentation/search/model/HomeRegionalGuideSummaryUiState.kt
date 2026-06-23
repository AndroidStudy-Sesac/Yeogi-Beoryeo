package com.team.yeogibeoryeo.presentation.search.model

sealed interface HomeRegionalGuideSummaryUiState {
    data object Loading : HomeRegionalGuideSummaryUiState

    data object NoFavorite : HomeRegionalGuideSummaryUiState

    data class Summary(
        val targetId: String,
        val regionName: String,
        val wasteTypesText: String,
        val disposalDays: String,
        val disposalTime: String?,
    ) : HomeRegionalGuideSummaryUiState

    data class NoTodaySchedule(
        val targetId: String,
        val regionName: String,
    ) : HomeRegionalGuideSummaryUiState

    data class ScheduleNeedsConfirmation(
        val targetId: String,
        val regionName: String,
    ) : HomeRegionalGuideSummaryUiState

    data class FavoriteRestoreFailed(
        val targetId: String,
    ) : HomeRegionalGuideSummaryUiState

    data class LoadFailed(
        val targetId: String,
        val regionName: String,
    ) : HomeRegionalGuideSummaryUiState
}
