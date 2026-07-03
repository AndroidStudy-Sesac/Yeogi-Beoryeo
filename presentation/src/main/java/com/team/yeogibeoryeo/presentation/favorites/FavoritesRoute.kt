package com.team.yeogibeoryeo.presentation.favorites

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteCollectionSpotMapMoveRequest

@Composable
fun FavoritesRoute(
    onItemGuideClick: (String) -> Unit,
    onCollectionSpotClick: (FavoriteCollectionSpotMapMoveRequest) -> Unit,
    onRegionalGuideClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FavoritesScreen(
        uiState = uiState,
        onTabClick = viewModel::selectTab,
        onItemGuideClick = onItemGuideClick,
        onCollectionSpotClick = onCollectionSpotClick,
        onRegionalGuideClick = onRegionalGuideClick,
        onCollectionSpotFavoriteRemoveClick = viewModel::removeCollectionSpotFavorite,
        onRegionalGuideFavoriteRemoveClick = viewModel::removeRegionalGuideFavorite,
        modifier = modifier.statusBarsPadding(),
    )
}
