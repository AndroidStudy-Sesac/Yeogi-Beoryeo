package com.team.yeogibeoryeo.domain.favorite.repository

import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot

interface RegionalGuideFavoriteRepository {
    suspend fun toggleFavorite(snapshot: RegionalGuideFavoriteSnapshot): Boolean

    suspend fun removeFavorite(targetId: String)
}
