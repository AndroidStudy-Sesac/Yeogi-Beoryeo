package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.text.withKoreanLineBreakOpportunities
import com.team.yeogibeoryeo.presentation.search.ItemSearchLayoutDefaults
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory

@Composable
fun QuickCategoryGrid(
    categories: List<RepresentativeGuideCategory> = quickCategoryOrder,
    onCategoryClick: (RepresentativeGuideCategory) -> Unit,
    modifier: Modifier = Modifier,
    screenHorizontalPadding: Dp = 0.dp,
    itemContent: @Composable (
        category: RepresentativeGuideCategory,
        onClick: () -> Unit,
    ) -> Unit = { category, onClick ->
        QuickCategoryItem(
            category = category,
            name = category.quickCategoryLabel,
            onClick = onClick,
        )
    },
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val metrics = quickCategoryGridMetrics(maxWidth = maxWidth)
        val columnCount = metrics.columnCount

        CompositionLocalProvider(LocalQuickCategoryGridMetrics provides metrics) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(metrics.verticalSpace),
                horizontalAlignment = Alignment.Start,
            ) {
                categories.chunked(columnCount).forEach { rowCategories ->
                    QuickCategoryRow(
                        categories = rowCategories,
                        columnCount = columnCount,
                        screenHorizontalPadding = screenHorizontalPadding,
                        onCategoryClick = onCategoryClick,
                        itemContent = itemContent,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickCategoryRow(
    categories: List<RepresentativeGuideCategory>,
    columnCount: Int,
    screenHorizontalPadding: Dp,
    onCategoryClick: (RepresentativeGuideCategory) -> Unit,
    itemContent: @Composable (
        category: RepresentativeGuideCategory,
        onClick: () -> Unit,
    ) -> Unit,
) {
    val metrics = LocalQuickCategoryGridMetrics.current

    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            categories.forEach { category ->
                itemContent(category) { onCategoryClick(category) }
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

@Composable
private fun QuickCategoryItem(
    category: RepresentativeGuideCategory,
    name: String,
    onClick: () -> Unit
) {
    val spacing = ItemSearchLayoutDefaults.spacing
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
            .clickable(
                onClickLabel = clickActionLabel,
                role = Role.Button,
                onClick = onClick,
            )
    ) {
        Box(
            modifier = Modifier
                .size(metrics.tileSize)
                .background(
                    color = category.containerColor(),
                    shape = MaterialTheme.shapes.large
                ),
            contentAlignment = Alignment.Center
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
            modifier = Modifier.width(metrics.cellSize),
            style = metrics.labelTextStyle.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            ),
            minLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}
