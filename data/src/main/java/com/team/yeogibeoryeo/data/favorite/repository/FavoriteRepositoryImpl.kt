package com.team.yeogibeoryeo.data.favorite.repository

import com.team.yeogibeoryeo.data.favorite.local.FavoriteDao
import com.team.yeogibeoryeo.data.favorite.mapper.toDomain
import com.team.yeogibeoryeo.data.favorite.mapper.toEntity
import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoriteRepositoryImpl
    @Inject
    constructor(
        private val favoriteDao: FavoriteDao,
    ) : FavoriteRepository {
        override fun observeFavorites(): Flow<List<Favorite>> =
            favoriteDao.observeFavorites()
                .map { entities -> entities.mapNotNull { it.toDomain() } }

        override fun observeFavorite(
            type: FavoriteTargetType,
            targetId: String,
        ): Flow<Boolean> = favoriteDao.observeFavorite(type.name, targetId)

        override suspend fun isFavorite(
            type: FavoriteTargetType,
            targetId: String,
        ): Boolean = favoriteDao.isFavorite(type.name, targetId)

        override suspend fun toggleFavorite(favorite: Favorite): Boolean =
            favoriteDao.toggleFavorite(favorite.toEntity())

        override suspend fun addFavorite(favorite: Favorite) {
            favoriteDao.upsertFavorite(favorite.toEntity())
        }

        override suspend fun removeFavorite(
            type: FavoriteTargetType,
            targetId: String,
        ) {
            favoriteDao.deleteFavorite(type.name, targetId)
        }
    }
