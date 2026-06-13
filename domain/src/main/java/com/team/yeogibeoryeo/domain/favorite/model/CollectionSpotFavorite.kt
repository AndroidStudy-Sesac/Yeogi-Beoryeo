package com.team.yeogibeoryeo.domain.favorite.model

data class CollectionSpotFavorite(
    val targetId: String,
    val savedAtMillis: Long,
    val snapshot: CollectionSpotFavoriteSnapshot,
)
