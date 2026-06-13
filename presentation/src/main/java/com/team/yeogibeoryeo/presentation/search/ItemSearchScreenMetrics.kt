package com.team.yeogibeoryeo.presentation.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun itemSearchScreenMetrics(
    maxWidth: Dp,
): ItemSearchScreenMetrics {
    val spacing = ItemSearchLayoutDefaults.spacing
    val size = ItemSearchLayoutDefaults.size
    val isNarrowPhone = maxWidth <= ItemSearchScreenBreakpoints.NarrowPhoneWidth

    return ItemSearchScreenMetrics(
        horizontalPadding = if (isNarrowPhone) spacing.md else spacing.xl,
        topPadding = if (isNarrowPhone) spacing.lg else spacing.xl,
        screenVerticalSpace = if (isNarrowPhone) spacing.lg else spacing.xl,
        sectionVerticalSpace = spacing.md,
        listBottomPadding = spacing.xl,
        searchIconSize = if (isNarrowPhone) size.iconStandard else size.iconSmall,
    )
}

internal data class ItemSearchScreenMetrics(
    val horizontalPadding: Dp,
    val topPadding: Dp,
    val screenVerticalSpace: Dp,
    val sectionVerticalSpace: Dp,
    val listBottomPadding: Dp,
    val searchIconSize: Dp,
)

private object ItemSearchScreenBreakpoints {
    val NarrowPhoneWidth = 384.dp
}
