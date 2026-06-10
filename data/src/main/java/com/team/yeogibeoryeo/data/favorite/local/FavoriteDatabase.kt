package com.team.yeogibeoryeo.data.favorite.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        CollectionSpotFavoriteSnapshotEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class FavoriteDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    abstract fun collectionSpotFavoriteSnapshotDao(): CollectionSpotFavoriteSnapshotDao
}
