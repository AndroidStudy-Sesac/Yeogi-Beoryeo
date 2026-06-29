package com.team.yeogibeoryeo.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.text.KoreanLineBreakText
import com.team.yeogibeoryeo.presentation.search.components.HomeRegionalGuideSummaryBanner
import com.team.yeogibeoryeo.presentation.search.components.ItemSearchBar
import com.team.yeogibeoryeo.presentation.search.components.ItemUsefulGuideBannerRow
import com.team.yeogibeoryeo.presentation.search.components.QuickCategoryGrid
import com.team.yeogibeoryeo.presentation.search.model.HomeRegionalGuideSummaryUiState
import com.team.yeogibeoryeo.presentation.search.model.ItemUsefulGuideContent
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory
import com.team.yeogibeoryeo.presentation.search.model.itemUsefulGuideContents

@Composable
fun ItemSearchInitialContent(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onUsefulGuideClick: (ItemUsefulGuideContent) -> Unit,
    regionalGuideSummaryState: HomeRegionalGuideSummaryUiState,
    modifier: Modifier = Modifier,
    onRegionalGuideSummaryClick: (String) -> Unit = {},
    onRegionalGuideSummaryRetryClick: () -> Unit = {},
    onQuickCategoryClick: (RepresentativeGuideCategory) -> Unit,
    onQuickCategorySettingsClick: (Int) -> Unit,
    quickCategories: List<RepresentativeGuideCategory>,
    selectedQuickCategories: Set<RepresentativeGuideCategory>,
    isQuickCategoryExpanded: Boolean,
    quickCategoryFixedCollapsedItemCount: Int,
    quickCategoryScrollRestoreIndex: Int,
    quickCategoryScrollRestoreOffset: Int,
    quickCategoryScrollRestoreVersion: Int,
    onQuickCategoryMoreClick: (Int, Int, Int) -> Unit,
    onQuickCategoryCollapseClick: () -> Unit,
    onQuickCategoryViewportChanged: () -> Unit,
    onSettingsClick: () -> Unit,
    listState: LazyListState,
) {
    var viewportBottomInRootPx by rememberSaveable { mutableIntStateOf(0) }
    var maxSelectedQuickCategoryCount by rememberSaveable { mutableIntStateOf(quickCategories.size) }
    var handledScrollRestoreVersion by rememberSaveable {
        mutableIntStateOf(quickCategoryScrollRestoreVersion)
    }

    LaunchedEffect(
        isQuickCategoryExpanded,
        quickCategoryScrollRestoreVersion,
    ) {
        if (
            !isQuickCategoryExpanded &&
            quickCategoryScrollRestoreVersion != handledScrollRestoreVersion
        ) {
            listState.scrollToItem(
                index = quickCategoryScrollRestoreIndex,
                scrollOffset = quickCategoryScrollRestoreOffset,
            )
            handledScrollRestoreVersion = quickCategoryScrollRestoreVersion
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val metrics = itemSearchScreenMetrics(maxWidth = maxWidth)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = metrics.horizontalPadding)
                .padding(top = metrics.topPadding)
                .onGloballyPositioned { coordinates ->
                    val measuredViewportBottomInRootPx =
                        coordinates.positionInRoot().y.toInt() + coordinates.size.height
                    if (viewportBottomInRootPx != measuredViewportBottomInRootPx) {
                        viewportBottomInRootPx = measuredViewportBottomInRootPx
                        onQuickCategoryViewportChanged()
                    }
                },
            contentPadding = PaddingValues(bottom = metrics.listBottomPadding),
            verticalArrangement = Arrangement.spacedBy(metrics.screenVerticalSpace),
        ) {
            item {
                ItemSearchHeader(onSettingsClick = onSettingsClick)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(metrics.sectionVerticalSpace)) {
                    KoreanLineBreakText(
                        text = stringResource(R.string.item_useful_guide_section_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                    ItemUsefulGuideBannerRow(
                        guides = itemUsefulGuideContents,
                        onGuideClick = onUsefulGuideClick,
                    )
                }
            }

            item {
                HomeRegionalGuideSummaryBanner(
                    state = regionalGuideSummaryState,
                    onClick = onRegionalGuideSummaryClick,
                    onRetryClick = onRegionalGuideSummaryRetryClick,
                )
            }

            item {
                ItemSearchBar(
                    keyword = query,
                    onKeywordChange = onQueryChange,
                    onSearchClick = onSearchClick,
                    placeholder = stringResource(R.string.item_search_query_label),
                    modifier = Modifier.fillMaxWidth(),
                    iconSize = metrics.searchIconSize,
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(metrics.sectionVerticalSpace)) {
                    val quickCategoriesTitle = stringResource(R.string.quick_categories)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        KoreanLineBreakText(
                            text = quickCategoriesTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                        )
                        AssistChip(
                            onClick = { onQuickCategorySettingsClick(maxSelectedQuickCategoryCount) },
                            label = {
                                Text(text = stringResource(R.string.quick_category_edit_action))
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                )
                            },
                            colors =
                                AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    labelColor = MaterialTheme.colorScheme.primary,
                                    trailingIconContentColor = MaterialTheme.colorScheme.primary,
                                ),
                        )
                    }
                    QuickCategoryGrid(
                        categories = quickCategories,
                        selectedCategories = selectedQuickCategories,
                        onCategoryClick = onQuickCategoryClick,
                        isExpanded = isQuickCategoryExpanded,
                        fixedCollapsedItemCount = quickCategoryFixedCollapsedItemCount,
                        onMoreClick = { collapsedItemCount ->
                            onQuickCategoryMoreClick(
                                collapsedItemCount,
                                listState.firstVisibleItemIndex,
                                listState.firstVisibleItemScrollOffset,
                            )
                        },
                        onCollapseClick = onQuickCategoryCollapseClick,
                        screenHorizontalPadding = metrics.horizontalPadding,
                        viewportBottomInRootPx = viewportBottomInRootPx,
                        onVisibleCategoryCountChange = { maxSelectedQuickCategoryCount = it },
                    )
                }
            }
        }
    }
}

@Composable
fun ItemSearchHeader(
    modifier: Modifier = Modifier,
    onSettingsClick: (() -> Unit)? = null,
) {
    val spacing = ItemSearchLayoutDefaults.spacing

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.item_search_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.item_search_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onSettingsClick != null) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings_action),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
