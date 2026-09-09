package com.team.yeogibeoryeo.data.notice.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.firestore.FirebaseFirestore
import com.team.yeogibeoryeo.data.notice.remote.FirestoreNoticeRemoteDataSource
import com.team.yeogibeoryeo.data.notice.remote.NoticeRemoteDataSource
import com.team.yeogibeoryeo.data.notice.repository.NoticeRepositoryImpl
import com.team.yeogibeoryeo.domain.notice.repository.NoticeRepository
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
annotation class NoticePreferencesDataStore

private val Context.noticePreferencesDataStore by preferencesDataStore(
    name = "notice_preferences",
)

@Module
@InstallIn(SingletonComponent::class)
abstract class NoticeBindModule {
    @Binds
    @Singleton
    abstract fun bindNoticeRemoteDataSource(
        impl: FirestoreNoticeRemoteDataSource,
    ): NoticeRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindNoticeRepository(
        impl: NoticeRepositoryImpl,
    ): NoticeRepository
}

@Module
@InstallIn(SingletonComponent::class)
object NoticeProvideModule {
    @Provides
    @Singleton
    @NoticePreferencesDataStore
    fun provideNoticePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.noticePreferencesDataStore

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
}
