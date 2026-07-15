package com.team.yeogibeoryeo.presentation.favorites.model

import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType

data class FavoriteUiModel(
    val type: FavoriteTargetType,
    val targetId: String,
    val title: String,
    val subtitle: String?,
    val collectionSpotMapMoveRequest: FavoriteCollectionSpotMapMoveRequest? = null,
    val isHomeRegionalGuidePrimary: Boolean = false,
)
