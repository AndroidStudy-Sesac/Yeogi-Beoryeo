package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.sizeIn
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.search.ItemSearchLayoutDefaults
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory

@Composable
fun QuickCategoryGrid(
    categories: List<RepresentativeGuideCategory> = quickCategoryOrder,
    onCategoryClick: (RepresentativeGuideCategory) -> Unit,
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
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val spacing = ItemSearchLayoutDefaults.spacing
        val size = ItemSearchLayoutDefaults.size
        val itemWidth = size.categoryCell
        val horizontalSpace = spacing.md
        val columnCount =
            ((maxWidth + horizontalSpace) / (itemWidth + horizontalSpace))
                .toInt()
                .coerceIn(1, 4)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
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
                        Spacer(modifier = Modifier.width(itemWidth))
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
    val clickActionLabel = stringResource(R.string.quick_category_click_action)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
        modifier = Modifier
            .sizeIn(
                minWidth = size.minTouchTarget,
                minHeight = size.minTouchTarget,
            )
            .width(size.categoryCell)
            .clickable(
                onClickLabel = clickActionLabel,
                role = Role.Button,
                onClick = onClick,
            )
    ) {
        Box(
            modifier = Modifier
                .size(size.categoryTile)
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
                modifier = Modifier.size(size.iconLarge),
                tint = category.iconTint()
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            ),
            minLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}

private val quickCategoryOrder =
    listOf(
        RepresentativeGuideCategory.PAPER,
        RepresentativeGuideCategory.PAPER_PACK,
        RepresentativeGuideCategory.COLORLESS_PET,
        RepresentativeGuideCategory.PLASTIC,
        RepresentativeGuideCategory.VINYL,
        RepresentativeGuideCategory.STYROFOAM,
        RepresentativeGuideCategory.GLASS,
        RepresentativeGuideCategory.METAL,
        RepresentativeGuideCategory.CLOTHING,
        RepresentativeGuideCategory.BATTERY,
        RepresentativeGuideCategory.LIGHTING,
        RepresentativeGuideCategory.ELECTRONICS,
        RepresentativeGuideCategory.FOOD_WASTE,
        RepresentativeGuideCategory.GENERAL,
        RepresentativeGuideCategory.NON_COMBUSTIBLE,
        RepresentativeGuideCategory.LARGE_WASTE,
        RepresentativeGuideCategory.CONSTRUCTION_WASTE,
        RepresentativeGuideCategory.HAZARDOUS,
        RepresentativeGuideCategory.OTHER,
    )
