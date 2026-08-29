package com.team.yeogibeoryeo.presentation.common.effects

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.dp

@Composable
internal fun BottomBarVisibilityOnScrollEffect(
    scrollState: ScrollState,
    onBottomBarVisibilityChanged: (Boolean) -> Unit,
) {
    val onBottomBarVisibilityChangedState by rememberUpdatedState(onBottomBarVisibilityChanged)
    val isDragged by scrollState.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(scrollState) {
        var previousOffset = 0
        var previousVisibility = true
        var didVisibilityChangeInCurrentDrag = false
        onBottomBarVisibilityChangedState(true)

        snapshotFlow { scrollState.value to isDragged }
            .collect { (currentOffset, isDragged) ->
                if (!isDragged) {
                    didVisibilityChangeInCurrentDrag = false
                }
                val isVisible = when {
                    !isDragged -> currentOffset == 0 || previousVisibility
                    didVisibilityChangeInCurrentDrag -> previousVisibility
                    currentOffset == 0 -> true
                    currentOffset > previousOffset -> false
                    currentOffset < previousOffset -> true
                    else -> previousVisibility
                }
                if (isVisible != previousVisibility) {
                    onBottomBarVisibilityChangedState(isVisible)
                    previousVisibility = isVisible
                    didVisibilityChangeInCurrentDrag = true
                }
                previousOffset = currentOffset
            }
    }
}

@Composable
internal fun BottomBarVisibilityOnScrollEffect(
    listState: LazyListState,
    onBottomBarVisibilityChanged: (Boolean) -> Unit,
) {
    val onBottomBarVisibilityChangedState by rememberUpdatedState(onBottomBarVisibilityChanged)
    val isDragged by listState.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(listState) {
        var previousPosition = 0L
        var previousVisibility = true
        var didVisibilityChangeInCurrentDrag = false
        onBottomBarVisibilityChangedState(true)

        snapshotFlow {
            val position = listState.firstVisibleItemIndex.toLong() * SCROLL_POSITION_ITEM_MULTIPLIER +
                listState.firstVisibleItemScrollOffset
            Triple(
                position,
                listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0,
                isDragged,
            )
        }.collect { (currentPosition, isAtTop, isDragged) ->
            if (!isDragged) {
                didVisibilityChangeInCurrentDrag = false
            }
            val isVisible = when {
                !isDragged -> isAtTop || previousVisibility
                didVisibilityChangeInCurrentDrag -> previousVisibility
                isAtTop -> true
                currentPosition > previousPosition -> false
                currentPosition < previousPosition -> true
                else -> previousVisibility
            }
            if (isVisible != previousVisibility) {
                onBottomBarVisibilityChangedState(isVisible)
                previousVisibility = isVisible
                didVisibilityChangeInCurrentDrag = true
            }
            previousPosition = currentPosition
        }
    }
}

internal val bottomBarCollapseScrollAllowance = 80.dp

private const val SCROLL_POSITION_ITEM_MULTIPLIER = 1_000_000L
