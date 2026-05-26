package com.team.yeogibeoryeo.presentation.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalInstruction
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.model.DisposalSubCategory
import com.team.yeogibeoryeo.presentation.common.design.AppAccentColors
import com.team.yeogibeoryeo.presentation.search.components.DisposalItemCard
import com.team.yeogibeoryeo.presentation.search.components.EmptySearchResult
import com.team.yeogibeoryeo.presentation.search.components.QuickCategoryGrid
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory

@Composable
fun ItemSearchRoute(
    viewModel: ItemSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedGuide = uiState.selectedGuide

    val searchResultListState = rememberLazyListState()
    val categoryListState = rememberLazyListState()

    LaunchedEffect(uiState.guides) {
        if (uiState.hasSearched && uiState.guides.isNotEmpty()) {
            searchResultListState.scrollToItem(0)
        }
    }

    if (selectedGuide != null) {
        BackHandler(onBack = viewModel::clearSelectedGuide)
        ItemGuideDetailScreen(
            guide = selectedGuide,
            onBackClick = viewModel::clearSelectedGuide,
        )
        return
    }

    if (uiState.hasSearched) {
        BackHandler(onBack = viewModel::clearSearch)
    }

    ItemSearchScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onSearchClick = viewModel::search,
        onGuideClick = viewModel::selectGuide,
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppAccentColors.ScreenBackground,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // 검색창 (더욱 강조된 검색바 디자인)
            val searchActionDescription = stringResource(R.string.search_action)
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.item_search_query_label),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = AppAccentColors.Gray,
                            fontSize = 16.sp
                        )
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Recycling,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (uiState.query.isEmpty()) AppAccentColors.Gray else AppAccentColors.MainCyan
                    )
                },
                trailingIcon = {
                    Row(
                        modifier = Modifier.padding(end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = AppAccentColors.Gray
                                )
                            }
                        }
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = searchActionDescription,
                                modifier = Modifier
                                    .size(24.dp)
                                    .semantics {
                                        contentDescription = searchActionDescription
                                    },
                                tint = AppAccentColors.MainCyan
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchClick() }),
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppAccentColors.MainCyan,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = AppAccentColors.SoftGray,
                    unfocusedContainerColor = AppAccentColors.SoftGray,
                    cursorColor = AppAccentColors.MainCyan,
                )
            )

            // 검색 결과 또는 제안 섹션
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when {
                    uiState.isLoading -> {
                        val loadingContentDescription = stringResource(R.string.loading_action)
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.semantics {
                                    contentDescription = loadingContentDescription
                                },
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    uiState.errorMessageResId != null -> {
                        EmptySearchResult(
                            title = stringResource(uiState.errorMessageResId),
                            description = stringResource(R.string.retry_later_message),
                        )
                    }

                    !uiState.hasSearched -> {
                        LazyColumn(
                            state = categoryListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            item {
                                Text(
                                    text = stringResource(R.string.quick_categories),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = AppAccentColors.DarkSlate
                                    ),
                                )
                            }
                            item {
                                QuickCategoryGrid(onCategoryClick = onQuickCategoryClick)
                            }
                        }
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
                                fontSize = 16.sp,
                                color = AppAccentColors.DarkSlate
                            ),
                        )
                        LazyColumn(
                            state = searchResultListState,
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(uiState.guides, key = { it.id }) { guide ->
                                DisposalItemCard(
                                    guide = guide,
                                    onClick = { onGuideClick(guide) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

