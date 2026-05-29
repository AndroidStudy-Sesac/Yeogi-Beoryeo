package com.team.yeogibeoryeo.data.region.di

import com.team.yeogibeoryeo.data.region.RegionOptionsRepositoryImpl
import com.team.yeogibeoryeo.data.region.RegionRepositoryImpl
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import com.team.yeogibeoryeo.domain.region.repository.RegionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RegionDataModule {

    @Binds
    @Singleton
    abstract fun bindRegionRepository(
        impl: RegionRepositoryImpl
    ): RegionRepository

    @Binds
    @Singleton
    abstract fun bindRegionOptionsRepository(
        impl: RegionOptionsRepositoryImpl
    ): RegionOptionsRepository
}