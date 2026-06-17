package com.team.yeogibeoryeo.presentation.favorites.model

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType

data class FavoriteCollectionSpotMapMoveRequest(
    val targetId: String,
    val name: String,
    val type: CollectionSpotType,
    val address: String,
    val detailLocation: String?,
    val latitude: Double,
    val longitude: Double,
)
