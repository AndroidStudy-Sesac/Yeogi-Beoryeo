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
    val spacing = ItemSearchLayoutDefaults.spacing
    val size = ItemSearchLayoutDefaults.size
    val fontScale = LocalDensity.current.fontScale
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
    val useRoomyPhoneVisuals =
        !useLargeFontLayout &&
            maxWidth >= QuickCategoryGridBreakpoints.RoomyPhoneContentWidth
    val useNarrowVisuals = maxWidth < QuickCategoryGridBreakpoints.NarrowContentWidth
    val useNarrowLargeFontLayout =
        useLargeFontLayout &&
            maxWidth < QuickCategoryGridBreakpoints.NarrowContentWidth
    val useRoomyLargeFontLayout =
        useLargeFontLayout &&
            maxWidth >= QuickCategoryGridBreakpoints.RoomyLargeFontContentWidth

    val cellSize =
        when {
            useNarrowLargeFontLayout -> QuickCategoryGridDefaults.NarrowLargeFontCellSize
            useRoomyLargeFontLayout -> QuickCategoryGridDefaults.RoomyLargeFontCellSize
            useLargeFontLayout -> QuickCategoryGridDefaults.LargeFontCellSize
            useExpandedWidePhoneVisuals -> QuickCategoryGridDefaults.ExpandedWidePhoneCellSize
            useRoomyPhoneVisuals -> QuickCategoryGridDefaults.RoomyPhoneCellSize
            useWidePhoneVisuals -> QuickCategoryGridDefaults.WidePhoneCellSize
            useFourColumnCompactVisuals -> QuickCategoryGridDefaults.CompactCellSize
            useNarrowVisuals -> QuickCategoryGridDefaults.NarrowCellSize
            else -> size.categoryCell
        }
    val tileSize =
        when {
            useLargeFontLayout -> QuickCategoryGridDefaults.LargeFontTileSize
            useExpandedWidePhoneVisuals -> QuickCategoryGridDefaults.WidePhoneTileSize
            useRoomyPhoneVisuals -> QuickCategoryGridDefaults.WidePhoneTileSize
            useWidePhoneVisuals -> QuickCategoryGridDefaults.WidePhoneTileSize
            useFourColumnCompactVisuals -> QuickCategoryGridDefaults.CompactTileSize
            useNarrowVisuals -> QuickCategoryGridDefaults.NarrowTileSize
            else -> size.categoryTile
        }
    val iconSize =
        when {
            useLargeFontLayout -> QuickCategoryGridDefaults.LargeFontIconSize
            useExpandedWidePhoneVisuals -> QuickCategoryGridDefaults.WidePhoneIconSize
            useRoomyPhoneVisuals -> QuickCategoryGridDefaults.WidePhoneIconSize
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

    return QuickCategoryGridMetrics(
        columnCount = columnCount,
        cellSize = cellSize,
        tileSize = tileSize,
        iconSize = iconSize,
        verticalSpace = verticalSpace,
        labelSpacing = labelSpacing,
        labelTextStyle = when {
            useLargeFontLayout -> MaterialTheme.typography.bodyLarge
            useExpandedWidePhoneVisuals -> MaterialTheme.typography.bodyLarge
            useRoomyPhoneVisuals -> MaterialTheme.typography.bodyLarge
            useWidePhoneVisuals -> MaterialTheme.typography.bodyLarge
            useFourColumnCompactVisuals -> MaterialTheme.typography.labelLarge
            else -> MaterialTheme.typography.bodyLarge
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

internal val LocalQuickCategoryGridMetrics = compositionLocalOf<QuickCategoryGridMetrics> {
    error("QuickCategoryGridMetrics is not provided.")
}

private object QuickCategoryGridDefaults {
    const val MaxColumnCount = 4
    const val NarrowLargeFontMaxColumnCount = 2
    const val LargeFontMaxColumnCount = 3
    val NarrowLargeFontCellSize = 128.dp
    val RoomyLargeFontCellSize = 112.dp
    val NarrowCellSize = 80.dp
    val NarrowTileSize = 72.dp
    val NarrowIconSize = 36.dp
    val CompactCellSize = 80.dp
    val CompactTileSize = 56.dp
    val CompactIconSize = 28.dp
    val WidePhoneCellSize = 84.dp
    val RoomyPhoneCellSize = 88.dp
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
    val RoomyPhoneContentWidth = 352.dp
    val ExpandedWidePhoneContentWidth = 360.dp
    val RoomyLargeFontContentWidth = 352.dp
    const val LargeFontScale = 1.3f
}
