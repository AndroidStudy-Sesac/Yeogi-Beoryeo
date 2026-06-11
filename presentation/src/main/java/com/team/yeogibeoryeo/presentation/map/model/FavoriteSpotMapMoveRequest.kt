package com.team.yeogibeoryeo.presentation.map.model

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate

data class FavoriteSpotMapMoveRequest(
    val targetId: String,
    val name: String,
    val type: CollectionSpotType,
    val address: String,
    val detailLocation: String?,
    val coordinate: Coordinate,
)
