package com.team.yeogibeoryeo.presentation.regionalguide

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal fun regionalGuideScreenMetricsSpec(
    maxWidth: Dp,
    maxHeight: Dp,
): RegionalGuideScreenMetrics {
    val isCompactLandscape =
        maxWidth > maxHeight && maxHeight <= RegionalGuideScreenBreakpoints.CompactLandscapeHeight

    return RegionalGuideScreenMetrics(
        isCompactLandscape = isCompactLandscape,
        horizontalPadding = if (isCompactLandscape) 16.dp else 20.dp,
        topPadding = if (isCompactLandscape) 12.dp else 20.dp,
        headerVerticalSpace = if (isCompactLandscape) 8.dp else 20.dp,
        contentTopSpace = if (isCompactLandscape) 12.dp else 20.dp,
        candidateListMaxHeight = if (isCompactLandscape) {
            minOf(maxHeight * CandidateListHeightFraction, CompactCandidateListMaxHeight)
        } else {
            DefaultCandidateListMaxHeight
        },
    )
}

internal data class RegionalGuideScreenMetrics(
    val isCompactLandscape: Boolean,
    val horizontalPadding: Dp,
    val topPadding: Dp,
    val headerVerticalSpace: Dp,
    val contentTopSpace: Dp,
    val candidateListMaxHeight: Dp,
)

private object RegionalGuideScreenBreakpoints {
    val CompactLandscapeHeight = 480.dp
}

private const val CandidateListHeightFraction = 0.32f
private val CompactCandidateListMaxHeight = 160.dp
private val DefaultCandidateListMaxHeight = 260.dp
