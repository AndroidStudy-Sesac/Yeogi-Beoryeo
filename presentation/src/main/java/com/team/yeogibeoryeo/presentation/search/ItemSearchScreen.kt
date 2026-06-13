package com.team.yeogibeoryeo.presentation.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.search.components.DisposalItemCard
import com.team.yeogibeoryeo.presentation.search.components.EmptySearchResult
import com.team.yeogibeoryeo.presentation.search.components.ItemSearchBar
import com.team.yeogibeoryeo.presentation.search.components.ItemSearchLoadingContent
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory

@Composable
fun ItemSearchRoute(
    initialQuery: String? = null,
    onGuideSelected: (DisposalItemGuide) -> Unit,
    viewModel: ItemSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
        if (!initialQuery.isNullOrBlank()) {
            viewModel.search(initialQuery)
        }
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
        onQueryChange = viewModel::onQueryChange,
        onSearchClick = viewModel::search,
        onGuideClick = onGuideSelected,
        onQuickCategoryClick = viewModel::openCategoryGuide,
        searchResultListState = searchResultListState,
        categoryListState = categoryListState,
    )
}

@Composable
fun ItemSearchScreen(
    uiState: ItemSearchUiState,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onGuideClick: (DisposalItemGuide) -> Unit,
    onQuickCategoryClick: (RepresentativeGuideCategory) -> Unit,
    modifier: Modifier = Modifier,
    searchResultListState: LazyListState = rememberLazyListState(),
    categoryListState: LazyListState = rememberLazyListState(),
) {
    val spacing = ItemSearchLayoutDefaults.spacing

    if (!uiState.hasSearched && !uiState.isLoading && uiState.errorMessageResId == null) {
        ItemSearchInitialContent(
            query = uiState.query,
            onQueryChange = onQueryChange,
            onSearchClick = onSearchClick,
            onQuickCategoryClick = onQuickCategoryClick,
            listState = categoryListState,
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = spacing.xl)
            .padding(top = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
        ItemSearchHeader()

        ItemSearchBar(
            keyword = uiState.query,
            onKeywordChange = onQueryChange,
            onSearchClick = {
                onSearchClick()
            },
            placeholder = stringResource(R.string.item_search_query_label),
            modifier = Modifier.fillMaxWidth(),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
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

                else -> {
                    Text(
                        text = stringResource(
                            R.string.search_results_count,
                            uiState.guides.size
                        ),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                    )
                    LazyColumn(
                        state = searchResultListState,
                        contentPadding = PaddingValues(bottom = spacing.xl),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        items(uiState.guides, key = { it.id }) { guide ->
                            DisposalItemCard(
                                guide = guide,
                                onClick = { onGuideClick(guide) },
                                isFavorite = guide.id in uiState.favoriteGuideIds,
                            )
                        }
                    }
                }
            }
        }
    }
}
