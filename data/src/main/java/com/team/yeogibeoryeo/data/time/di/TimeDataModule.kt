package com.team.yeogibeoryeo.data.time.di

import com.team.yeogibeoryeo.data.time.SystemTimeProvider
import com.team.yeogibeoryeo.domain.time.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TimeDataModule {

    @Binds
    @Singleton
    abstract fun bindTimeProvider(
        systemTimeProvider: SystemTimeProvider,
    ): TimeProvider
}
