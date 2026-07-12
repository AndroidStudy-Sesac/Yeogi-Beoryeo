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

sealed interface HomeRegionalGuideSummaryBuildResult {
    data class Summary(
        val summary: HomeRegionalGuideSummary,
    ) : HomeRegionalGuideSummaryBuildResult

    data object NoRepresentativeSchedule : HomeRegionalGuideSummaryBuildResult

    data object RepresentativeScheduleNeedsConfirmation : HomeRegionalGuideSummaryBuildResult
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

    data class NoRepresentativeSchedule(
        val targetId: String,
        val regionName: String,
    ) : HomeRegionalGuideSummaryResult

    data class RepresentativeScheduleNeedsConfirmation(
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
