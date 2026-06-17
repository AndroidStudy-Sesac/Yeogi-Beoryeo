package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.search.ItemSearchLayoutDefaults

@Composable
internal fun quickCategoryGridMetrics(
    maxWidth: Dp,
): QuickCategoryGridMetrics {
    val fontScale = LocalDensity.current.fontScale
    val spec = quickCategoryGridMetricsSpec(
        maxWidth = maxWidth,
        fontScale = fontScale,
    )

    return QuickCategoryGridMetrics(
        columnCount = spec.columnCount,
        cellSize = spec.cellSize,
        tileSize = spec.tileSize,
        iconSize = spec.iconSize,
        verticalSpace = spec.verticalSpace,
        labelSpacing = spec.labelSpacing,
        labelTextStyle = when (spec.labelTextStyleType) {
            QuickCategoryGridLabelTextStyleType.BodyLarge -> MaterialTheme.typography.bodyLarge
            QuickCategoryGridLabelTextStyleType.LabelLarge -> MaterialTheme.typography.labelLarge
        },
    )
}

internal fun quickCategoryGridMetricsSpec(
    maxWidth: Dp,
    fontScale: Float,
): QuickCategoryGridMetricsSpec {
    val spacing = ItemSearchLayoutDefaults.spacing
    val size = ItemSearchLayoutDefaults.size
    val useLargeFontLayout = fontScale >= QuickCategoryGridBreakpoints.LargeFontScale
    val useFourColumnCompactVisuals =
        !useLargeFontLayout &&
            maxWidth >= QuickCategoryGridBreakpoints.NarrowContentWidth &&
            maxWidth < QuickCategoryGridBreakpoints.FourColumnContentWidth
    val useWidePhoneVisuals =
        !useLargeFontLayout &&
            maxWidth >= QuickCategoryGridBreakpoints.WidePhoneContentWidth
    val useExpandedWidePhoneVisuals =
        !useLargeFontLayout &&
            maxWidth >= QuickCategoryGridBreakpoints.ExpandedWidePhoneContentWidth
    val useLargePhoneVisuals =
        !useLargeFontLayout &&
            maxWidth >= QuickCategoryGridBreakpoints.LargePhoneContentWidth
    val useNarrowVisuals = maxWidth < QuickCategoryGridBreakpoints.NarrowContentWidth
    val useNarrowLargeFontLayout =
        useLargeFontLayout &&
            maxWidth < QuickCategoryGridBreakpoints.NarrowContentWidth
    val useWideLargeFontLayout =
        useLargeFontLayout &&
            maxWidth >= QuickCategoryGridBreakpoints.WideLargeFontContentWidth

    val cellSize =
        when {
            useNarrowLargeFontLayout -> QuickCategoryGridDefaults.NarrowLargeFontCellSize
            useWideLargeFontLayout -> QuickCategoryGridDefaults.WideLargeFontCellSize
            useLargeFontLayout -> QuickCategoryGridDefaults.LargeFontCellSize
            useExpandedWidePhoneVisuals -> QuickCategoryGridDefaults.ExpandedWidePhoneCellSize
            useLargePhoneVisuals -> QuickCategoryGridDefaults.LargePhoneCellSize
            useWidePhoneVisuals -> QuickCategoryGridDefaults.WidePhoneCellSize
            useFourColumnCompactVisuals -> QuickCategoryGridDefaults.CompactCellSize
            useNarrowVisuals -> QuickCategoryGridDefaults.NarrowCellSize
            else -> size.categoryCell
        }
    val tileSize =
        when {
            useLargeFontLayout -> QuickCategoryGridDefaults.LargeFontTileSize
            useExpandedWidePhoneVisuals -> QuickCategoryGridDefaults.WidePhoneTileSize
            useLargePhoneVisuals -> QuickCategoryGridDefaults.WidePhoneTileSize
            useWidePhoneVisuals -> QuickCategoryGridDefaults.WidePhoneTileSize
            useFourColumnCompactVisuals -> QuickCategoryGridDefaults.CompactTileSize
            useNarrowVisuals -> QuickCategoryGridDefaults.NarrowTileSize
            else -> size.categoryTile
        }
    val iconSize =
        when {
            useLargeFontLayout -> QuickCategoryGridDefaults.LargeFontIconSize
            useExpandedWidePhoneVisuals -> QuickCategoryGridDefaults.WidePhoneIconSize
            useLargePhoneVisuals -> QuickCategoryGridDefaults.WidePhoneIconSize
            useWidePhoneVisuals -> QuickCategoryGridDefaults.WidePhoneIconSize
            useFourColumnCompactVisuals -> QuickCategoryGridDefaults.CompactIconSize
            useNarrowVisuals -> QuickCategoryGridDefaults.NarrowIconSize
            else -> size.iconLarge
        }
    val verticalSpace = if (useLargeFontLayout) {
        spacing.lg
    } else {
        spacing.md
    }
    val labelSpacing = if (useLargeFontLayout) {
        spacing.xs
    } else {
        spacing.xxs
    }
    val maxColumnCount =
        if (useNarrowLargeFontLayout) {
            QuickCategoryGridDefaults.NarrowLargeFontMaxColumnCount
        } else if (useLargeFontLayout) {
            QuickCategoryGridDefaults.LargeFontMaxColumnCount
        } else {
            QuickCategoryGridDefaults.MaxColumnCount
        }
    val columnCount =
        (maxWidth / cellSize)
            .toInt()
            .coerceIn(1, maxColumnCount)

    return QuickCategoryGridMetricsSpec(
        columnCount = columnCount,
        cellSize = cellSize,
        tileSize = tileSize,
        iconSize = iconSize,
        verticalSpace = verticalSpace,
        labelSpacing = labelSpacing,
        labelTextStyleType = when {
            useFourColumnCompactVisuals -> QuickCategoryGridLabelTextStyleType.LabelLarge
            else -> QuickCategoryGridLabelTextStyleType.BodyLarge
        },
    )
}

