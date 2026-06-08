package com.team.yeogibeoryeo.presentation.favorites

import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteTab
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteUiModel

data class FavoritesUiState(
    val isLoading: Boolean = false,
    val selectedTab: FavoriteTab = FavoriteTab.ITEM_GUIDE,
    val itemGuideFavorites: List<FavoriteUiModel> = emptyList(),
    val collectionSpotFavorites: List<FavoriteUiModel> = emptyList(),
    val regionalGuideFavorites: List<FavoriteUiModel> = emptyList(),
) {
    val selectedFavorites: List<FavoriteUiModel>
        get() =
            when (selectedTab) {
                FavoriteTab.ITEM_GUIDE -> itemGuideFavorites
                FavoriteTab.COLLECTION_SPOT -> collectionSpotFavorites
                FavoriteTab.REGIONAL_GUIDE -> regionalGuideFavorites
            }
}
