package com.team.yeogibeoryeo.data.network.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

internal const val HOUSEHOLD_WASTE_INFO_API_BASE_URL =
    "https://apis.data.go.kr/1741000/household_waste_info/"

@Module
@InstallIn(SingletonComponent::class)
object HouseholdWasteInfoNetworkModule {

    @Provides
    @Singleton
    @HouseholdWasteInfoRetrofit
    fun provideHouseholdWasteInfoRetrofit(
        json: Json,
        okHttpClient: OkHttpClient,
    ): Retrofit {
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(HOUSEHOLD_WASTE_INFO_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
}
