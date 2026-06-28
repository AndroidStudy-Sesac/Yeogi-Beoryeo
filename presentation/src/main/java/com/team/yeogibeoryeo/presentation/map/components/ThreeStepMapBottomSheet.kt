package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun ThreeStepMapBottomSheet(
    sheetLevel: MapSheetLevel,
    revealKey: Any?,
    onSheetLevelChanged: (MapSheetLevel) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()
        val sheetHeight = maxHeight - MapSheetTopMargin
        val sheetHeightPx = with(density) {
            sheetHeight.toPx()
        }
        val hiddenOffset = with(density) {
            (sheetHeight - Dp.Hairline).toPx().coerceIn(0f, sheetHeightPx)
        }
        val peekOffset = with(density) {
            (sheetHeight - MapResultBottomSheetPeekHeight).toPx().coerceIn(0f, hiddenOffset)
        }
        val mediumOffset = with(density) {
            (sheetHeight - MapSpotDetailBottomSheetPeekHeight).toPx().coerceIn(0f, hiddenOffset)
        }
        val halfOffset = (sheetHeightPx * (1f - MapSheetHalfVisibleRatio))
            .coerceIn(0f, hiddenOffset)
        val expandedOffset = 0f
        fun offsetFor(level: MapSheetLevel): Float =
            when (level) {
                MapSheetLevel.Hidden -> hiddenOffset
                MapSheetLevel.Peek -> peekOffset
                MapSheetLevel.Medium -> mediumOffset
                MapSheetLevel.Half -> halfOffset
                MapSheetLevel.Expanded -> expandedOffset
            }

        val targetOffset = offsetFor(sheetLevel)
        val sheetOffset = remember(sheetHeightPx) {
            Animatable(targetOffset)
        }

        LaunchedEffect(targetOffset, revealKey) {
            sheetOffset.animateTo(targetOffset)
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(sheetHeight)
                .offset {
                    IntOffset(x = 0, y = sheetOffset.value.roundToInt())
                }
                .pointerInput(sheetHeightPx, hiddenOffset, peekOffset, mediumOffset, halfOffset) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                sheetOffset.snapTo(
                                    (sheetOffset.value + dragAmount).coerceIn(
                                        expandedOffset,
                                        hiddenOffset,
                                    ),
                                )
                            }
                        },
                        onDragEnd = {
                            val nearestLevel = MapSheetLevel.entries.minBy { level ->
                                kotlin.math.abs(offsetFor(level) - sheetOffset.value)
                            }

                            onSheetLevelChanged(nearestLevel)
                            coroutineScope.launch {
                                sheetOffset.animateTo(offsetFor(nearestLevel))
                            }
                        },
                    )
                },
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

enum class MapSheetLevel {
    Hidden,
    Peek,
    Medium,
    Half,
    Expanded,
}

private val MapSheetTopMargin = 72.dp
private const val MapSheetHalfVisibleRatio = 0.55f
val MapResultBottomSheetPeekHeight = 144.dp
val MapSpotDetailBottomSheetPeekHeight = 220.dp
