package com.team.yeogibeoryeo.data.favorite.mapper

import com.team.yeogibeoryeo.data.favorite.local.FavoriteEntity
import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType

fun Favorite.toEntity(): FavoriteEntity =
    FavoriteEntity(
        type = type.name,
        targetId = targetId,
        savedAtMillis = savedAtMillis,
    )

fun FavoriteEntity.toDomain(): Favorite? {
    val targetType =
        runCatching {
            FavoriteTargetType.valueOf(type)
        }.getOrNull() ?: return null

    return Favorite(
        type = targetType,
        targetId = targetId,
        savedAtMillis = savedAtMillis,
    )
}
