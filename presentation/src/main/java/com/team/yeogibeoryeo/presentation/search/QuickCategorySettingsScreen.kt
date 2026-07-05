package com.team.yeogibeoryeo.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.components.AppBackButton
import com.team.yeogibeoryeo.presentation.common.components.AppTopBar
import com.team.yeogibeoryeo.presentation.search.components.QuickCategoryDisplayRow
import com.team.yeogibeoryeo.presentation.search.components.SelectedCategorySummary
import com.team.yeogibeoryeo.presentation.search.components.quickCategoryOrder
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory

@Composable
internal fun QuickCategorySettingsScreen(
    selectedCategories: Set<RepresentativeGuideCategory>,
    maxSelectedCount: Int,
    onCategoryClick: (RepresentativeGuideCategory) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ItemSearchLayoutDefaults.spacing
    var keyword by rememberSaveable { mutableStateOf("") }
    val categories =
        quickCategoryOrder.filter { category ->
            keyword.isBlank() ||
                category.displayName.contains(keyword, ignoreCase = true) ||
                category.representativeGuideName.contains(keyword, ignoreCase = true)
        }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            AppTopBar(
                navigationIcon = {
                    AppBackButton(onClick = onBackClick)
                },
                title = {
                    Text(
                        text = stringResource(R.string.quick_category_settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = spacing.xl),
            contentPadding = PaddingValues(bottom = spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    Text(
                        text = stringResource(R.string.quick_category_settings_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        placeholder = {
                            Text(text = stringResource(R.string.quick_category_search_placeholder))
                        },
                        singleLine = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                    )
                    SelectedCategorySummary(
                        selectedCount = selectedCategories.size,
                        maxSelectedCount = maxSelectedCount,
                    )
                }
            }

            items(categories, key = { it.name }) { category ->
                val isSelected = category in selectedCategories
                val canSelect = isSelected || selectedCategories.size < maxSelectedCount

                QuickCategoryDisplayRow(
                    category = category,
                    isSelected = isSelected,
                    enabled = canSelect,
                    onClick = {
                        if (canSelect) {
                            onCategoryClick(category)
                        }
                    },
                )
            }
        }
    }
}
