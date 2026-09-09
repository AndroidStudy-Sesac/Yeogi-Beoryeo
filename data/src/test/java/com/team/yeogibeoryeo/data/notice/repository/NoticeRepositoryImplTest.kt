package com.team.yeogibeoryeo.data.notice.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.team.yeogibeoryeo.data.notice.remote.NoticeRemoteDataSource
import com.team.yeogibeoryeo.data.notice.remote.dto.NoticeDto
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NoticeRepositoryImplTest {
    private val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preferencesFile = File.createTempFile("read-notices", ".preferences_pb").apply {
        delete()
    }
    private val dataStore = PreferenceDataStoreFactory.create(
        scope = dataStoreScope,
        produceFile = { preferencesFile },
    )

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        preferencesFile.delete()
    }

    @Test
    fun `공개 공지를 최신 게시 순서로 변환한다`() = runBlocking {
        val older = noticeDto(id = "older", publishedAtMillis = 1_000)
        val newer = noticeDto(id = "newer", publishedAtMillis = 2_000)
        val dataSource = FakeNoticeRemoteDataSource(listOf(older, newer))
        val repository = NoticeRepositoryImpl(dataSource, dataStore)

        val notices = repository.getPublishedNotices()

        assertEquals(listOf("newer", "older"), notices.map { it.id })
        assertEquals(1, dataSource.fetchCallCount)
    }

    @Test
    fun `필수 내용이 없는 공지를 목록에서 제외한다`() = runBlocking {
        val dataSource = FakeNoticeRemoteDataSource(
            listOf(
                noticeDto(id = "valid"),
                noticeDto(id = "invalid").copy(title = null),
            ),
        )
        val repository = NoticeRepositoryImpl(dataSource, dataStore)

        val notices = repository.getPublishedNotices()

        assertEquals(listOf("valid"), notices.map { it.id })
    }

    @Test
    fun `재조회하면 원격 데이터 소스를 다시 호출한다`() = runBlocking {
        val dataSource = FakeNoticeRemoteDataSource(listOf(noticeDto()))
        val repository = NoticeRepositoryImpl(dataSource, dataStore)

        repository.getPublishedNotices()
        repository.getPublishedNotices()

        assertEquals(2, dataSource.fetchCallCount)
    }

    @Test
    fun `원격 조회 예외를 호출자에게 전달한다`() {
        val failure = IllegalStateException("Firestore 조회 실패")
        val repository = NoticeRepositoryImpl(
            remoteDataSource = ThrowingNoticeRemoteDataSource(failure),
            dataStore = dataStore,
        )

        val thrown = assertThrows(IllegalStateException::class.java) {
            runBlocking { repository.getPublishedNotices() }
        }

        assertEquals(failure, thrown)
    }

    @Test
    fun `읽은 공지 ID를 정규화해 누적 저장한다`() = runBlocking {
        val repository = NoticeRepositoryImpl(
            remoteDataSource = FakeNoticeRemoteDataSource(emptyList()),
            dataStore = dataStore,
        )

        repository.markNoticeRead(" notice-1 ")
        repository.markNoticeRead("notice-2")
        repository.markNoticeRead("notice-1")
        repository.markNoticeRead("   ")

        assertEquals(setOf("notice-1", "notice-2"), repository.getReadNoticeIds())
    }

    @Test
    fun `DataStore를 다시 생성해도 읽은 공지 ID를 유지한다`() = runBlocking {
        val file = File.createTempFile("persisted-read-notices", ".preferences_pb").apply {
            delete()
        }
        val firstJob = SupervisorJob()
        val firstDataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(firstJob + Dispatchers.IO),
            produceFile = { file },
        )

        try {
            NoticeRepositoryImpl(
                remoteDataSource = FakeNoticeRemoteDataSource(emptyList()),
                dataStore = firstDataStore,
            ).markNoticeRead("notice-1")
            firstJob.cancelAndJoin()

            val secondJob = SupervisorJob()
            val secondDataStore = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(secondJob + Dispatchers.IO),
                produceFile = { file },
            )
            try {
                val restoredIds = NoticeRepositoryImpl(
                    remoteDataSource = FakeNoticeRemoteDataSource(emptyList()),
                    dataStore = secondDataStore,
                ).getReadNoticeIds()

                assertEquals(setOf("notice-1"), restoredIds)
            } finally {
                secondJob.cancelAndJoin()
            }
        } finally {
            firstJob.cancelAndJoin()
            file.delete()
        }
    }

    @Test
    fun `읽음 상태 조회 실패는 빈 set으로 대체한다`() = runBlocking {
        val repository = NoticeRepositoryImpl(
            remoteDataSource = FakeNoticeRemoteDataSource(emptyList()),
            dataStore = ThrowingDataStore(IllegalStateException("읽기 실패")),
        )

        assertEquals(emptySet<String>(), repository.getReadNoticeIds())
    }

    @Test
    fun `읽음 상태 저장 실패는 상세 진입을 막지 않는다`() = runBlocking {
        val repository = NoticeRepositoryImpl(
            remoteDataSource = FakeNoticeRemoteDataSource(emptyList()),
            dataStore = ThrowingDataStore(IllegalStateException("저장 실패")),
        )

        repository.markNoticeRead("notice-1")
    }

    @Test
    fun `읽음 상태 조회 취소는 호출자에게 전달한다`() {
        val cancellation = CancellationException("취소")
        val repository = NoticeRepositoryImpl(
            remoteDataSource = FakeNoticeRemoteDataSource(emptyList()),
            dataStore = ThrowingDataStore(cancellation),
        )

        val thrown = assertThrows(CancellationException::class.java) {
            runBlocking { repository.getReadNoticeIds() }
        }

        assertEquals(cancellation, thrown)
    }

    @Test
    fun `읽음 상태 저장 취소는 호출자에게 전달한다`() {
        val cancellation = CancellationException("취소")
        val repository = NoticeRepositoryImpl(
            remoteDataSource = FakeNoticeRemoteDataSource(emptyList()),
            dataStore = ThrowingDataStore(cancellation),
        )

        val thrown = assertThrows(CancellationException::class.java) {
            runBlocking { repository.markNoticeRead("notice-1") }
        }

        assertEquals(cancellation, thrown)
    }

    private fun noticeDto(
        id: String = "service-update",
        publishedAtMillis: Long = 1_754_000_000_000,
    ): NoticeDto {
        return NoticeDto(
            id = id,
            title = "서비스 업데이트 안내",
            body = "새 기능을 안내합니다.",
            publishedAtMillis = publishedAtMillis,
            updatedAtMillis = null,
        )
    }

    private class FakeNoticeRemoteDataSource(
        private val notices: List<NoticeDto>,
    ) : NoticeRemoteDataSource {
        var fetchCallCount = 0
            private set

        override suspend fun fetchPublishedNotices(): List<NoticeDto> {
            fetchCallCount += 1
            return notices
        }
    }

    private class ThrowingNoticeRemoteDataSource(
        private val throwable: Throwable,
    ) : NoticeRemoteDataSource {
        override suspend fun fetchPublishedNotices(): List<NoticeDto> {
            throw throwable
        }
    }

    private class ThrowingDataStore(
        private val throwable: Throwable,
    ) : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw throwable }

        override suspend fun updateData(
            transform: suspend (Preferences) -> Preferences,
        ): Preferences {
            throw throwable
        }
    }
}
