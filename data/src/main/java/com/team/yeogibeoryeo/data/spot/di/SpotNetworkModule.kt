package com.team.yeogibeoryeo.data.spot.di

import com.team.yeogibeoryeo.data.network.di.WasteRecyclingRetrofit
import com.team.yeogibeoryeo.data.spot.remote.SpotApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object SpotNetworkModule {

    @Provides
    @Singleton
    fun provideSpotApiService(
        @WasteRecyclingRetrofit retrofit: Retrofit,
    ): SpotApiService {
        return retrofit.create(SpotApiService::class.java)
    }
}
