package com.team.yeogibeoryeo.presentation.common.effects

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow

@Composable
internal fun BottomBarVisibilityOnScrollEffect(
    scrollState: ScrollState,
    onBottomBarVisibilityChanged: (Boolean) -> Unit,
) {
    val onBottomBarVisibilityChangedState by rememberUpdatedState(onBottomBarVisibilityChanged)

    LaunchedEffect(scrollState) {
        var previousOffset = 0
        var previousVisibility = true
        onBottomBarVisibilityChangedState(true)

        snapshotFlow { scrollState.value }
            .collect { currentOffset ->
                val isVisible = when {
                    currentOffset == 0 -> true
                    currentOffset > previousOffset -> false
                    currentOffset < previousOffset -> true
                    else -> previousVisibility
                }
                if (isVisible != previousVisibility) {
                    onBottomBarVisibilityChangedState(isVisible)
                    previousVisibility = isVisible
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

    LaunchedEffect(listState) {
        var previousPosition = 0L
        var previousVisibility = true
        onBottomBarVisibilityChangedState(true)

        snapshotFlow {
            val position = listState.firstVisibleItemIndex.toLong() * SCROLL_POSITION_ITEM_MULTIPLIER +
                listState.firstVisibleItemScrollOffset
            position to (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0)
        }.collect { (currentPosition, isAtTop) ->
            val isVisible = when {
                isAtTop -> true
                currentPosition > previousPosition -> false
                currentPosition < previousPosition -> true
                else -> previousVisibility
            }
            if (isVisible != previousVisibility) {
                onBottomBarVisibilityChangedState(isVisible)
                previousVisibility = isVisible
            }
            previousPosition = currentPosition
        }
    }
}

private const val SCROLL_POSITION_ITEM_MULTIPLIER = 1_000_000L
