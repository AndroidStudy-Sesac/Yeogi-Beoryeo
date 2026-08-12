package com.team.yeogibeoryeo.core.di

import com.team.yeogibeoryeo.core.app.BuildConfigAppVersionProvider
import com.team.yeogibeoryeo.core.key.BuildConfigAppKeyProvider
import com.team.yeogibeoryeo.data.core.key.AppKeyProvider
import com.team.yeogibeoryeo.domain.app.AppVersionProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class KeyProviderModule {

    @Binds
    @Singleton
    abstract fun bindAppKeyProvider(
        buildConfigAppKeyProvider: BuildConfigAppKeyProvider,
    ): AppKeyProvider

    @Binds
    @Singleton
    abstract fun bindAppVersionProvider(
        buildConfigAppVersionProvider: BuildConfigAppVersionProvider,
    ): AppVersionProvider
}
