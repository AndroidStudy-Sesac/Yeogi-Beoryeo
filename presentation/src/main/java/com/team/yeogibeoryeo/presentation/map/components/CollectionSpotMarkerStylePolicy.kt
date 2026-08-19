package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.common.design.theme.MapMarkerDefaultDark
import com.team.yeogibeoryeo.common.design.theme.MapMarkerSelectedDark

internal enum class CollectionSpotMarkerIcon {
    Default,
    Black,
}

internal data class CollectionSpotMarkerStyle(
    val icon: CollectionSpotMarkerIcon,
    val color: Color,
    val width: Dp?,
    val height: Dp?,
    val zIndex: Int,
    val isForceShowIcon: Boolean,
)

internal object CollectionSpotMarkerStylePolicy {
    fun defaultStyle(
        isDarkTheme: Boolean,
        lightColor: Color,
    ): CollectionSpotMarkerStyle =
        if (isDarkTheme) {
            CollectionSpotMarkerStyle(
                icon = CollectionSpotMarkerIcon.Black,
                color = MapMarkerDefaultDark,
                width = DarkDefaultMarkerWidth,
                height = DarkDefaultMarkerHeight,
                zIndex = DEFAULT_MARKER_Z_INDEX,
                isForceShowIcon = false,
            )
        } else {
            CollectionSpotMarkerStyle(
                icon = CollectionSpotMarkerIcon.Default,
                color = lightColor,
                width = null,
                height = null,
                zIndex = DEFAULT_MARKER_Z_INDEX,
                isForceShowIcon = false,
            )
        }

    fun selectedStyle(
        isDarkTheme: Boolean,
        lightColor: Color,
    ): CollectionSpotMarkerStyle =
        if (isDarkTheme) {
            CollectionSpotMarkerStyle(
                icon = CollectionSpotMarkerIcon.Black,
                color = MapMarkerSelectedDark,
                width = DarkSelectedMarkerWidth,
                height = DarkSelectedMarkerHeight,
                zIndex = SELECTED_MARKER_Z_INDEX,
                isForceShowIcon = true,
            )
        } else {
            CollectionSpotMarkerStyle(
                icon = CollectionSpotMarkerIcon.Default,
                color = lightColor,
                width = null,
                height = null,
                zIndex = SELECTED_MARKER_Z_INDEX,
                isForceShowIcon = false,
            )
        }

    fun style(
        isSelected: Boolean,
        isDarkTheme: Boolean,
        lightDefaultColor: Color,
        lightSelectedColor: Color,
    ): CollectionSpotMarkerStyle =
        if (isSelected) {
            selectedStyle(
                isDarkTheme = isDarkTheme,
                lightColor = lightSelectedColor,
            )
        } else {
            defaultStyle(
                isDarkTheme = isDarkTheme,
                lightColor = lightDefaultColor,
            )
        }

    private val DarkDefaultMarkerWidth = 40.dp
    private val DarkDefaultMarkerHeight = 53.dp
    private val DarkSelectedMarkerWidth = 48.dp
    private val DarkSelectedMarkerHeight = 64.dp
    private const val DEFAULT_MARKER_Z_INDEX = 0
    private const val SELECTED_MARKER_Z_INDEX = 10
}
