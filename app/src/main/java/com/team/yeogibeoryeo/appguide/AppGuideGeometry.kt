package com.team.yeogibeoryeo.appguide

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

internal fun expandedSpotlightBounds(
    targetBounds: Rect,
    paddingPx: Float,
    containerSize: IntSize,
): Rect =
    Rect(
        left = (targetBounds.left - paddingPx).coerceAtLeast(0f),
        top = (targetBounds.top - paddingPx).coerceAtLeast(0f),
        right = (targetBounds.right + paddingPx).coerceAtMost(containerSize.width.toFloat()),
        bottom = (targetBounds.bottom + paddingPx).coerceAtMost(containerSize.height.toFloat()),
    )

internal fun calculateGuideCardOffset(
    targetBounds: Rect,
    cardSize: IntSize,
    containerSize: IntSize,
    marginPx: Int,
    targetGapPx: Int,
    safeTopPx: Int,
    safeBottomPx: Int,
): IntOffset {
    val minimumX = marginPx
    val maximumX = (containerSize.width - cardSize.width - marginPx).coerceAtLeast(minimumX)
    val centeredX = (targetBounds.center.x - cardSize.width / 2f).toInt()
    val x = centeredX.coerceIn(minimumX, maximumX)

    val minimumY = safeTopPx + marginPx
    val maximumY =
        (containerSize.height - safeBottomPx - marginPx - cardSize.height)
            .coerceAtLeast(minimumY)
    val belowY = targetBounds.bottom.toInt() + targetGapPx
    val aboveY = targetBounds.top.toInt() - targetGapPx - cardSize.height
    val spaceBelow = maximumY - belowY
    val spaceAbove = aboveY - minimumY
    val y =
        when {
            spaceBelow >= 0 -> belowY
            spaceAbove >= 0 -> aboveY
            spaceAbove >= spaceBelow -> minimumY
            else -> maximumY
        }.coerceIn(minimumY, maximumY)

    return IntOffset(x = x, y = y)
}
