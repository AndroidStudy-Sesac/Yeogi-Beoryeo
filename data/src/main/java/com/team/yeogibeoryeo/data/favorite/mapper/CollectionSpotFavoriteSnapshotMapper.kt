package com.team.yeogibeoryeo.data.favorite.mapper

import com.team.yeogibeoryeo.data.favorite.local.CollectionSpotFavoriteSnapshotEntity
import com.team.yeogibeoryeo.domain.favorite.model.CollectionSpotFavoriteSnapshot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate

fun CollectionSpotFavoriteSnapshot.toEntity(): CollectionSpotFavoriteSnapshotEntity =
    CollectionSpotFavoriteSnapshotEntity(
        targetId = targetId,
        name = name,
        spotType = type.name,
        address = address,
        detailLocation = detailLocation,
        latitude = coordinate?.latitude,
        longitude = coordinate?.longitude,
    )

fun CollectionSpotFavoriteSnapshotEntity.toDomain(): CollectionSpotFavoriteSnapshot? {
    val type = runCatching {
        CollectionSpotType.valueOf(spotType)
    }.getOrNull() ?: return null

    return CollectionSpotFavoriteSnapshot(
        targetId = targetId,
        name = name,
        type = type,
        address = address,
        detailLocation = detailLocation,
        coordinate = latitude?.let { latitude ->
            longitude?.let { longitude ->
                Coordinate(
                    latitude = latitude,
                    longitude = longitude,
                )
            }
        },
    )
}
