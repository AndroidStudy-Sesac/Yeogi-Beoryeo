package com.team.yeogibeoryeo.presentation.map.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.team.yeogibeoryeo.presentation.map.cache.DataStoreRecentCurrentLocationSpotCache
import com.team.yeogibeoryeo.presentation.map.cache.RecentCurrentLocationSpotCache
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.mapCacheDataStore by preferencesDataStore(
    name = "map_cache_preferences",
)

@Module
@InstallIn(SingletonComponent::class)
abstract class MapCacheBindModule {

    @Binds
    @Singleton
    abstract fun bindRecentCurrentLocationSpotCache(
        dataStoreRecentCurrentLocationSpotCache: DataStoreRecentCurrentLocationSpotCache,
    ): RecentCurrentLocationSpotCache
}

@Module
@InstallIn(SingletonComponent::class)
object MapCacheProvideModule {

    @Provides
    @Singleton
    fun provideMapCacheDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> {
        return context.mapCacheDataStore
    }
}
