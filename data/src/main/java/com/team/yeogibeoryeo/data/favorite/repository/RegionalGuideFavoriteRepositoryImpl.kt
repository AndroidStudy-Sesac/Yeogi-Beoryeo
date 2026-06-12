package com.team.yeogibeoryeo.data.favorite.repository

import androidx.room.withTransaction
import com.team.yeogibeoryeo.data.favorite.local.FavoriteDatabase
import com.team.yeogibeoryeo.data.favorite.mapper.toEntity
import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteRepository
import javax.inject.Inject

class RegionalGuideFavoriteRepositoryImpl
    @Inject
    constructor(
        private val database: FavoriteDatabase,
    ) : RegionalGuideFavoriteRepository {
        override suspend fun toggleFavorite(snapshot: RegionalGuideFavoriteSnapshot): Boolean =
            database.withTransaction {
                val favoriteDao = database.favoriteDao()
                val snapshotDao = database.regionalGuideFavoriteSnapshotDao()
                val favorite =
                    Favorite(
                        type = FavoriteTargetType.REGIONAL_GUIDE,
                        targetId = snapshot.targetId,
                        savedAtMillis = System.currentTimeMillis(),
                    )

                if (favoriteDao.isFavorite(favorite.type.name, favorite.targetId)) {
                    favoriteDao.deleteFavorite(favorite.type.name, favorite.targetId)
                    snapshotDao.deleteSnapshot(snapshot.targetId)
                    false
                } else {
                    favoriteDao.upsertFavorite(favorite.toEntity())
                    snapshotDao.upsertSnapshot(snapshot.toEntity())
                    true
                }
            }

        override suspend fun removeFavorite(targetId: String) {
            database.withTransaction {
                database.favoriteDao().deleteFavorite(
                    type = FavoriteTargetType.REGIONAL_GUIDE.name,
                    targetId = targetId,
                )
                database.regionalGuideFavoriteSnapshotDao().deleteSnapshot(targetId)
            }
        }
    }
