package com.team.yeogibeoryeo.presentation.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun QuickCategorySettingsRoute(
    maxSelectedCount: Int,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ItemSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val boundedMaxSelectedCount = maxSelectedCount.coerceAtLeast(0)
    val selectedCategories = uiState.homeQuickCategories.toSet()

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
