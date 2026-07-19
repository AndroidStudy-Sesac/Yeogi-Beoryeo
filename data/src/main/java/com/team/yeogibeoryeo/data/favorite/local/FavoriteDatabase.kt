package com.team.yeogibeoryeo.data.favorite.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        CollectionSpotFavoriteSnapshotEntity::class,
        RegionalGuideFavoriteSnapshotEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class FavoriteDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    abstract fun collectionSpotFavoriteSnapshotDao(): CollectionSpotFavoriteSnapshotDao

    abstract fun regionalGuideFavoriteSnapshotDao(): RegionalGuideFavoriteSnapshotDao
}
