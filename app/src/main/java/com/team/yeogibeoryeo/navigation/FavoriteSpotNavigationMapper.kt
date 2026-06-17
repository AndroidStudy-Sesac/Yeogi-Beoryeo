package com.team.yeogibeoryeo.navigation

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteCollectionSpotMapMoveRequest
import com.team.yeogibeoryeo.presentation.map.model.FavoriteSpotMapMoveRequest
import java.util.UUID

internal fun FavoriteCollectionSpotMapMoveRequest.toMapRoute(): MapRoute =
    MapRoute(
        favoriteSpotRequestId = UUID.randomUUID().toString(),
        favoriteSpotTargetId = targetId,
        favoriteSpotName = name,
        favoriteSpotType = type.name,
        favoriteSpotAddress = address,
        favoriteSpotDetailLocation = detailLocation,
        favoriteSpotLatitude = latitude,
        favoriteSpotLongitude = longitude,
    )

internal fun MapRoute.toFavoriteSpotMapMoveRequest(): FavoriteSpotMapMoveRequest? {
    val targetId = favoriteSpotTargetId ?: return null
    val name = favoriteSpotName ?: return null
    val typeName = favoriteSpotType ?: return null
    val address = favoriteSpotAddress ?: return null
    val latitude = favoriteSpotLatitude ?: return null
    val longitude = favoriteSpotLongitude ?: return null
    val type =
        runCatching {
            CollectionSpotType.valueOf(typeName)
        }.getOrNull() ?: return null

    return FavoriteSpotMapMoveRequest(
        requestId = favoriteSpotRequestId ?: targetId,
        targetId = targetId,
        name = name,
        type = type,
        address = address,
        detailLocation = favoriteSpotDetailLocation,
        coordinate = Coordinate(
            latitude = latitude,
            longitude = longitude,
        ),
    )
}
