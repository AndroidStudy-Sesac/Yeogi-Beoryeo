package com.team.yeogibeoryeo.domain.operationnotice.usecase

import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNotice
import com.team.yeogibeoryeo.domain.operationnotice.repository.DismissedOperationNoticeRepository
import com.team.yeogibeoryeo.domain.operationnotice.repository.OperationNoticeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class OperationNoticeUseCaseTest {

    @Test
    fun `RefreshOperationNoticesUseCase는 repository refresh를 호출한다`() = runBlocking {
        val repository = FakeOperationNoticeRepository()
        val useCase = RefreshOperationNoticesUseCase(repository)

        useCase()

        assertEquals(1, repository.refreshCount)
    }

    @Test
    fun `DismissOperationNoticeUseCase는 repository에 공지 ID를 전달한다`() = runBlocking {
        val repository = FakeDismissedOperationNoticeRepository()
        val useCase = DismissOperationNoticeUseCase(repository)

        useCase("notice-1")

        assertEquals(listOf("notice-1"), repository.dismissedIds)
    }

    private class FakeOperationNoticeRepository : OperationNoticeRepository {
        var refreshCount = 0

        override fun observeOperationNotices(): Flow<List<OperationNotice>> = flowOf(emptyList())

        override suspend fun refreshOperationNotices() {
            refreshCount += 1
        }
    }

    private class FakeDismissedOperationNoticeRepository : DismissedOperationNoticeRepository {
        val dismissedIds = mutableListOf<String>()

        override fun observeDismissedNoticeIds(): Flow<Set<String>> = flowOf(emptySet())

        override suspend fun dismissNotice(id: String) {
            dismissedIds += id
        }
    }
}
