package com.team.yeogibeoryeo.data.di

import com.team.yeogibeoryeo.data.item.remote.ItemApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val WASTE_RECYCLING_BASE_URL = "https://apis.data.go.kr/1482000/WasteRecyclingService/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideWasteRecyclingRetrofit(json: Json): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(WASTE_RECYCLING_BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideItemApiService(retrofit: Retrofit): ItemApiService = retrofit.create(ItemApiService::class.java)
}
