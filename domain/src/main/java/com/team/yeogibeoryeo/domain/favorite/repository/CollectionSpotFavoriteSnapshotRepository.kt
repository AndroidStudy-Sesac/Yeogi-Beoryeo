package com.team.yeogibeoryeo.domain.favorite.repository

import com.team.yeogibeoryeo.domain.favorite.model.CollectionSpotFavoriteSnapshot
import kotlinx.coroutines.flow.Flow

interface CollectionSpotFavoriteSnapshotRepository {
    fun observeSnapshots(): Flow<List<CollectionSpotFavoriteSnapshot>>

    suspend fun getSnapshot(targetId: String): CollectionSpotFavoriteSnapshot?

    suspend fun upsertSnapshot(snapshot: CollectionSpotFavoriteSnapshot)

    suspend fun deleteSnapshot(targetId: String)
}
