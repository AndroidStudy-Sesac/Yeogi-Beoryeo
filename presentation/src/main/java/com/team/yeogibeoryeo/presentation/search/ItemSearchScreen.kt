package com.team.yeogibeoryeo.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.effects.BottomBarVisibilityOnScrollEffect
import com.team.yeogibeoryeo.presentation.operationnotice.OperationNoticeBanner
import com.team.yeogibeoryeo.presentation.operationnotice.OperationNoticeUiModel
import com.team.yeogibeoryeo.presentation.search.components.DisposalItemCard
import com.team.yeogibeoryeo.presentation.search.components.EmptySearchResult
import com.team.yeogibeoryeo.presentation.search.components.ItemSearchBar
import com.team.yeogibeoryeo.presentation.search.components.ItemSearchLoadingContent
import com.team.yeogibeoryeo.presentation.search.model.HomeRegionalGuideSummaryUiState
import com.team.yeogibeoryeo.presentation.search.model.ItemUsefulGuideContent
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory
import kotlinx.coroutines.launch

@Composable
fun ItemSearchScreen(
    uiState: ItemSearchUiState,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onGuideClick: (DisposalItemGuide) -> Unit,
    onQuickCategoryClick: (RepresentativeGuideCategory) -> Unit,
    modifier: Modifier = Modifier,
    onRetryClick: () -> Unit = {},
    onSuggestionClick: (String) -> Unit = {},
    regionalGuideSummaryState: HomeRegionalGuideSummaryUiState = HomeRegionalGuideSummaryUiState.NoFavorite,
    onUsefulGuideClick: (ItemUsefulGuideContent) -> Unit = {},
    onRegionalGuideSummaryClick: (String) -> Unit = {},
    onRegionalGuideSearchClick: () -> Unit = {},
    onRegionalGuideSummaryRetryClick: () -> Unit = {},
    onQuickCategoryMoreClick: (Int, Int, Int) -> Unit = { _, _, _ -> },
    onQuickCategoryCollapseClick: () -> Unit = {},
    onQuickCategoryViewportChanged: () -> Unit = {},
    onQuickCategorySettingsClick: (Int) -> Unit = {},
    onSettingsClick: (() -> Unit)? = null,
    hasUnreadNotices: Boolean = false,
    onBackClick: () -> Unit = {},
    operationNotice: OperationNoticeUiModel? = null,
    onOperationNoticeDismiss: (String) -> Unit = {},
    searchResultListState: LazyListState = rememberLazyListState(),
    categoryListState: LazyListState = rememberLazyListState(),
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
    onItemSearchBottomBarScrollEnabledChanged: (Boolean) -> Unit = {},
    isAppGuideActive: Boolean = false,
    appGuideTarget: ItemSearchGuideTarget? = null,
    searchGuideModifier: Modifier = Modifier,
    quickCategoryGuideModifier: Modifier = Modifier,
    usefulGuideModifier: Modifier = Modifier,
) {
    val showsInitialContent =
        isAppGuideActive ||
            (!uiState.hasSearched && !uiState.isLoading && uiState.errorMessageResId == null)
    val showsSearchResults = uiState.guides.isNotEmpty()

    LaunchedEffect(showsInitialContent, showsSearchResults) {
        if (!showsInitialContent) {
            onItemSearchBottomBarScrollEnabledChanged(showsSearchResults)
        }
        if (!showsSearchResults) {
            onBottomBarVisibilityChanged(true)
        }
    }

    if (showsInitialContent) {
        ItemSearchInitialContent(
            query = uiState.query,
            onQueryChange = onQueryChange,
            onSearchClick = onSearchClick,
            onUsefulGuideClick = onUsefulGuideClick,
            regionalGuideSummaryState = regionalGuideSummaryState,
            onRegionalGuideSummaryClick = onRegionalGuideSummaryClick,
            onRegionalGuideSearchClick = onRegionalGuideSearchClick,
            onRegionalGuideSummaryRetryClick = onRegionalGuideSummaryRetryClick,
            onQuickCategoryClick = onQuickCategoryClick,
            onQuickCategorySettingsClick = onQuickCategorySettingsClick,
            quickCategories = uiState.quickCategories,
            selectedQuickCategories = uiState.homeQuickCategories.toSet(),
            isQuickCategoryExpanded = uiState.isQuickCategoryExpanded,
            quickCategoryFixedCollapsedItemCount =
                uiState.quickCategoryFixedCollapsedItemCount,
            quickCategoryScrollRestoreIndex = uiState.quickCategoryScrollRestoreIndex,
            quickCategoryScrollRestoreOffset = uiState.quickCategoryScrollRestoreOffset,
            quickCategoryScrollRestoreVersion = uiState.quickCategoryScrollRestoreVersion,
            onQuickCategoryMoreClick = onQuickCategoryMoreClick,
            onQuickCategoryCollapseClick = onQuickCategoryCollapseClick,
            onQuickCategoryViewportChanged = onQuickCategoryViewportChanged,
            onSettingsClick = onSettingsClick,
            hasUnreadNotices = hasUnreadNotices,
            operationNotice = operationNotice,
            onOperationNoticeDismiss = onOperationNoticeDismiss,
            listState = categoryListState,
            modifier = modifier.statusBarsPadding(),
            onBottomBarVisibilityChanged = onBottomBarVisibilityChanged,
            onItemSearchBottomBarScrollEnabledChanged = onItemSearchBottomBarScrollEnabledChanged,
            appGuideTarget = appGuideTarget,
            searchGuideModifier = searchGuideModifier,
            quickCategoryGuideModifier = quickCategoryGuideModifier,
            usefulGuideModifier = usefulGuideModifier,
        )
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val spacing = ItemSearchLayoutDefaults.spacing
        val metrics = itemSearchScreenMetrics(
            maxWidth = maxWidth,
            maxHeight = maxHeight,
        )
        val coroutineScope = rememberCoroutineScope()

        if (uiState.guides.isNotEmpty()) {
            val showScrollToTopButton by remember(searchResultListState) {
                derivedStateOf {
                    searchResultListState.firstVisibleItemIndex > 0 ||
                        searchResultListState.firstVisibleItemScrollOffset > 0
                }
            }

            BottomBarVisibilityOnScrollEffect(
                listState = searchResultListState,
                onBottomBarVisibilityChanged = onBottomBarVisibilityChanged,
            )

            Column(modifier = Modifier.fillMaxSize()) {
                ItemSearchTopBar(onBackClick = onBackClick)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(bottom = metrics.sectionVerticalSpace),
                ) {
                    ItemSearchBar(
                        keyword = uiState.query,
                        onKeywordChange = onQueryChange,
                        onSearchClick = onSearchClick,
                        placeholder = stringResource(R.string.item_search_query_label),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = metrics.horizontalPadding),
                        iconSize = metrics.searchIconSize,
                    )
                    operationNotice?.let { notice ->
                        OperationNoticeBanner(
                            notice = notice,
                            onDismiss = onOperationNoticeDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = spacing.sm,
                                    start = metrics.horizontalPadding,
                                    end = metrics.horizontalPadding,
                                ),
                        )
                    }
                    uiState.submittedQuery?.let { submittedQuery ->
                        ItemSearchResultQuery(
                            query = submittedQuery,
                            resultCount = uiState.guides.size,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = spacing.xs,
                                    start = metrics.horizontalPadding,
                                    end = metrics.horizontalPadding,
                                ),
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = searchResultListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = metrics.listBottomPadding),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        items(uiState.guides, key = { it.id }) { guide ->
                            DisposalItemCard(
                                guide = guide,
                                onClick = { onGuideClick(guide) },
                                isFavorite = guide.id in uiState.favoriteGuideIds,
                                modifier = Modifier.padding(horizontal = metrics.horizontalPadding),
                            )
                        }
                    }

                    if (showScrollToTopButton) {
                        FloatingActionButton(
                            onClick = {
                                coroutineScope.launch {
                                    searchResultListState.animateScrollToItem(0)
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(spacing.md),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowUp,
                                contentDescription = stringResource(R.string.scroll_to_top_action),
                            )
                        }
                    }
                }
            }
            return@BoxWithConstraints
        }

        val scrollEmptyResult = !uiState.isLoading && uiState.errorMessageResId == null
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(metrics.screenVerticalSpace),
        ) {
            ItemSearchTopBar(onBackClick = onBackClick)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (scrollEmptyResult) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = metrics.horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(metrics.screenVerticalSpace),
            ) {
                ItemSearchBar(
                    keyword = uiState.query,
                    onKeywordChange = onQueryChange,
                    onSearchClick = {
                        onSearchClick()
                    },
                    placeholder = stringResource(R.string.item_search_query_label),
                    modifier = Modifier
                        .fillMaxWidth(),
                    iconSize = metrics.searchIconSize,
                )

                operationNotice?.let { notice ->
                    OperationNoticeBanner(
                        notice = notice,
                        onDismiss = onOperationNoticeDismiss,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Column(
                    modifier = if (scrollEmptyResult) Modifier.fillMaxWidth() else Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(metrics.sectionVerticalSpace),
                ) {
                    if (!uiState.isLoading && uiState.errorMessageResId == null) {
                        uiState.submittedQuery?.let { submittedQuery ->
                            ItemSearchResultQuery(query = submittedQuery)
                        }
                    }

                    when {
                        uiState.isLoading -> {
                            ItemSearchLoadingContent(modifier = Modifier.fillMaxSize())
                        }

                        uiState.errorMessageResId != null -> {
                            EmptySearchResult(
                                title = stringResource(uiState.errorMessageResId),
                                description = stringResource(R.string.retry_later_message),
                                actionLabel = uiState.submittedQuery?.let {
                                    stringResource(R.string.retry_action)
                                },
                                onActionClick = onRetryClick,
                            )
                        }

                        uiState.guides.isEmpty() -> {
                            EmptySearchResult(
                                title = stringResource(R.string.no_search_results_title),
                                description = stringResource(
                                    if (uiState.visibleSuggestedQueries.isEmpty()) {
                                        R.string.no_search_results_description
                                    } else {
                                        R.string.item_search_suggestions_description
                                    },
                                ),
                                suggestedQueries = uiState.visibleSuggestedQueries,
                                onSuggestionClick = onSuggestionClick,
                            )
                        }

                        else -> Unit
                    }
                }
            }
        }
    }
}
