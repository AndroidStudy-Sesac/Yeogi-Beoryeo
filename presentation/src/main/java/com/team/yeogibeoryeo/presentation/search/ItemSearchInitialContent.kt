package com.team.yeogibeoryeo.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.team.yeogibeoryeo.presentation.search.model.itemUsefulGuideContents
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory

@Composable
fun ItemSearchInitialContent(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onUsefulGuideClick: (ItemUsefulGuideContent) -> Unit,
    regionalGuideSummaryState: HomeRegionalGuideSummaryUiState,
    onRegionalGuideSummaryClick: (String) -> Unit = {},
    onRegionalGuideSummaryRetryClick: () -> Unit = {},
    onQuickCategoryClick: (RepresentativeGuideCategory) -> Unit,
    isQuickCategoryExpanded: Boolean,
    quickCategoryFixedCollapsedItemCount: Int,
    quickCategoryScrollRestoreIndex: Int,
    quickCategoryScrollRestoreOffset: Int,
    quickCategoryScrollRestoreVersion: Int,
    onQuickCategoryMoreClick: (Int, Int, Int) -> Unit,
    onQuickCategoryCollapseClick: () -> Unit,
    onQuickCategoryViewportChanged: () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    var viewportBottomInRootPx by rememberSaveable { mutableIntStateOf(0) }
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
                ItemSearchHeader()
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
                HomeRegionalGuideSummaryBanner(
                    state = regionalGuideSummaryState,
                    onClick = onRegionalGuideSummaryClick,
                    onRetryClick = onRegionalGuideSummaryRetryClick,
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(metrics.sectionVerticalSpace)) {
                    val quickCategoriesTitle = stringResource(R.string.quick_categories)
                    KoreanLineBreakText(
                        text = quickCategoriesTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                    )
                    QuickCategoryGrid(
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
                    )
                }
            }
        }
    }
}

@Composable
fun ItemSearchHeader(
    modifier: Modifier = Modifier,
) {
    val spacing = ItemSearchLayoutDefaults.spacing

    Column(
        modifier = modifier,
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
}
