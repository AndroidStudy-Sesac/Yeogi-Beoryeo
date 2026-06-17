package com.team.yeogibeoryeo.domain.favorite.repository

import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import kotlinx.coroutines.flow.Flow

interface RegionalGuideFavoriteSnapshotRepository {
    fun observeSnapshots(): Flow<List<RegionalGuideFavoriteSnapshot>>

    suspend fun getSnapshot(targetId: String): RegionalGuideFavoriteSnapshot?

    suspend fun upsertSnapshot(snapshot: RegionalGuideFavoriteSnapshot)

    suspend fun deleteSnapshot(targetId: String)
}
