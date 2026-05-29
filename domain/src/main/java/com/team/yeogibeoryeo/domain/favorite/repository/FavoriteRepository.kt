package com.team.yeogibeoryeo.domain.favorite.repository

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun observeFavorites(): Flow<List<Favorite>>

    fun observeFavorite(
        type: FavoriteTargetType,
        targetId: String,
    ): Flow<Boolean>

    suspend fun isFavorite(
        type: FavoriteTargetType,
        targetId: String,
    ): Boolean

    suspend fun toggleFavorite(favorite: Favorite): Boolean

    suspend fun addFavorite(favorite: Favorite)

    suspend fun removeFavorite(
        type: FavoriteTargetType,
        targetId: String,
    )
}
