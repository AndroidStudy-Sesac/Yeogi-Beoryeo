package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.text.koreanTextLineBreak
import com.team.yeogibeoryeo.presentation.search.ItemSearchLayoutDefaults
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory

@Composable
fun QuickCategoryGrid(
    categories: List<RepresentativeGuideCategory> = quickCategoryOrder,
    onCategoryClick: (RepresentativeGuideCategory) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (
        category: RepresentativeGuideCategory,
        onClick: () -> Unit,
    ) -> Unit = { category, onClick ->
        QuickCategoryItem(
            category = category,
            name = category.displayName,
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        rowCategories.forEach { category ->
                            itemContent(
                                category,
                                { onCategoryClick(category) },
                            )
                        }
                        repeat(columnCount - rowCategories.size) {
                            Spacer(modifier = Modifier.width(metrics.cellSize))
                        }
                    }
                }
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
            text = name,
            modifier = Modifier.width(metrics.cellSize),
            style = metrics.labelTextStyle.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                lineBreak = koreanTextLineBreak,
            ),
            minLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}
