package com.team.yeogibeoryeo.data.item.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.team.yeogibeoryeo.data.item.remote.ItemApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object ItemNetworkModule {

    private const val BASE_URL = "https://apis.data.go.kr/1482000/WasteRecyclingService/"

    @Provides
    @Singleton
    @WasteRecyclingRetrofit
    fun provideWasteRecyclingRetrofit(
        json: Json,
        okHttpClient: OkHttpClient,
    ): Retrofit {
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideItemApiService(
        @WasteRecyclingRetrofit retrofit: Retrofit,
    ): ItemApiService = retrofit.create(ItemApiService::class.java)
}
