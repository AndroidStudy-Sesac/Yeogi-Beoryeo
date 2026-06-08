package com.team.yeogibeoryeo.domain.favorite.model

data class Favorite(
    val type: FavoriteTargetType,
    val targetId: String,
    val savedAtMillis: Long,
)
