package com.team.yeogibeoryeo.presentation.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.presentation.operationnotice.HomeOperationNoticeViewModel
import com.team.yeogibeoryeo.presentation.search.model.ItemUsefulGuideContent

@Composable
fun ItemSearchRoute(
    onGuideSelected: (DisposalItemGuide) -> Unit,
    onUsefulGuideClick: (ItemUsefulGuideContent) -> Unit,
    onRegionalGuideSummaryClick: (String) -> Unit,
    onRegionalGuideSearchClick: () -> Unit,
    onQuickCategorySettingsClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasUnreadNotices: Boolean = false,
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
    operationNoticeViewModel: HomeOperationNoticeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val regionalGuideSummaryState by
        regionalGuideSummaryViewModel.uiState.collectAsStateWithLifecycle()
    val operationNotice by operationNoticeViewModel.notice.collectAsStateWithLifecycle()
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
        onSuggestionClick = viewModel::selectSuggestedQuery,
        onRetryClick = viewModel::retrySearch,
        onGuideClick = onGuideSelected,
        onUsefulGuideClick = onUsefulGuideClick,
        onRegionalGuideSummaryClick = onRegionalGuideSummaryClick,
        onRegionalGuideSearchClick = onRegionalGuideSearchClick,
        onRegionalGuideSummaryRetryClick = regionalGuideSummaryViewModel::retry,
        onQuickCategoryClick = viewModel::openCategoryGuide,
        onQuickCategoryMoreClick = viewModel::expandQuickCategory,
        onQuickCategoryCollapseClick = viewModel::collapseQuickCategory,
        onQuickCategoryViewportChanged =
            viewModel::resetQuickCategoryFixedCollapsedItemCount,
        onQuickCategorySettingsClick = onQuickCategorySettingsClick,
        onSettingsClick = onSettingsClick,
        hasUnreadNotices = hasUnreadNotices,
        onBackClick = viewModel::clearSearch,
        operationNotice = operationNotice,
        onOperationNoticeDismiss = operationNoticeViewModel::dismissNotice,
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
