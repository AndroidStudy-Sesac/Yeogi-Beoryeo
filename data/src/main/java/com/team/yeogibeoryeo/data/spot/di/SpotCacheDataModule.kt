package com.team.yeogibeoryeo.data.spot.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.team.yeogibeoryeo.data.spot.cache.DataStoreRecentCurrentLocationSpotCacheRepository
import com.team.yeogibeoryeo.domain.spot.repository.RecentCurrentLocationSpotCacheRepository
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
abstract class SpotCacheBindModule {

    @Binds
    @Singleton
    abstract fun bindRecentCurrentLocationSpotCacheRepository(
        dataStoreRecentCurrentLocationSpotCacheRepository: DataStoreRecentCurrentLocationSpotCacheRepository,
    ): RecentCurrentLocationSpotCacheRepository
}

@Module
@InstallIn(SingletonComponent::class)
object SpotCacheProvideModule {

    @Provides
    @Singleton
    fun provideMapCacheDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> {
        return context.mapCacheDataStore
    }
}
