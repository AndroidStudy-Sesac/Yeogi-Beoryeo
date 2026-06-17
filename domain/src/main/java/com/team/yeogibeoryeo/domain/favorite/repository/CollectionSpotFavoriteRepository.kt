package com.team.yeogibeoryeo.domain.favorite.repository

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot

interface CollectionSpotFavoriteRepository {
    suspend fun toggleFavorite(spot: CollectionSpot): Boolean

    suspend fun removeFavorite(targetId: String)
}
