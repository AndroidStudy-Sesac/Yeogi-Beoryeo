package com.team.yeogibeoryeo.data.favorite.repository

import androidx.room.withTransaction
import com.team.yeogibeoryeo.data.favorite.local.FavoriteDatabase
import com.team.yeogibeoryeo.data.favorite.mapper.toEntity
import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.model.toFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteRepository
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import javax.inject.Inject

class CollectionSpotFavoriteRepositoryImpl
    @Inject
    constructor(
        private val database: FavoriteDatabase,
    ) : CollectionSpotFavoriteRepository {
        override suspend fun toggleFavorite(spot: CollectionSpot): Boolean =
            database.withTransaction {
                val favoriteDao = database.favoriteDao()
                val snapshotDao = database.collectionSpotFavoriteSnapshotDao()
                val favorite =
                    Favorite(
                        type = FavoriteTargetType.COLLECTION_SPOT,
                        targetId = spot.id,
                        savedAtMillis = System.currentTimeMillis(),
                    )

                if (favoriteDao.isFavorite(favorite.type.name, favorite.targetId)) {
                    favoriteDao.deleteFavorite(favorite.type.name, favorite.targetId)
                    snapshotDao.deleteSnapshot(spot.id)
                    false
                } else {
                    favoriteDao.upsertFavorite(favorite.toEntity())
                    snapshotDao.upsertSnapshot(spot.toFavoriteSnapshot().toEntity())
                    true
                }
            }

        override suspend fun removeFavorite(targetId: String) {
            database.withTransaction {
                database.favoriteDao().deleteFavorite(
                    type = FavoriteTargetType.COLLECTION_SPOT.name,
                    targetId = targetId,
                )
                database.collectionSpotFavoriteSnapshotDao().deleteSnapshot(targetId)
            }
        }
    }
