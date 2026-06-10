package com.team.yeogibeoryeo.domain.favorite.model

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate

data class CollectionSpotFavoriteSnapshot(
    val targetId: String,
    val name: String,
    val type: CollectionSpotType,
    val address: String,
    val detailLocation: String?,
    val coordinate: Coordinate?,
)

fun CollectionSpot.toFavoriteSnapshot(): CollectionSpotFavoriteSnapshot =
    CollectionSpotFavoriteSnapshot(
        targetId = id,
        name = name,
        type = type,
        address = address,
        detailLocation = detailLocation,
        coordinate = coordinate,
    )
