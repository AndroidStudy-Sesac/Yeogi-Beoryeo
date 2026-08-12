package com.team.yeogibeoryeo.data.operationnotice.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.team.yeogibeoryeo.data.operationnotice.repository.DataStoreDismissedOperationNoticeRepository
import com.team.yeogibeoryeo.data.operationnotice.repository.FirebaseRemoteConfigOperationNoticeRepository
import com.team.yeogibeoryeo.domain.operationnotice.repository.DismissedOperationNoticeRepository
import com.team.yeogibeoryeo.domain.operationnotice.repository.OperationNoticeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OperationNoticePreferencesDataStore

private val Context.operationNoticePreferencesDataStore by preferencesDataStore(
    name = "operation_notice_preferences",
)

@Module
@InstallIn(SingletonComponent::class)
object OperationNoticePreferencesDataModule {

    @Provides
    @Singleton
    @OperationNoticePreferencesDataStore
    fun provideOperationNoticePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.operationNoticePreferencesDataStore
}

@Module
@InstallIn(SingletonComponent::class)
abstract class OperationNoticeDataModule {

    @Binds
    @Singleton
    abstract fun bindOperationNoticeRepository(
        repository: FirebaseRemoteConfigOperationNoticeRepository,
    ): OperationNoticeRepository

    @Binds
    @Singleton
    abstract fun bindDismissedOperationNoticeRepository(
        repository: DataStoreDismissedOperationNoticeRepository,
    ): DismissedOperationNoticeRepository
}

