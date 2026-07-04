package com.team.yeogibeoryeo.presentation.search

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
        onBottomBarVisibilityChangedState(true)

        snapshotFlow { scrollState.value to scrollState.maxValue }
            .collect { (currentOffset, maxOffset) ->
                val isAtBottom = maxOffset > 0 && currentOffset in maxOffset..Int.MAX_VALUE
                when {
                    currentOffset == 0 -> onBottomBarVisibilityChangedState(true)
                    currentOffset > previousOffset -> onBottomBarVisibilityChangedState(false)
                    currentOffset < previousOffset && !isAtBottom -> {
                        onBottomBarVisibilityChangedState(true)
                    }
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
        onBottomBarVisibilityChangedState(true)

        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val lastIndex = layoutInfo.totalItemsCount - 1
            val isAtBottom = lastIndex >= 0 && lastVisibleIndex in lastIndex..Int.MAX_VALUE
            val position = listState.firstVisibleItemIndex.toLong() * SCROLL_POSITION_ITEM_MULTIPLIER +
                listState.firstVisibleItemScrollOffset
            Triple(position, listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0, isAtBottom)
        }.collect { (currentPosition, isAtTop, isAtBottom) ->
            when {
                isAtTop -> onBottomBarVisibilityChangedState(true)
                currentPosition > previousPosition -> onBottomBarVisibilityChangedState(false)
                currentPosition < previousPosition && !isAtBottom -> {
                    onBottomBarVisibilityChangedState(true)
                }
            }
            previousPosition = currentPosition
        }
    }
}

private const val SCROLL_POSITION_ITEM_MULTIPLIER = 1_000_000L
