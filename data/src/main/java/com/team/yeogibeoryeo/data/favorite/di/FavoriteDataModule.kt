package com.team.yeogibeoryeo.data.favorite.di

import android.content.Context
import androidx.room.Room
import com.team.yeogibeoryeo.data.favorite.local.FavoriteDao
import com.team.yeogibeoryeo.data.favorite.local.FavoriteDatabase
import com.team.yeogibeoryeo.data.favorite.repository.FavoriteRepositoryImpl
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
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
        ).build()

    @Provides
    @Singleton
    fun provideFavoriteDao(database: FavoriteDatabase): FavoriteDao = database.favoriteDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FavoriteBindModule {
    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(repository: FavoriteRepositoryImpl): FavoriteRepository
}
