package com.team.yeogibeoryeo.data.item.di

import com.team.yeogibeoryeo.data.item.local.ItemCategoryLocalDataSource
import com.team.yeogibeoryeo.data.item.local.ItemCategoryLocalSource
import com.team.yeogibeoryeo.data.item.repository.DataStoreHomeQuickCategoryRepository
import com.team.yeogibeoryeo.data.item.repository.DisposalItemGuideRepositoryImpl
import com.team.yeogibeoryeo.domain.item.repository.DisposalItemGuideRepository
import com.team.yeogibeoryeo.domain.item.repository.HomeQuickCategoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ItemBindModule {
    @Binds
    @Singleton
    abstract fun bindItemCategoryLocalSource(
        dataSource: ItemCategoryLocalDataSource,
    ): ItemCategoryLocalSource

    @Binds
    @Singleton
    abstract fun bindDisposalItemGuideRepository(
        repository: DisposalItemGuideRepositoryImpl,
    ): DisposalItemGuideRepository

    @Binds
    @Singleton
    abstract fun bindHomeQuickCategoryRepository(
        repository: DataStoreHomeQuickCategoryRepository,
    ): HomeQuickCategoryRepository
}
