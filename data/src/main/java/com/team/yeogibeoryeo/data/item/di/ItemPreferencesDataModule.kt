package com.team.yeogibeoryeo.data.item.di

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
annotation class ItemPreferencesDataStore

private val Context.itemPreferencesDataStore by preferencesDataStore(
    name = "item_preferences",
)

@Module
@InstallIn(SingletonComponent::class)
object ItemPreferencesDataModule {

    @Provides
    @Singleton
    @ItemPreferencesDataStore
    fun provideItemPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.itemPreferencesDataStore
}
