package com.team.yeogibeoryeo.presentation.regionalguide.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.regionalguide.RegionalGuideCandidateListScrollPosition
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter

@Composable
internal fun <T> RegionalGuideCandidateList(
    candidates: List<T>,
    key: (T) -> Any,
    onCandidateClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp = CANDIDATE_LIST_MAX_HEIGHT,
    scrollStateKey: Any = candidates.candidateListContentKey(key),
    initialScrollPosition: RegionalGuideCandidateListScrollPosition =
        RegionalGuideCandidateListScrollPosition.Initial,
    onScrollPositionChange: (RegionalGuideCandidateListScrollPosition) -> Unit = {},
    itemContent: @Composable (T) -> Unit,
) {
    val restoredScrollPosition = initialScrollPosition.coerceIn(candidates.indices)
    val listState = remember(scrollStateKey) {
        LazyListState(
            firstVisibleItemIndex = restoredScrollPosition.firstVisibleItemIndex,
            firstVisibleItemScrollOffset = restoredScrollPosition.firstVisibleItemScrollOffset,
        )
    }
    val currentOnScrollPositionChange by rememberUpdatedState(onScrollPositionChange)

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .drop(1)
            .filter { isScrollInProgress -> !isScrollInProgress }
            .collect {
                currentOnScrollPositionChange(listState.toScrollPosition())
            }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(
                items = candidates,
                key = { index, candidate -> "${key(candidate)}#$index" }
            ) { index, candidate ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable {
                            currentOnScrollPositionChange(listState.toScrollPosition())
                            onCandidateClick(candidate)
                        }
                ) {
                    itemContent(candidate)
                }

                if (index < candidates.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }

        RegionalGuideCandidateScrollbar(
            listState = listState,
            modifier = Modifier.matchParentSize(),
        )
    }
}

private fun <T> List<T>.candidateListContentKey(key: (T) -> Any): String =
    mapIndexed { index, candidate -> "${key(candidate)}#$index" }
        .joinToString(separator = "|")

private fun LazyListState.toScrollPosition(): RegionalGuideCandidateListScrollPosition =
    RegionalGuideCandidateListScrollPosition(
        firstVisibleItemIndex = firstVisibleItemIndex,
        firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
    )

private fun RegionalGuideCandidateListScrollPosition.coerceIn(
    indices: IntRange
): RegionalGuideCandidateListScrollPosition {
    if (indices.isEmpty()) return RegionalGuideCandidateListScrollPosition.Initial

    return copy(
        firstVisibleItemIndex = firstVisibleItemIndex.coerceIn(indices),
        firstVisibleItemScrollOffset = firstVisibleItemScrollOffset.coerceAtLeast(0),
    )
}

@Composable
private fun RegionalGuideCandidateScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = SCROLLBAR_TRACK_ALPHA)
    val thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = SCROLLBAR_THUMB_ALPHA)

    Box(
        modifier = modifier
            .drawWithCache {
                val layoutInfo = listState.layoutInfo
                val totalItemsCount = layoutInfo.totalItemsCount
                val visibleItemsCount = layoutInfo.visibleItemsInfo.size
                val scrollbarWidth = SCROLLBAR_WIDTH.toPx()
                val scrollbarStartX = size.width - scrollbarWidth - SCROLLBAR_END_PADDING.toPx()

                onDrawWithContent {
                    drawContent()

                    if (
                        totalItemsCount == 0 ||
                        visibleItemsCount == 0 ||
                        visibleItemsCount >= totalItemsCount
                    ) {
                        return@onDrawWithContent
                    }

                    val thumbHeight = (size.height * visibleItemsCount / totalItemsCount)
                        .coerceAtLeast(MIN_SCROLLBAR_THUMB_HEIGHT_PX)
                        .coerceAtMost(size.height)
                    val scrollableItemsCount = (totalItemsCount - visibleItemsCount).coerceAtLeast(1)
                    val offsetFraction =
                        listState.firstVisibleItemIndex.toFloat() / scrollableItemsCount
                    val thumbOffset = (size.height - thumbHeight) * offsetFraction
                    val cornerRadius = CornerRadius(scrollbarWidth / 2f, scrollbarWidth / 2f)

                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(x = scrollbarStartX, y = 0f),
                        size = Size(width = scrollbarWidth, height = size.height),
                        cornerRadius = cornerRadius,
                    )
                    drawRoundRect(
                        color = thumbColor,
                        topLeft = Offset(x = scrollbarStartX, y = thumbOffset),
                        size = Size(width = scrollbarWidth, height = thumbHeight),
                        cornerRadius = cornerRadius,
                    )
                }
            },
    )
}

private val CANDIDATE_LIST_MAX_HEIGHT = 260.dp
private val SCROLLBAR_WIDTH = 3.dp
private val SCROLLBAR_END_PADDING = 2.dp
private const val SCROLLBAR_TRACK_ALPHA = 0.35f
private const val SCROLLBAR_THUMB_ALPHA = 0.85f
private const val MIN_SCROLLBAR_THUMB_HEIGHT_PX = 24f
