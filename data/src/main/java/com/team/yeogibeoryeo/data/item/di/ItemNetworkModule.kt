package com.team.yeogibeoryeo.data.item.di

import com.team.yeogibeoryeo.data.item.remote.ItemApiService
import com.team.yeogibeoryeo.data.network.di.WasteRecyclingRetrofit
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object ItemNetworkModule {
    @Provides
    @Singleton
    fun provideItemApiService(
        @WasteRecyclingRetrofit retrofit: Retrofit,
    ): ItemApiService = retrofit.create(ItemApiService::class.java)
}
