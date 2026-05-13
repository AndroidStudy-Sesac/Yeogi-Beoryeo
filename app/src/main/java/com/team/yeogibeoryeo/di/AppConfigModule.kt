package com.team.yeogibeoryeo.di

import com.team.yeogibeoryeo.BuildConfig
import com.team.yeogibeoryeo.data.common.di.PublicDataServiceKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {
    @Provides
    @PublicDataServiceKey
    fun providePublicDataServiceKey(): String = BuildConfig.PUBLIC_DATA_SERVICE_KEY
}
