package com.team.yeogibeoryeo.data.favorite.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY savedAtMillis DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM favorites
            WHERE type = :type AND targetId = :targetId
        )
        """,
    )
    fun observeFavorite(
        type: String,
        targetId: String,
    ): Flow<Boolean>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM favorites
            WHERE type = :type AND targetId = :targetId
        )
        """,
    )
    suspend fun isFavorite(
        type: String,
        targetId: String,
    ): Boolean

    @Upsert
    suspend fun upsertFavorite(entity: FavoriteEntity)

    @Transaction
    suspend fun toggleFavorite(entity: FavoriteEntity): Boolean {
        return if (isFavorite(entity.type, entity.targetId)) {
            deleteFavorite(entity.type, entity.targetId)
            false
        } else {
            upsertFavorite(entity)
            true
        }
    }

    @Query("DELETE FROM favorites WHERE type = :type AND targetId = :targetId")
    suspend fun deleteFavorite(
        type: String,
        targetId: String,
    )
}
