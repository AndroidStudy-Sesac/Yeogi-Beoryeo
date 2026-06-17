package com.team.yeogibeoryeo.presentation.search.mapper

import com.team.yeogibeoryeo.domain.regionalguide.model.HomeRegionalGuideSummaryResult
import com.team.yeogibeoryeo.presentation.search.model.HomeRegionalGuideSummaryUiState

fun HomeRegionalGuideSummaryResult.toUiState(): HomeRegionalGuideSummaryUiState =
    when (this) {
        HomeRegionalGuideSummaryResult.Loading -> HomeRegionalGuideSummaryUiState.Loading
        HomeRegionalGuideSummaryResult.NoFavorite -> HomeRegionalGuideSummaryUiState.NoFavorite
        is HomeRegionalGuideSummaryResult.Success ->
            HomeRegionalGuideSummaryUiState.Summary(
                targetId = summary.targetId,
                regionName = summary.regionName,
                wasteTypesText = summary.wasteTypeNames.joinToString(", "),
                disposalDays = summary.disposalDays,
                disposalTime = summary.disposalTime,
            )

        is HomeRegionalGuideSummaryResult.NoTodaySchedule ->
            HomeRegionalGuideSummaryUiState.NoTodaySchedule(
                targetId = targetId,
                regionName = regionName,
            )

        is HomeRegionalGuideSummaryResult.FavoriteRestoreFailed ->
            HomeRegionalGuideSummaryUiState.FavoriteRestoreFailed(targetId = targetId)

        is HomeRegionalGuideSummaryResult.Failure ->
            HomeRegionalGuideSummaryUiState.LoadFailed(
                targetId = targetId,
                regionName = regionName,
            )
    }
