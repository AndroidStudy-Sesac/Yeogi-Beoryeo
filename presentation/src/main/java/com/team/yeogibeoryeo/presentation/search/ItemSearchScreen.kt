package com.team.yeogibeoryeo.presentation.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.components.AppBackButton
import com.team.yeogibeoryeo.presentation.common.components.AppTopBar
import com.team.yeogibeoryeo.presentation.common.effects.BottomBarVisibilityOnScrollEffect
import com.team.yeogibeoryeo.presentation.search.components.DisposalItemCard
import com.team.yeogibeoryeo.presentation.search.components.EmptySearchResult
import com.team.yeogibeoryeo.presentation.search.components.ItemSearchBar
import com.team.yeogibeoryeo.presentation.search.components.ItemSearchLoadingContent
import com.team.yeogibeoryeo.presentation.search.model.HomeRegionalGuideSummaryUiState
import com.team.yeogibeoryeo.presentation.search.model.ItemUsefulGuideContent
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory
import kotlinx.coroutines.launch

@Composable
fun ItemSearchRoute(
    onGuideSelected: (DisposalItemGuide) -> Unit,
    onUsefulGuideClick: (ItemUsefulGuideContent) -> Unit,
    onRegionalGuideSummaryClick: (String) -> Unit,
    onQuickCategorySettingsClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialQuery: String? = null,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
    onItemSearchBottomBarScrollEnabledChanged: (Boolean) -> Unit = {},
    isAppGuideActive: Boolean = false,
    appGuideTarget: ItemSearchGuideTarget? = null,
    searchGuideModifier: Modifier = Modifier,
    quickCategoryGuideModifier: Modifier = Modifier,
    usefulGuideModifier: Modifier = Modifier,
    viewModel: ItemSearchViewModel = hiltViewModel(),
    regionalGuideSummaryViewModel: HomeRegionalGuideSummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val regionalGuideSummaryState by
        regionalGuideSummaryViewModel.uiState.collectAsStateWithLifecycle()
    val currentOnGuideSelected by rememberUpdatedState(onGuideSelected)

    val searchResultListState = rememberLazyListState()
    val categoryListState = rememberLazyListState()
    var handledSearchResultVersion by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(uiState.searchResultVersion) {
        if (
            uiState.searchResultVersion != handledSearchResultVersion &&
            uiState.hasSearched &&
            uiState.guides.isNotEmpty()
        ) {
            searchResultListState.scrollToItem(0)
            handledSearchResultVersion = uiState.searchResultVersion
        }
    }

    LaunchedEffect(initialQuery) {
        viewModel.searchInitialQueryIfNeeded(initialQuery)
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is ItemSearchEvent.NavigateToGuide -> currentOnGuideSelected(event.guide)
            }
        }
    }

    if (uiState.hasSearched) {
        BackHandler(onBack = viewModel::clearSearch)
    }

    ItemSearchScreen(
        uiState = uiState,
        regionalGuideSummaryState = regionalGuideSummaryState,
        onQueryChange = viewModel::onQueryChange,
        onSearchClick = viewModel::search,
        onGuideClick = onGuideSelected,
        onUsefulGuideClick = onUsefulGuideClick,
        onRegionalGuideSummaryClick = onRegionalGuideSummaryClick,
        onRegionalGuideSummaryRetryClick = regionalGuideSummaryViewModel::retry,
        onQuickCategoryClick = viewModel::openCategoryGuide,
        onQuickCategoryMoreClick = viewModel::expandQuickCategory,
        onQuickCategoryCollapseClick = viewModel::collapseQuickCategory,
        onQuickCategoryViewportChanged =
            viewModel::resetQuickCategoryFixedCollapsedItemCountIfCollapsed,
        onQuickCategorySettingsClick = onQuickCategorySettingsClick,
        onSettingsClick = onSettingsClick,
        onBackClick = viewModel::clearSearch,
        searchResultListState = searchResultListState,
        categoryListState = categoryListState,
        onBottomBarVisibilityChanged = onBottomBarVisibilityChanged,
        onItemSearchBottomBarScrollEnabledChanged = onItemSearchBottomBarScrollEnabledChanged,
        isAppGuideActive = isAppGuideActive,
        appGuideTarget = appGuideTarget,
        searchGuideModifier = searchGuideModifier,
        quickCategoryGuideModifier = quickCategoryGuideModifier,
        usefulGuideModifier = usefulGuideModifier,
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemSearchScreen(
    uiState: ItemSearchUiState,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onGuideClick: (DisposalItemGuide) -> Unit,
    onQuickCategoryClick: (RepresentativeGuideCategory) -> Unit,
    modifier: Modifier = Modifier,
    regionalGuideSummaryState: HomeRegionalGuideSummaryUiState = HomeRegionalGuideSummaryUiState.NoFavorite,
    onUsefulGuideClick: (ItemUsefulGuideContent) -> Unit = {},
    onRegionalGuideSummaryClick: (String) -> Unit = {},
    onRegionalGuideSummaryRetryClick: () -> Unit = {},
    onQuickCategoryMoreClick: (Int, Int, Int) -> Unit = { _, _, _ -> },
    onQuickCategoryCollapseClick: () -> Unit = {},
    onQuickCategoryViewportChanged: () -> Unit = {},
    onQuickCategorySettingsClick: (Int) -> Unit = {},
    onSettingsClick: (() -> Unit)? = null,
    onBackClick: () -> Unit = {},
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
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()

        if (uiState.guides.isNotEmpty()) {
            val statusBarTopPadding = with(density) {
                WindowInsets.statusBars.getTop(this).toDp()
            }
            val stuckHeaderTopPadding = statusBarTopPadding + spacing.xs
            val isSearchResultHeaderStuck by remember(searchResultListState) {
                derivedStateOf {
                    searchResultListState.firstVisibleItemIndex >= SearchResultHeaderItemIndex
                }
            }
            val searchResultHeaderTopPadding by animateDpAsState(
                targetValue = if (isSearchResultHeaderStuck) stuckHeaderTopPadding else 0.dp,
                label = "searchResultHeaderTopPadding",
            )
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

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = searchResultListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = metrics.listBottomPadding),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    item {
                        ItemSearchTopBar(onBackClick = onBackClick)
                    }

                    stickyHeader {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(SearchResultHeaderZIndex)
                                .background(MaterialTheme.colorScheme.background)
                                .padding(bottom = metrics.sectionVerticalSpace)
                        ) {
                            ItemSearchBar(
                                keyword = uiState.query,
                                onKeywordChange = onQueryChange,
                                onSearchClick = {
                                    onSearchClick()
                                },
                                placeholder = stringResource(R.string.item_search_query_label),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = searchResultHeaderTopPadding)
                                    .padding(horizontal = metrics.horizontalPadding),
                                iconSize = metrics.searchIconSize,
                            )
                        }
                    }

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
            return@BoxWithConstraints
        }

        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(metrics.screenVerticalSpace),
        ) {
            ItemSearchTopBar(onBackClick = onBackClick)

            Column(
                modifier = Modifier
                    .fillMaxSize()
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

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(metrics.sectionVerticalSpace),
                ) {
                    when {
                        uiState.isLoading -> {
                            ItemSearchLoadingContent(modifier = Modifier.fillMaxSize())
                        }

                        uiState.errorMessageResId != null -> {
                            EmptySearchResult(
                                title = stringResource(uiState.errorMessageResId),
                                description = stringResource(R.string.retry_later_message),
                            )
                        }

                        uiState.guides.isEmpty() -> {
                            EmptySearchResult(
                                title = stringResource(R.string.no_search_results_title),
                                description = stringResource(R.string.no_search_results_description),
                            )
                        }

                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemSearchTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppTopBar(
        modifier = modifier,
        navigationIcon = {
            AppBackButton(onClick = onBackClick)
        },
        title = {
            Text(
                text = stringResource(R.string.item_search_screen_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
    )
}

private const val SearchResultHeaderItemIndex = 1
private const val SearchResultHeaderZIndex = 1f
