package com.team.yeogibeoryeo.data.favorite.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionSpotFavoriteSnapshotDao {
    @Query("SELECT * FROM collection_spot_favorite_snapshots")
    fun observeSnapshots(): Flow<List<CollectionSpotFavoriteSnapshotEntity>>

    @Query("SELECT * FROM collection_spot_favorite_snapshots WHERE targetId = :targetId")
    suspend fun getSnapshot(targetId: String): CollectionSpotFavoriteSnapshotEntity?

    @Upsert
    suspend fun upsertSnapshot(entity: CollectionSpotFavoriteSnapshotEntity)

    @Query("DELETE FROM collection_spot_favorite_snapshots WHERE targetId = :targetId")
    suspend fun deleteSnapshot(targetId: String)
}
