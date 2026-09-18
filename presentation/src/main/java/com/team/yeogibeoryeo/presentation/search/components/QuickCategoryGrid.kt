package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.text.koreanLineBreakSemantics
import com.team.yeogibeoryeo.presentation.common.text.withKoreanLineBreakOpportunities
import com.team.yeogibeoryeo.presentation.search.ItemSearchLayoutDefaults
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory

@Composable
fun QuickCategoryGrid(
    onCategoryClick: (RepresentativeGuideCategory) -> Unit,
    modifier: Modifier = Modifier,
    categories: List<RepresentativeGuideCategory> = quickCategoryOrder,
    selectedCategories: Set<RepresentativeGuideCategory> = emptySet(),
    screenHorizontalPadding: Dp = 0.dp,
    viewportBottomInRootPx: Int = 0,
    isExpanded: Boolean = true,
    fixedCollapsedItemCount: Int = 0,
    onMoreClick: (Int) -> Unit = {},
    onCollapseClick: () -> Unit = {},
    onVisibleCategoryCountChange: (Int) -> Unit = {},
    collapseBringIntoViewRequestVersion: Int = 0,
    itemContent: @Composable (
        category: RepresentativeGuideCategory,
        onClick: () -> Unit,
    ) -> Unit = { category, onClick ->
        QuickCategoryItem(
            category = category,
            name = category.quickCategoryLabel,
            isSelected = category in selectedCategories,
            onClick = onClick,
        )
    },
) {
    val density = LocalDensity.current
    var measuredViewportBottomInRootPx by remember { mutableIntStateOf(0) }
    var initialAvailableHeightPx by remember { mutableIntStateOf(0) }
    var rowHeightPx by remember { mutableIntStateOf(0) }
    var shouldBringExpandedGridIntoView by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val collapseBringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(isExpanded, shouldBringExpandedGridIntoView) {
        if (isExpanded && shouldBringExpandedGridIntoView) {
            bringIntoViewRequester.bringIntoView()
            shouldBringExpandedGridIntoView = false
        }
    }

    LaunchedEffect(isExpanded, collapseBringIntoViewRequestVersion) {
        if (isExpanded && collapseBringIntoViewRequestVersion > 0) {
            collapseBringIntoViewRequester.bringIntoView()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onGloballyPositioned { coordinates ->
                if (measuredViewportBottomInRootPx != viewportBottomInRootPx) {
                    measuredViewportBottomInRootPx = viewportBottomInRootPx
                    initialAvailableHeightPx = 0
                }
                if (viewportBottomInRootPx > 0 && initialAvailableHeightPx == 0) {
                    initialAvailableHeightPx =
                        (
                            viewportBottomInRootPx -
                                coordinates.positionInRoot().y.toInt()
                            ).coerceAtLeast(0)
                }
            }
    ) {
        val metrics = quickCategoryGridMetrics(maxWidth = maxWidth)
        val columnCount = metrics.columnCount
        val collapseLayout = quickCategoryGridCollapseLayout(
            categoryCount = categories.size,
            columnCount = columnCount,
            availableHeightPx = initialAvailableHeightPx,
            rowHeightPx = rowHeightPx,
            rowSpacingPx = with(density) { metrics.verticalSpace.roundToPx() },
            fixedCollapsedItemCount = fixedCollapsedItemCount,
            isExpanded = isExpanded,
        )

        val visibleCategories =
            if (collapseLayout.showsMore) {
                categories.take(collapseLayout.visibleCategoryCount)
            } else {
                categories
            }
        val collapsedVisibleCategoryCount =
            if (categories.size > collapseLayout.collapsedItemCount) {
                (collapseLayout.collapsedItemCount - 1).coerceAtLeast(0)
            } else {
                categories.size
            }

        LaunchedEffect(collapsedVisibleCategoryCount) {
            onVisibleCategoryCountChange(collapsedVisibleCategoryCount)
        }
        val gridItems =
            buildList {
                addAll(visibleCategories.map(QuickCategoryGridItem::Category))
                when {
                    collapseLayout.showsMore -> add(
                        QuickCategoryGridItem.Toggle(
                            labelResId = R.string.quick_category_more_action,
                            onClick = {
                                shouldBringExpandedGridIntoView = true
                                onMoreClick(collapseLayout.collapsedItemCount)
                            },
                        ),
                    )

                    collapseLayout.showsCollapse -> add(
                        QuickCategoryGridItem.Toggle(
                            labelResId = R.string.quick_category_collapse_action,
                            onClick = onCollapseClick,
                            modifier =
                                Modifier.bringIntoViewRequester(
                                    collapseBringIntoViewRequester,
                                ),
                        ),
                    )
                }
            }

        CompositionLocalProvider(LocalQuickCategoryGridMetrics provides metrics) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(metrics.verticalSpace),
                horizontalAlignment = Alignment.Start,
            ) {
                gridItems.chunked(columnCount).forEach { rowItems ->
                    QuickCategoryRow(
                        items = rowItems,
                        columnCount = columnCount,
                        screenHorizontalPadding = screenHorizontalPadding,
                        onCategoryClick = onCategoryClick,
                        onRowHeightChanged = { height ->
                            if (rowHeightPx != height) {
                                rowHeightPx = height
                            }
                        },
                        itemContent = itemContent,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickCategoryToggleItem(
    labelResId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val size = ItemSearchLayoutDefaults.size
    val metrics = LocalQuickCategoryGridMetrics.current
    val label = stringResource(labelResId)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(metrics.labelSpacing),
        modifier = modifier
            .sizeIn(
                minWidth = size.minTouchTarget,
                minHeight = size.minTouchTarget,
            )
            .width(metrics.cellSize)
    ) {
        QuickCategoryTile(
            contentDescription = label,
            onClickLabel = label,
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Icon(
                imageVector = Icons.Filled.MoreHoriz,
                contentDescription = null,
                modifier = Modifier.size(metrics.iconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = label,
            modifier = Modifier
                .width(metrics.cellSize)
                .koreanLineBreakSemantics(label),
            style = metrics.labelTextStyle.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            ),
            minLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun QuickCategoryRow(
    items: List<QuickCategoryGridItem>,
    columnCount: Int,
    screenHorizontalPadding: Dp,
    onCategoryClick: (RepresentativeGuideCategory) -> Unit,
    onRowHeightChanged: (Int) -> Unit,
    itemContent: @Composable (
        category: RepresentativeGuideCategory,
        onClick: () -> Unit,
    ) -> Unit,
) {
    val metrics = LocalQuickCategoryGridMetrics.current

    Layout(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                onRowHeightChanged(coordinates.size.height)
            },
        content = {
            items.forEach { item ->
                when (item) {
                    is QuickCategoryGridItem.Category ->
                        itemContent(item.category) { onCategoryClick(item.category) }

                    is QuickCategoryGridItem.Toggle ->
                        QuickCategoryToggleItem(
                            labelResId = item.labelResId,
                            onClick = item.onClick,
                            modifier = item.modifier,
                        )
                }
            }
        },
    ) { measurables, constraints ->
        val cellWidth = metrics.cellSize.roundToPx()
        val tileWidth = metrics.tileSize.roundToPx()
        val sidePadding = screenHorizontalPadding.roundToPx()
        val screenWidth = constraints.maxWidth + sidePadding * 2
        val tileGap =
            ((screenWidth - columnCount * tileWidth) / (columnCount + 1))
                .coerceAtLeast(0)

        val placeables = measurables.map { measurable ->
            measurable.measure(
                constraints.copy(
                    minWidth = cellWidth,
                    maxWidth = cellWidth,
                )
            )
        }
        val height = placeables.maxOfOrNull { it.height } ?: 0

        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable ->
                val tileLeftInScreen = tileGap + index * (tileWidth + tileGap)
                val tileLeftInContent = tileLeftInScreen - sidePadding
                val itemLeft = tileLeftInContent + (tileWidth - placeable.width) / 2
                placeable.placeRelative(x = itemLeft, y = 0)
            }
        }
    }
}

private sealed interface QuickCategoryGridItem {
    data class Category(val category: RepresentativeGuideCategory) : QuickCategoryGridItem
    data class Toggle(
        val labelResId: Int,
        val onClick: () -> Unit,
        val modifier: Modifier = Modifier,
    ) : QuickCategoryGridItem
}

@Composable
private fun QuickCategoryTile(
    contentDescription: String,
    onClickLabel: String,
    onClick: () -> Unit,
    containerColor: Color,
    badge: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val metrics = LocalQuickCategoryGridMetrics.current
    val shape = MaterialTheme.shapes.large

    Box(
        modifier = Modifier.size(metrics.tileSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(metrics.tileSize)
                .clip(shape)
                .background(
                    color = containerColor,
                    shape = shape
                )
                .semantics { this.contentDescription = contentDescription }
                .clickable(
                    onClickLabel = onClickLabel,
                    role = Role.Button,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
            content = content,
        )
        badge()
    }
}

@Composable
internal fun QuickCategoryItem(
    category: RepresentativeGuideCategory,
    name: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLabelTextLayout: (TextLayoutResult) -> Unit = {},
) {
    val size = ItemSearchLayoutDefaults.size
    val metrics = LocalQuickCategoryGridMetrics.current
    val clickActionLabel = stringResource(R.string.quick_category_click_action)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(metrics.labelSpacing),
        modifier = Modifier
            .sizeIn(
                minWidth = size.minTouchTarget,
                minHeight = size.minTouchTarget,
            )
            .width(metrics.cellSize)
    ) {
        QuickCategoryTile(
            contentDescription = category.displayName,
            onClickLabel = clickActionLabel,
            onClick = onClick,
            containerColor = category.containerColor(),
            badge = {
                if (isSelected) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(
                                x = size.categorySelectionBadgeOffset,
                                y = -size.categorySelectionBadgeOffset,
                            )
                            .size(size.categorySelectionBadge),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(
                            width = ItemSearchLayoutDefaults.stroke.outline,
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(size.categorySelectionBadgeIcon),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            },
        ) {
            Icon(
                painter = painterResource(id = category.iconResId),
                // 항목의 Text와 click action이 의미를 제공하므로 아이콘은 중복 읽기를 피합니다.
                contentDescription = null,
                modifier = Modifier.size(metrics.iconSize),
                tint = category.iconTint()
            )
        }
        Text(
            text = name.withKoreanLineBreakOpportunities(),
            modifier = Modifier
                .width(metrics.cellSize)
                .koreanLineBreakSemantics(name),
            style = metrics.labelTextStyle.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            ),
            minLines = 2,
            onTextLayout = onLabelTextLayout,
            textAlign = TextAlign.Center,
        )
    }
}
