package com.team.yeogibeoryeo.domain.regionalguide.model

data class HomeRegionalGuideSummary(
    val targetId: String,
    val regionName: String,
    val wasteTypeNames: List<String>,
    val disposalDays: String?,
    val disposalTime: String?,
    val hasDifferentDisposalDays: Boolean,
    val hasDifferentDisposalTime: Boolean,
)

sealed interface TodayRegionalWasteSummaryResult {
    data class Summary(
        val summary: HomeRegionalGuideSummary,
    ) : TodayRegionalWasteSummaryResult

    data object NoTodaySchedule : TodayRegionalWasteSummaryResult

    data object NeedsScheduleConfirmation : TodayRegionalWasteSummaryResult
}

sealed interface HomeRegionalGuideSummaryResult {
    data class Loading(
        val targetId: String? = null,
        val regionName: String? = null,
    ) : HomeRegionalGuideSummaryResult

    data object NoFavorite : HomeRegionalGuideSummaryResult

    data class Success(
        val summary: HomeRegionalGuideSummary,
    ) : HomeRegionalGuideSummaryResult

    data class NoTodaySchedule(
        val targetId: String,
        val regionName: String,
    ) : HomeRegionalGuideSummaryResult

    data class ScheduleNeedsConfirmation(
        val targetId: String,
        val regionName: String,
    ) : HomeRegionalGuideSummaryResult

    data class FavoriteRestoreFailed(
        val targetId: String,
    ) : HomeRegionalGuideSummaryResult

    data class Failure(
        val targetId: String,
        val regionName: String,
        val reason: RegionalGuideFailureReason,
    ) : HomeRegionalGuideSummaryResult
}
