package com.team.yeogibeoryeo.presentation.search.model

sealed interface HomeRegionalGuideSummaryUiState {
    data class Loading(
        val targetId: String? = null,
        val regionName: String? = null,
    ) : HomeRegionalGuideSummaryUiState

    data object NoFavorite : HomeRegionalGuideSummaryUiState

    data class Summary(
        val targetId: String,
        val regionName: String,
        val disposalDays: String?,
        val disposalTime: String?,
        val hasDifferentDisposalDays: Boolean,
        val hasDifferentDisposalTime: Boolean,
    ) : HomeRegionalGuideSummaryUiState

    data class NoRepresentativeSchedule(
        val targetId: String,
        val regionName: String,
    ) : HomeRegionalGuideSummaryUiState

    data class RepresentativeScheduleNeedsConfirmation(
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
