package com.team.yeogibeoryeo.core.di

import com.team.yeogibeoryeo.core.key.BuildConfigPublicDataKeyProvider
import com.team.yeogibeoryeo.data.core.key.PublicDataKeyProvider
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
    abstract fun bindPublicDataKeyProvider(
        buildConfigPublicDataKeyProvider: BuildConfigPublicDataKeyProvider,
    ): PublicDataKeyProvider
}