package com.team.yeogibeoryeo.domain.regionalguide.model

data class HomeRegionalGuideSummary(
    val targetId: String,
    val regionName: String,
    val wasteTypeNames: List<String>,
    val disposalDays: String,
    val disposalTime: String?,
)

sealed interface HomeRegionalGuideSummaryResult {
    data object Loading : HomeRegionalGuideSummaryResult

    data object NoFavorite : HomeRegionalGuideSummaryResult

    data class Success(
        val summary: HomeRegionalGuideSummary,
    ) : HomeRegionalGuideSummaryResult

    data class NoTodaySchedule(
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
