package com.team.yeogibeoryeo.data.notice.di

import com.google.firebase.firestore.FirebaseFirestore
import com.team.yeogibeoryeo.data.notice.remote.FirestoreNoticeRemoteDataSource
import com.team.yeogibeoryeo.data.notice.remote.NoticeRemoteDataSource
import com.team.yeogibeoryeo.data.notice.repository.NoticeRepositoryImpl
import com.team.yeogibeoryeo.domain.notice.repository.NoticeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
}
