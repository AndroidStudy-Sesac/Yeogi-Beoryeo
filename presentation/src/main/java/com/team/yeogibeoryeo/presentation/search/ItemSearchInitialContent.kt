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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.text.koreanLineBreakSemantics
import com.team.yeogibeoryeo.presentation.common.text.withKoreanLineBreakOpportunities
import com.team.yeogibeoryeo.presentation.search.components.ItemSearchBar
import com.team.yeogibeoryeo.presentation.search.components.QuickCategoryGrid
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory

@Composable
fun ItemSearchInitialContent(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onQuickCategoryClick: (RepresentativeGuideCategory) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val spacing = ItemSearchLayoutDefaults.spacing

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
                .padding(top = metrics.topPadding),
            contentPadding = PaddingValues(bottom = metrics.listBottomPadding),
            verticalArrangement = Arrangement.spacedBy(metrics.screenVerticalSpace),
        ) {
            item {
                ItemSearchHeader()
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
                    Text(
                        text = quickCategoriesTitle.withKoreanLineBreakOpportunities(),
                        modifier = Modifier.koreanLineBreakSemantics(quickCategoriesTitle),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                    )
                    QuickCategoryGrid(
                        onCategoryClick = onQuickCategoryClick,
                        screenHorizontalPadding = metrics.horizontalPadding,
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
