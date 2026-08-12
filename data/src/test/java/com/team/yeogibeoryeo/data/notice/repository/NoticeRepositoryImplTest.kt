package com.team.yeogibeoryeo.data.notice.repository

import com.team.yeogibeoryeo.data.notice.remote.NoticeRemoteDataSource
import com.team.yeogibeoryeo.data.notice.remote.dto.NoticeDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NoticeRepositoryImplTest {
    @Test
    fun `공개 공지를 최신 게시 순서로 변환한다`() = runBlocking {
        val older = noticeDto(id = "older", publishedAtMillis = 1_000)
        val newer = noticeDto(id = "newer", publishedAtMillis = 2_000)
        val dataSource = FakeNoticeRemoteDataSource(listOf(older, newer))
        val repository = NoticeRepositoryImpl(dataSource)

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
        val repository = NoticeRepositoryImpl(dataSource)

        val notices = repository.getPublishedNotices()

        assertEquals(listOf("valid"), notices.map { it.id })
    }

    @Test
    fun `재조회하면 원격 데이터 소스를 다시 호출한다`() = runBlocking {
        val dataSource = FakeNoticeRemoteDataSource(listOf(noticeDto()))
        val repository = NoticeRepositoryImpl(dataSource)

        repository.getPublishedNotices()
        repository.getPublishedNotices()

        assertEquals(2, dataSource.fetchCallCount)
    }

    @Test
    fun `원격 조회 예외를 호출자에게 전달한다`() {
        val failure = IllegalStateException("Firestore 조회 실패")
        val repository = NoticeRepositoryImpl(
            remoteDataSource = ThrowingNoticeRemoteDataSource(failure),
        )

        val thrown = assertThrows(IllegalStateException::class.java) {
            runBlocking { repository.getPublishedNotices() }
        }

        assertEquals(failure, thrown)
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
}
