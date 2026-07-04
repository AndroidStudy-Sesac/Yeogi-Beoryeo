package com.team.yeogibeoryeo.presentation.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.team.yeogibeoryeo.common.R as CommonR
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.search.components.containerColor
import com.team.yeogibeoryeo.presentation.search.components.iconResId
import com.team.yeogibeoryeo.presentation.search.components.iconTint
import com.team.yeogibeoryeo.presentation.search.components.quickCategoryOrder
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory

@Composable
fun QuickCategorySettingsRoute(
    maxSelectedCount: Int,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ItemSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val boundedMaxSelectedCount = maxSelectedCount.coerceAtLeast(0)
    val selectedCategories = uiState.homeQuickCategories.take(boundedMaxSelectedCount).toSet()

    LaunchedEffect(boundedMaxSelectedCount) {
        viewModel.limitHomeQuickCategories(boundedMaxSelectedCount)
    }

    QuickCategorySettingsScreen(
        selectedCategories = selectedCategories,
        maxSelectedCount = boundedMaxSelectedCount,
        onCategoryClick = { category ->
            viewModel.toggleHomeQuickCategory(category, boundedMaxSelectedCount)
        },
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickCategorySettingsScreen(
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
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.quick_category_settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = CommonR.drawable.ic_action_back),
                            contentDescription = stringResource(R.string.back_action),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
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

@Composable
private fun SelectedCategorySummary(
    selectedCount: Int,
    maxSelectedCount: Int,
    modifier: Modifier = Modifier,
) {
    val spacing = ItemSearchLayoutDefaults.spacing

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = stringResource(R.string.quick_category_selected_count, selectedCount, maxSelectedCount),
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun QuickCategoryDisplayRow(
    category: RepresentativeGuideCategory,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ItemSearchLayoutDefaults.spacing
    val size = ItemSearchLayoutDefaults.size
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = isSelected,
                enabled = enabled,
                role = Role.Checkbox,
            ) {
                onClick()
            },
        shape = MaterialTheme.shapes.large,
        color =
            if (isSelected) {
                colorScheme.secondaryContainer
            } else {
                colorScheme.surface
            },
        border = BorderStroke(
            width = ItemSearchLayoutDefaults.stroke.outline,
            color =
                if (isSelected) {
                    colorScheme.outline
                } else {
                    colorScheme.outlineVariant
                },
        ),
    ) {
        Row(
            modifier = Modifier.padding(spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(QuickCategoryIconContainerSize)
                    .background(
                        color = category.containerColor(),
                        shape = MaterialTheme.shapes.medium,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = category.iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(size.iconStandard),
                    tint = category.iconTint(),
                )
            }
            Text(
                text = category.displayName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color =
                    if (isSelected) {
                        colorScheme.onSecondaryContainer
                    } else {
                        colorScheme.onSurface
                    },
            )
            SelectionIndicator(isSelected = isSelected, enabled = enabled)
        }
    }
}

@Composable
private fun SelectionIndicator(
    isSelected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.size(QuickCategorySelectionControlSize),
        shape = MaterialTheme.shapes.extraLarge,
        color =
            if (isSelected) {
                colorScheme.primary
            } else {
                colorScheme.surface
            },
        border =
            if (isSelected) {
                null
            } else {
                BorderStroke(
                    width = ItemSearchLayoutDefaults.stroke.outline,
                    color =
                        if (enabled) {
                            colorScheme.outline
                        } else {
                            colorScheme.outlineVariant
                        },
                )
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(ItemSearchLayoutDefaults.size.iconSmall),
                    tint = colorScheme.onPrimary,
                )
            }
        }
    }
}

private val QuickCategoryIconContainerSize = 44.dp
private val QuickCategorySelectionControlSize = 28.dp
