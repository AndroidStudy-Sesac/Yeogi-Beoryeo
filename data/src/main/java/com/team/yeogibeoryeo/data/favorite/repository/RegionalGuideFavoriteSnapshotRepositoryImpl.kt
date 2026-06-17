package com.team.yeogibeoryeo.data.favorite.repository

import com.team.yeogibeoryeo.data.favorite.local.RegionalGuideFavoriteSnapshotDao
import com.team.yeogibeoryeo.data.favorite.mapper.toDomain
import com.team.yeogibeoryeo.data.favorite.mapper.toEntity
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteSnapshotRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RegionalGuideFavoriteSnapshotRepositoryImpl
    @Inject
    constructor(
        private val snapshotDao: RegionalGuideFavoriteSnapshotDao,
    ) : RegionalGuideFavoriteSnapshotRepository {
        override fun observeSnapshots(): Flow<List<RegionalGuideFavoriteSnapshot>> =
            snapshotDao.observeSnapshots()
                .map { entities -> entities.mapNotNull { entity -> entity.toDomain() } }

        override suspend fun getSnapshot(targetId: String): RegionalGuideFavoriteSnapshot? =
            snapshotDao.getSnapshot(targetId)?.toDomain()

        override suspend fun upsertSnapshot(snapshot: RegionalGuideFavoriteSnapshot) {
            snapshotDao.upsertSnapshot(snapshot.toEntity())
        }

        override suspend fun deleteSnapshot(targetId: String) {
            snapshotDao.deleteSnapshot(targetId)
        }
    }
