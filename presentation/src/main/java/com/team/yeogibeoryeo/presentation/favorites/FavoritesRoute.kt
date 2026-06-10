package com.team.yeogibeoryeo.presentation.favorites

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier

@Composable
fun FavoritesRoute(
    onItemGuideClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FavoritesScreen(
        uiState = uiState,
        onTabClick = viewModel::selectTab,
        onItemGuideClick = onItemGuideClick,
        onCollectionSpotClick = {},
        onRegionalGuideClick = {},
        onCollectionSpotFavoriteRemoveClick = viewModel::removeCollectionSpotFavorite,
        modifier = modifier,
    )
}
