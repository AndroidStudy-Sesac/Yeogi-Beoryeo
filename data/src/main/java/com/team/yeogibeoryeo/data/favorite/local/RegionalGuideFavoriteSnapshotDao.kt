package com.team.yeogibeoryeo.data.favorite.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RegionalGuideFavoriteSnapshotDao {
    @Query("SELECT * FROM regional_guide_favorite_snapshots")
    fun observeSnapshots(): Flow<List<RegionalGuideFavoriteSnapshotEntity>>

    @Query("SELECT * FROM regional_guide_favorite_snapshots WHERE targetId = :targetId")
    suspend fun getSnapshot(targetId: String): RegionalGuideFavoriteSnapshotEntity?

    @Upsert
    suspend fun upsertSnapshot(entity: RegionalGuideFavoriteSnapshotEntity)

    @Query("DELETE FROM regional_guide_favorite_snapshots WHERE targetId = :targetId")
    suspend fun deleteSnapshot(targetId: String)
}
