package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.team.yeogibeoryeo.presentation.search.ItemSearchLayoutDefaults
import com.team.yeogibeoryeo.presentation.search.model.ItemUsefulGuideContent
import com.team.yeogibeoryeo.presentation.search.model.ItemUsefulGuideType
import kotlin.math.abs

@Composable
fun ItemUsefulGuideBannerRow(
    guides: List<ItemUsefulGuideContent>,
    onGuideClick: (ItemUsefulGuideContent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ItemSearchLayoutDefaults.spacing
    val size = ItemSearchLayoutDefaults.size
    val fraction = ItemSearchLayoutDefaults.fraction
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val selectedIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo
                .minByOrNull { item ->
                    abs(item.offset + item.size / 2 - viewportCenter)
                }
                ?.index
                ?.coerceIn(guides.indices)
                ?: 0
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            items(guides, key = { it.type }) { guide ->
                ItemUsefulGuideBannerCard(
                    guide = guide,
                    onClick = { onGuideClick(guide) },
                    modifier = Modifier
                        .fillParentMaxWidth(fraction.USEFUL_GUIDE_BANNER_WIDTH)
                        .height(size.usefulGuideBannerCardHeight),
                )
            }
        }
        ItemUsefulGuidePageIndicator(
            pageCount = guides.size,
            selectedIndex = selectedIndex,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun ItemUsefulGuideBannerCard(
    guide: ItemUsefulGuideContent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ItemSearchLayoutDefaults.spacing
    val size = ItemSearchLayoutDefaults.size

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(size.minTouchTarget)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = guide.type.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(size.iconStandard),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xxs),
            ) {
                Text(
                    text = stringResource(guide.labelResId),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(guide.titleResId),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(guide.descriptionResId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(size.iconSmall),
            )
        }
    }
}

@Composable
private fun ItemUsefulGuidePageIndicator(
    pageCount: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
) {
    val spacing = ItemSearchLayoutDefaults.spacing
    val size = ItemSearchLayoutDefaults.size
    val alpha = ItemSearchLayoutDefaults.alpha

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == selectedIndex
            val targetColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha.USEFUL_GUIDE_PAGE_INDICATOR_INACTIVE)
            }
            val color by animateColorAsState(targetValue = targetColor)
            val width by animateDpAsState(
                targetValue = if (isSelected) {
                    size.usefulGuidePageIndicatorActiveWidth
                } else {
                    size.usefulGuidePageIndicatorInactiveSize
                },
            )
            Box(
                modifier = Modifier
                    .size(
                        width = width,
                        height = size.usefulGuidePageIndicatorHeight,
                    )
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

private fun ItemUsefulGuideType.icon() =
    when (this) {
        ItemUsefulGuideType.SMALL_E_WASTE -> Icons.Filled.LocationOn
        ItemUsefulGuideType.REGIONAL_GUIDE -> Icons.Filled.Info
        ItemUsefulGuideType.REPRESENTATIVE_CATEGORY -> Icons.AutoMirrored.Filled.List
        ItemUsefulGuideType.ITEM_DICTIONARY -> Icons.Filled.Search
    }
