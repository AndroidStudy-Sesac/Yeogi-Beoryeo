package com.team.yeogibeoryeo.data.regionalguide.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RegionalGuidePreferencesDataStore

private val Context.regionalGuidePreferencesDataStore by preferencesDataStore(
    name = "regional_guide_preferences",
)

@Module
@InstallIn(SingletonComponent::class)
object RegionalGuidePreferencesDataModule {
    @Provides
    @Singleton
    @RegionalGuidePreferencesDataStore
    fun provideRegionalGuidePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.regionalGuidePreferencesDataStore
}
