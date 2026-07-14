package com.team.yeogibeoryeo.presentation.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.common.components.AppTopBarDefaults

@Composable
internal fun itemSearchScreenMetrics(
    maxWidth: Dp,
    maxHeight: Dp,
): ItemSearchScreenMetrics {
    val spec = itemSearchScreenMetricsSpec(
        maxWidth = maxWidth,
        maxHeight = maxHeight,
    )

    return ItemSearchScreenMetrics(
        horizontalPadding = spec.horizontalPadding,
        topPadding = spec.topPadding,
        homeHeaderTopPadding = spec.homeHeaderTopPadding,
        screenVerticalSpace = spec.screenVerticalSpace,
        sectionVerticalSpace = spec.sectionVerticalSpace,
        listBottomPadding = spec.listBottomPadding,
        searchIconSize = spec.searchIconSize,
        usefulGuideBannerWidthFraction = spec.usefulGuideBannerWidthFraction,
        isCompactLandscape = spec.isCompactLandscape,
    )
}

internal fun itemSearchScreenMetricsSpec(
    maxWidth: Dp,
    maxHeight: Dp,
): ItemSearchScreenMetricsSpec {
    val spacing = ItemSearchLayoutDefaults.spacing
    val size = ItemSearchLayoutDefaults.size
    val isNarrowPhone = maxWidth <= ItemSearchScreenBreakpoints.NarrowPhoneWidth
    val isCompactLandscape =
        maxWidth > maxHeight && maxHeight <= ItemSearchScreenBreakpoints.CompactLandscapeHeight

    return ItemSearchScreenMetricsSpec(
        horizontalPadding = if (isNarrowPhone) spacing.md else spacing.xl,
        topPadding = when {
            isCompactLandscape -> spacing.md
            isNarrowPhone -> spacing.lg
            else -> spacing.xl
        },
        homeHeaderTopPadding = (AppTopBarDefaults.height - AppTopBarDefaults.buttonSize) / 2f,
        screenVerticalSpace = when {
            isCompactLandscape -> spacing.md
            isNarrowPhone -> spacing.lg
            else -> spacing.xl
        },
        sectionVerticalSpace = spacing.md,
        listBottomPadding = spacing.xl,
        searchIconSize = if (isNarrowPhone) size.iconStandard else size.iconSmall,
        usefulGuideBannerWidthFraction = if (isCompactLandscape) {
            ItemSearchLayoutDefaults.fraction.USEFUL_GUIDE_LANDSCAPE_BANNER_WIDTH
        } else {
            ItemSearchLayoutDefaults.fraction.USEFUL_GUIDE_BANNER_WIDTH
        },
        isCompactLandscape = isCompactLandscape,
    )
}

internal data class ItemSearchScreenMetrics(
    val horizontalPadding: Dp,
    val topPadding: Dp,
    val homeHeaderTopPadding: Dp,
    val screenVerticalSpace: Dp,
    val sectionVerticalSpace: Dp,
    val listBottomPadding: Dp,
    val searchIconSize: Dp,
    val usefulGuideBannerWidthFraction: Float,
    val isCompactLandscape: Boolean,
)

internal data class ItemSearchScreenMetricsSpec(
    val horizontalPadding: Dp,
    val topPadding: Dp,
    val homeHeaderTopPadding: Dp,
    val screenVerticalSpace: Dp,
    val sectionVerticalSpace: Dp,
    val listBottomPadding: Dp,
    val searchIconSize: Dp,
    val usefulGuideBannerWidthFraction: Float,
    val isCompactLandscape: Boolean,
)

private object ItemSearchScreenBreakpoints {
    val NarrowPhoneWidth = 384.dp
    val CompactLandscapeHeight = 480.dp
}
