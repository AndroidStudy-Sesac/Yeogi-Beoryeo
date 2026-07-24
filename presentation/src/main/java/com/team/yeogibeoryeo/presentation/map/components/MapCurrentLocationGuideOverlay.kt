package com.team.yeogibeoryeo.presentation.map.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.R

@Composable
fun MapCurrentLocationGuideOverlay(
    targetBounds: Rect,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(R.string.map_current_location_guide_title)
    val description = stringResource(R.string.map_current_location_guide_description)
    val focusRequester = remember { FocusRequester() }

    BackHandler(onBack = onDismiss)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        val scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)
        val outlineColor = MaterialTheme.colorScheme.primary

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            event.changes
                                .filterNot { change -> change.isConsumed }
                                .forEach { change -> change.consume() }
                        }
                    }
                },
        ) {
            val spotlightBounds = expandedSpotlightBounds(
                targetBounds = targetBounds,
                paddingPx = SPOTLIGHT_PADDING.toPx(),
                containerSize = IntSize(size.width.toInt(), size.height.toInt()),
            )
            val radius = SPOTLIGHT_RADIUS.toPx()
            val scrimPath = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, size.width, size.height))
                addRoundRect(RoundRect(spotlightBounds, radius, radius))
            }

            drawPath(path = scrimPath, color = scrimColor)
            drawRoundRect(
                color = outlineColor,
                topLeft = spotlightBounds.topLeft,
                size = spotlightBounds.size,
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width = SPOTLIGHT_STROKE_WIDTH.toPx()),
            )
        }

        GuideCardLayout(targetBounds = targetBounds) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { paneTitle = title },
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = CARD_ELEVATION,
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(CARD_CONTENT_PADDING),
                ) {
                    Text(
                        text = title,
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .focusable()
                            .semantics {
                                liveRegion = LiveRegionMode.Polite
                                contentDescription = "$title $description"
                            },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(CARD_TEXT_SPACING))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(CARD_ACTION_SPACING))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(onClick = onDismiss) {
                            Text(text = stringResource(R.string.map_current_location_guide_confirm))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideCardLayout(
    targetBounds: Rect,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val safeTopPx = WindowInsets.safeDrawing.getTop(density)
    val safeBottomPx = WindowInsets.safeDrawing.getBottom(density)

    Layout(
        modifier = Modifier.fillMaxSize(),
        content = content,
    ) { measurables, constraints ->
        val marginPx = CARD_SCREEN_MARGIN.roundToPx()
        val targetGapPx = CARD_TARGET_GAP.roundToPx()
        val cardWidth = minOf(
            CARD_MAX_WIDTH.roundToPx(),
            (constraints.maxWidth - marginPx * 2).coerceAtLeast(0),
        )
        val cardMaxHeight =
            (constraints.maxHeight - safeTopPx - safeBottomPx - marginPx * 2).coerceAtLeast(0)
        val placeable = measurables.single().measure(
            Constraints(
                minWidth = cardWidth,
                maxWidth = cardWidth,
                minHeight = 0,
                maxHeight = cardMaxHeight,
            ),
        )
        val offset = calculateGuideCardOffset(
            targetBounds = targetBounds,
            cardSize = IntSize(placeable.width, placeable.height),
            containerSize = IntSize(constraints.maxWidth, constraints.maxHeight),
            marginPx = marginPx,
            targetGapPx = targetGapPx,
            safeTopPx = safeTopPx,
            safeBottomPx = safeBottomPx,
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(x = offset.x, y = offset.y)
        }
    }
}

private fun expandedSpotlightBounds(
    targetBounds: Rect,
    paddingPx: Float,
    containerSize: IntSize,
): Rect {
    return Rect(
        left = (targetBounds.left - paddingPx).coerceAtLeast(0f),
        top = (targetBounds.top - paddingPx).coerceAtLeast(0f),
        right = (targetBounds.right + paddingPx).coerceAtMost(containerSize.width.toFloat()),
        bottom = (targetBounds.bottom + paddingPx).coerceAtMost(containerSize.height.toFloat()),
    )
}

private fun calculateGuideCardOffset(
    targetBounds: Rect,
    cardSize: IntSize,
    containerSize: IntSize,
    marginPx: Int,
    targetGapPx: Int,
    safeTopPx: Int,
    safeBottomPx: Int,
): IntOffset {
    val minY = safeTopPx + marginPx
    val maxY = containerSize.height - safeBottomPx - marginPx - cardSize.height
    val availableAbove = targetBounds.top.toInt() - targetGapPx - minY
    val y = if (availableAbove >= cardSize.height) {
        targetBounds.top.toInt() - targetGapPx - cardSize.height
    } else {
        targetBounds.bottom.toInt() + targetGapPx
    }.coerceIn(minY, maxY.coerceAtLeast(minY))

    val targetCenterX = targetBounds.center.x.toInt()
    val preferredX = targetCenterX - cardSize.width / 2
    val maxX = containerSize.width - marginPx - cardSize.width
    val x = preferredX.coerceIn(marginPx, maxX.coerceAtLeast(marginPx))

    return IntOffset(x = x, y = y)
}

private const val SCRIM_ALPHA = 0.40f
private val SPOTLIGHT_PADDING = 8.dp
private val SPOTLIGHT_RADIUS = 16.dp
private val SPOTLIGHT_STROKE_WIDTH = 2.dp
private val CARD_SCREEN_MARGIN = 16.dp
private val CARD_TARGET_GAP = 16.dp
private val CARD_MAX_WIDTH = 520.dp
private val CARD_CONTENT_PADDING = 20.dp
private val CARD_TEXT_SPACING = 8.dp
private val CARD_ACTION_SPACING = 16.dp
private val CARD_ELEVATION = 8.dp
