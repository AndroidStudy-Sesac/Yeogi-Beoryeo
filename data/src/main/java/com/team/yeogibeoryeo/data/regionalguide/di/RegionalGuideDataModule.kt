package com.team.yeogibeoryeo.data.regionalguide.di

import com.team.yeogibeoryeo.data.regionalguide.remote.RegionalGuideDataSource
import com.team.yeogibeoryeo.data.regionalguide.remote.RegionalGuideRemoteDataSource
import com.team.yeogibeoryeo.data.regionalguide.repository.RegionalDisposalGuideRepositoryImpl
import com.team.yeogibeoryeo.domain.regionalguide.repository.RegionalDisposalGuideRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RegionalGuideDataModule {

    @Binds
    @Singleton
    abstract fun bindRegionalGuideDataSource(
        impl: RegionalGuideRemoteDataSource
    ): RegionalGuideDataSource

    @Binds
    @Singleton
    abstract fun bindRegionalDisposalGuideRepository(
        impl: RegionalDisposalGuideRepositoryImpl
    ): RegionalDisposalGuideRepository
}