internal data class QuickCategoryGridMetrics(
    val columnCount: Int,
    val cellSize: Dp,
    val tileSize: Dp,
    val iconSize: Dp,
    val verticalSpace: Dp,
    val labelSpacing: Dp,
    val labelTextStyle: TextStyle,
)

internal data class QuickCategoryGridMetricsSpec(
    val columnCount: Int,
    val cellSize: Dp,
    val tileSize: Dp,
    val iconSize: Dp,
    val verticalSpace: Dp,
    val labelSpacing: Dp,
    val labelTextStyleType: QuickCategoryGridLabelTextStyleType,
)

internal enum class QuickCategoryGridLabelTextStyleType {
    BodyLarge,
    LabelLarge,
}

internal val LocalQuickCategoryGridMetrics = compositionLocalOf<QuickCategoryGridMetrics> {
    error("QuickCategoryGridMetrics is not provided.")
}

private object QuickCategoryGridDefaults {
    const val MaxColumnCount = 4
    const val NarrowLargeFontMaxColumnCount = 2
    const val LargeFontMaxColumnCount = 3
    val NarrowLargeFontCellSize = 128.dp
    val WideLargeFontCellSize = 112.dp
    val NarrowCellSize = 80.dp
    val NarrowTileSize = 72.dp
    val NarrowIconSize = 36.dp
    val CompactCellSize = 80.dp
    val CompactTileSize = 56.dp
    val CompactIconSize = 28.dp
    val WidePhoneCellSize = 84.dp
    val LargePhoneCellSize = 88.dp
    val ExpandedWidePhoneCellSize = 90.dp
    val WidePhoneTileSize = 64.dp
    val WidePhoneIconSize = 32.dp
    val LargeFontCellSize = 96.dp
    val LargeFontTileSize = 72.dp
    val LargeFontIconSize = 36.dp
}

private object QuickCategoryGridBreakpoints {
    val NarrowContentWidth = 300.dp
    val FourColumnContentWidth = 336.dp
    val WidePhoneContentWidth = 336.dp
    val LargePhoneContentWidth = 352.dp
    val ExpandedWidePhoneContentWidth = 360.dp
    val WideLargeFontContentWidth = 352.dp
    const val LargeFontScale = 1.3f
}
