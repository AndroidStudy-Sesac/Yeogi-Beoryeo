package com.team.yeogibeoryeo.appguide

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.R

@Composable
fun AppGuideOverlay(
    step: AppGuideStep,
    targetBounds: Rect,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = step.content()
    val focusRequester = remember { FocusRequester() }

    BackHandler {
        if (step.hasPrevious) {
            onPrevious()
        } else {
            onSkip()
        }
    }

    LaunchedEffect(step) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
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
        val animatedBounds by animateRectAsState(
            targetValue = targetBounds,
            animationSpec = tween(durationMillis = SPOTLIGHT_ANIMATION_DURATION_MILLIS),
            label = "appGuideSpotlightBounds",
        )
        val scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)
        val outlineColor = MaterialTheme.colorScheme.primary

        Canvas(modifier = Modifier.fillMaxSize()) {
            val spotlightBounds = expandedSpotlightBounds(
                targetBounds = animatedBounds,
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

        GuideCardLayout(
            targetBounds = animatedBounds,
        ) {
            AppGuideCard(
                step = step,
                content = content,
                focusRequester = focusRequester,
                onPrevious = onPrevious,
                onNext = onNext,
                onSkip = onSkip,
            )
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
        val cardWidth =
            minOf(
                CARD_MAX_WIDTH.roundToPx(),
                (constraints.maxWidth - marginPx * 2).coerceAtLeast(0),
            )
        val cardMaxHeight =
            (constraints.maxHeight - safeTopPx - safeBottomPx - marginPx * 2)
                .coerceAtLeast(0)
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

@Composable
private fun AppGuideCard(
    step: AppGuideStep,
    content: AppGuideContent,
    focusRequester: FocusRequester,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val progress = stringResource(
        R.string.app_guide_progress,
        step.number,
        AppGuideStep.entries.size,
    )
    val accessibilityAnnouncement = stringResource(
        R.string.app_guide_accessibility_announcement,
        step.number,
        AppGuideStep.entries.size,
        content.title.trimEnd('.', '?', '!'),
        content.description,
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .semantics { paneTitle = content.title },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = CARD_ELEVATION,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(CARD_CONTENT_PADDING),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = progress,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (!step.isLast) {
                    TextButton(onClick = onSkip) {
                        Text(text = stringResource(R.string.app_guide_skip))
                    }
                }
            }

            Text(
                text = content.title,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusable()
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = accessibilityAnnouncement
                    },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(CARD_TEXT_SPACING))
            Text(
                text = content.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(CARD_ACTION_SPACING))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (step.hasPrevious) {
                    TextButton(onClick = onPrevious) {
                        Text(text = stringResource(R.string.app_guide_previous))
                    }
                    Spacer(modifier = Modifier.width(CARD_BUTTON_SPACING))
                }
                Button(onClick = onNext) {
                    Text(
                        text = stringResource(
                            if (step.isLast) {
                                R.string.app_guide_finish
                            } else {
                                R.string.app_guide_next
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppGuideStep.content(): AppGuideContent =
    when (this) {
        AppGuideStep.ITEM_SEARCH -> AppGuideContent(
            title = stringResource(R.string.app_guide_item_search_title),
            description = stringResource(R.string.app_guide_item_search_description),
        )

        AppGuideStep.QUICK_CATEGORY -> AppGuideContent(
            title = stringResource(R.string.app_guide_quick_category_title),
            description = stringResource(R.string.app_guide_quick_category_description),
        )

        AppGuideStep.MAP -> AppGuideContent(
            title = stringResource(R.string.app_guide_map_title),
            description = stringResource(R.string.app_guide_map_description),
        )

        AppGuideStep.REGIONAL_GUIDE -> AppGuideContent(
            title = stringResource(R.string.app_guide_regional_guide_title),
            description = stringResource(R.string.app_guide_regional_guide_description),
        )

        AppGuideStep.FAVORITES -> AppGuideContent(
            title = stringResource(R.string.app_guide_favorites_title),
            description = stringResource(R.string.app_guide_favorites_description),
        )
    }

private data class AppGuideContent(
    val title: String,
    val description: String,
)

private const val SPOTLIGHT_ANIMATION_DURATION_MILLIS = 200
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
private val CARD_BUTTON_SPACING = 8.dp
private val CARD_ELEVATION = 8.dp
