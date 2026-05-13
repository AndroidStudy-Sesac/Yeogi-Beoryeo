package com.team.yeogibeoryeo.data.di

import com.team.yeogibeoryeo.data.common.remote.WasteRecyclingServiceFactory
import com.team.yeogibeoryeo.data.item.remote.ItemApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideItemApiService(json: Json): ItemApiService = WasteRecyclingServiceFactory.createItemApiService(json)
}
