package com.team.yeogibeoryeo.data.regionalguide.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RegionalGuideFetchScope

@Module
@InstallIn(SingletonComponent::class)
object RegionalGuideCoroutineModule {

    @Provides
    @Singleton
    @RegionalGuideFetchScope
    fun provideRegionalGuideFetchScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
