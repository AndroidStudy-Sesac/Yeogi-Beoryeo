package com.team.yeogibeoryeo.data.appguide.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.team.yeogibeoryeo.data.appguide.repository.DataStoreAppGuideRepository
import com.team.yeogibeoryeo.domain.appguide.repository.AppGuideRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppGuidePreferencesDataStore

private val Context.appGuidePreferencesDataStore by preferencesDataStore(
    name = "app_guide_preferences",
)

@Module
@InstallIn(SingletonComponent::class)
abstract class AppGuideBindModule {
    @Binds
    @Singleton
    abstract fun bindAppGuideRepository(
        repository: DataStoreAppGuideRepository,
    ): AppGuideRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppGuideProvideModule {
    @Provides
    @Singleton
    @AppGuidePreferencesDataStore
    fun provideAppGuidePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.appGuidePreferencesDataStore
}
