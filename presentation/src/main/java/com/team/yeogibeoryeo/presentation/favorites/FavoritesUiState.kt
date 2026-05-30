package com.team.yeogibeoryeo.presentation.favorites

import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteItemUiModel

data class FavoritesUiState(
    val isLoading: Boolean = false,
    val favorites: List<FavoriteItemUiModel> = emptyList(),
)
