package com.team.yeogibeoryeo.data.favorite.repository

import com.team.yeogibeoryeo.data.favorite.local.CollectionSpotFavoriteSnapshotDao
import com.team.yeogibeoryeo.data.favorite.mapper.toDomain
import com.team.yeogibeoryeo.data.favorite.mapper.toEntity
import com.team.yeogibeoryeo.domain.favorite.model.CollectionSpotFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteSnapshotRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CollectionSpotFavoriteSnapshotRepositoryImpl
    @Inject
    constructor(
        private val snapshotDao: CollectionSpotFavoriteSnapshotDao,
    ) : CollectionSpotFavoriteSnapshotRepository {
        override fun observeSnapshots(): Flow<List<CollectionSpotFavoriteSnapshot>> =
            snapshotDao.observeSnapshots()
                .map { entities -> entities.mapNotNull { entity -> entity.toDomain() } }

        override suspend fun getSnapshot(targetId: String): CollectionSpotFavoriteSnapshot? =
            snapshotDao.getSnapshot(targetId)?.toDomain()

        override suspend fun upsertSnapshot(snapshot: CollectionSpotFavoriteSnapshot) {
            snapshotDao.upsertSnapshot(snapshot.toEntity())
        }

        override suspend fun deleteSnapshot(targetId: String) {
            snapshotDao.deleteSnapshot(targetId)
        }
    }
