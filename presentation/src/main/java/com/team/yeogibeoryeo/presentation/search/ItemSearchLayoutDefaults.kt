package com.team.yeogibeoryeo.presentation.search

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal object ItemSearchLayoutDefaults {
    val spacing = ItemSearchSpacingDefaults
    val size = ItemSearchSizeDefaults
    val fraction = ItemSearchFractionDefaults
    val alpha = ItemSearchAlphaDefaults
    val stroke = ItemSearchStrokeDefaults
    val elevation = ItemSearchElevationDefaults
}

internal object ItemSearchSpacingDefaults {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 20.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
}

internal object ItemSearchSizeDefaults {
    val iconSmall: Dp = 20.dp
    val iconStandard: Dp = 24.dp
    val iconLarge: Dp = 32.dp
    val iconProminent: Dp = 40.dp
    val minTouchTarget: Dp = 48.dp
    val searchFieldHeight: Dp = 56.dp
    val categoryTile: Dp = 64.dp
    val categoryCell: Dp = 72.dp
    val categorySelectionBadge: Dp = 18.dp
    val categorySelectionBadgeIcon: Dp = 12.dp
    val categorySelectionBadgeOffset: Dp = 4.dp
    val usefulGuideBannerCardHeight: Dp = 136.dp
    val usefulGuidePageIndicatorActiveWidth: Dp = 24.dp
    val usefulGuidePageIndicatorInactiveSize: Dp = 8.dp
    val usefulGuidePageIndicatorHeight: Dp = 8.dp
    val detailIconContainer: Dp = 88.dp
    val infoLabelMinWidth: Dp = 88.dp
    val infoLabelMaxWidth: Dp = 112.dp
}

internal object ItemSearchFractionDefaults {
    const val USEFUL_GUIDE_BANNER_WIDTH = 0.94f
    const val USEFUL_GUIDE_LANDSCAPE_BANNER_WIDTH = 0.56f
}

internal object ItemSearchAlphaDefaults {
    const val USEFUL_GUIDE_PAGE_INDICATOR_INACTIVE = 0.32f
}

internal object ItemSearchStrokeDefaults {
    val outline: Dp = 1.dp
}

internal object ItemSearchElevationDefaults {
    val none: Dp = 0.dp
    val searchField: Dp = 6.dp
}
