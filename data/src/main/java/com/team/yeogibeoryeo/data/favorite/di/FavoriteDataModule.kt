package com.team.yeogibeoryeo.data.favorite.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.team.yeogibeoryeo.data.favorite.local.CollectionSpotFavoriteSnapshotDao
import com.team.yeogibeoryeo.data.favorite.local.FavoriteDao
import com.team.yeogibeoryeo.data.favorite.local.FavoriteDatabase
import com.team.yeogibeoryeo.data.favorite.local.RegionalGuideFavoriteSnapshotDao
import com.team.yeogibeoryeo.data.favorite.repository.CollectionSpotFavoriteSnapshotRepositoryImpl
import com.team.yeogibeoryeo.data.favorite.repository.CollectionSpotFavoriteRepositoryImpl
import com.team.yeogibeoryeo.data.favorite.repository.FavoriteRepositoryImpl
import com.team.yeogibeoryeo.data.favorite.repository.RegionalGuideFavoriteRepositoryImpl
import com.team.yeogibeoryeo.data.favorite.repository.RegionalGuideFavoriteSnapshotRepositoryImpl
import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteSnapshotRepository
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteSnapshotRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FavoriteDatabaseModule {
    @Provides
    @Singleton
    fun provideFavoriteDatabase(
        @ApplicationContext context: Context,
    ): FavoriteDatabase =
        Room.databaseBuilder(
            context,
            FavoriteDatabase::class.java,
            "yeogi_beoryeo_favorites.db",
        )
            .addMigrations(FAVORITE_DATABASE_MIGRATION_1_2)
            .addMigrations(FAVORITE_DATABASE_MIGRATION_2_3)
            .build()

    @Provides
    @Singleton
    fun provideFavoriteDao(database: FavoriteDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    @Singleton
    fun provideCollectionSpotFavoriteSnapshotDao(
        database: FavoriteDatabase,
    ): CollectionSpotFavoriteSnapshotDao = database.collectionSpotFavoriteSnapshotDao()

    @Provides
    @Singleton
    fun provideRegionalGuideFavoriteSnapshotDao(
        database: FavoriteDatabase,
    ): RegionalGuideFavoriteSnapshotDao = database.regionalGuideFavoriteSnapshotDao()

    internal val FAVORITE_DATABASE_MIGRATION_1_2 =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS collection_spot_favorite_snapshots (
                        targetId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        spotType TEXT NOT NULL,
                        address TEXT NOT NULL,
                        detailLocation TEXT,
                        latitude REAL,
                        longitude REAL,
                        PRIMARY KEY(targetId)
                    )
                    """.trimIndent(),
                )
            }
        }

    internal val FAVORITE_DATABASE_MIGRATION_2_3 =
        object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS regional_guide_favorite_snapshots (
                        targetId TEXT NOT NULL,
                        sido TEXT,
                        sigungu TEXT,
                        eupmyeondong TEXT,
                        targetRegionName TEXT,
                        managementZoneName TEXT,
                        PRIMARY KEY(targetId)
                    )
                    """.trimIndent(),
                )
            }
        }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FavoriteBindModule {
    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(repository: FavoriteRepositoryImpl): FavoriteRepository

    @Binds
    @Singleton
    abstract fun bindCollectionSpotFavoriteSnapshotRepository(
        repository: CollectionSpotFavoriteSnapshotRepositoryImpl,
    ): CollectionSpotFavoriteSnapshotRepository

    @Binds
    @Singleton
    abstract fun bindCollectionSpotFavoriteRepository(
        repository: CollectionSpotFavoriteRepositoryImpl,
    ): CollectionSpotFavoriteRepository

    @Binds
    @Singleton
    abstract fun bindRegionalGuideFavoriteSnapshotRepository(
        repository: RegionalGuideFavoriteSnapshotRepositoryImpl,
    ): RegionalGuideFavoriteSnapshotRepository

    @Binds
    @Singleton
    abstract fun bindRegionalGuideFavoriteRepository(
        repository: RegionalGuideFavoriteRepositoryImpl,
    ): RegionalGuideFavoriteRepository
}
