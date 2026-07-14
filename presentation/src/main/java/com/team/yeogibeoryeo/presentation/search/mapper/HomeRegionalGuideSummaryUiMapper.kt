package com.team.yeogibeoryeo.presentation.search.mapper

import com.team.yeogibeoryeo.domain.regionalguide.model.HomeRegionalGuideSummaryResult
import com.team.yeogibeoryeo.presentation.search.model.HomeRegionalGuideSummaryUiState

fun HomeRegionalGuideSummaryResult.toUiState(): HomeRegionalGuideSummaryUiState =
    when (this) {
        is HomeRegionalGuideSummaryResult.Loading ->
            HomeRegionalGuideSummaryUiState.Loading(
                targetId = targetId,
                regionName = regionName,
            )
        HomeRegionalGuideSummaryResult.NoFavorite -> HomeRegionalGuideSummaryUiState.NoFavorite
        is HomeRegionalGuideSummaryResult.Success ->
            HomeRegionalGuideSummaryUiState.Summary(
                targetId = summary.targetId,
                regionName = summary.regionName,
                disposalDays = summary.disposalDays,
                disposalTime = summary.disposalTime,
                hasDifferentDisposalDays = summary.hasDifferentDisposalDays,
                hasDifferentDisposalTime = summary.hasDifferentDisposalTime,
            )

        is HomeRegionalGuideSummaryResult.NoRepresentativeSchedule ->
            HomeRegionalGuideSummaryUiState.NoRepresentativeSchedule(
                targetId = targetId,
                regionName = regionName,
            )

        is HomeRegionalGuideSummaryResult.RepresentativeScheduleNeedsConfirmation ->
            HomeRegionalGuideSummaryUiState.RepresentativeScheduleNeedsConfirmation(
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
