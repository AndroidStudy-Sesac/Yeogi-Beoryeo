package com.team.yeogibeoryeo.data.regionalguide.di

import com.team.yeogibeoryeo.data.network.di.HouseholdWasteInfoRetrofit
import com.team.yeogibeoryeo.data.regionalguide.remote.RegionalGuideApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RegionalGuideNetworkModule {

    @Provides
    @Singleton
    fun provideRegionalGuideApiService(
        @HouseholdWasteInfoRetrofit retrofit: Retrofit,
    ): RegionalGuideApiService {
        return retrofit.create(RegionalGuideApiService::class.java)
    }
}
