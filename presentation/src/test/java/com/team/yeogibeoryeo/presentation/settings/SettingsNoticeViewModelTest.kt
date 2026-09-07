package com.team.yeogibeoryeo.presentation.settings

import androidx.lifecycle.SavedStateHandle
import com.team.yeogibeoryeo.domain.notice.model.Notice
import com.team.yeogibeoryeo.domain.notice.repository.NoticeRepository
import com.team.yeogibeoryeo.domain.notice.usecase.GetPublishedNoticesUseCase
import com.team.yeogibeoryeo.domain.notice.usecase.GetReadNoticeIdsUseCase
import com.team.yeogibeoryeo.domain.notice.usecase.MarkNoticeReadUseCase
import com.team.yeogibeoryeo.presentation.search.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsNoticeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `최초 진입 시 공개 공지를 한 번 조회한다`() = runTest {
        val repository = FakeNoticeRepository(mutableListOf({ listOf(notice()) }))

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        assertEquals(1, repository.fetchCallCount)
        assertEquals(
            listOf("notice-1"),
            (viewModel.uiState.value as SettingsNoticeUiState.Content).notices.map { it.id },
        )
    }

    @Test
    fun `공지가 없으면 빈 목록 상태를 표시한다`() = runTest {
        val viewModel = createViewModel(
            FakeNoticeRepository(mutableListOf({ emptyList<Notice>() })),
        )
        advanceUntilIdle()

        assertEquals(
            emptyList<Notice>(),
            (viewModel.uiState.value as SettingsNoticeUiState.Content).notices,
        )
    }

    @Test
    fun `저장되지 않은 공지는 읽지 않은 상태로 표시한다`() = runTest {
        val viewModel = createViewModel(
            FakeNoticeRepository(mutableListOf({ listOf(notice()) })),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as SettingsNoticeUiState.Content

        assertEquals(true, state.hasUnreadNotices)
        assertEquals(true, state.isUnread("notice-1"))
    }

    @Test
    fun `저장된 공지는 읽은 상태로 표시한다`() = runTest {
        val viewModel = createViewModel(
            FakeNoticeRepository(
                responses = mutableListOf({ listOf(notice()) }),
                initialReadNoticeIds = setOf("notice-1"),
            ),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as SettingsNoticeUiState.Content

        assertEquals(false, state.hasUnreadNotices)
        assertEquals(false, state.isUnread("notice-1"))
    }

    @Test
    fun `조회 실패 후 다시 시도하면 공지를 표시한다`() = runTest {
        val repository = FakeNoticeRepository(
            mutableListOf(
                { throw IllegalStateException("조회 실패") },
                { listOf(notice()) },
            ),
        )
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        assertSame(SettingsNoticeUiState.LoadFailed, viewModel.uiState.value)

        viewModel.retryLoad()
        advanceUntilIdle()

        assertEquals(2, repository.fetchCallCount)
        assertEquals(
            listOf("notice-1"),
            (viewModel.uiState.value as SettingsNoticeUiState.Content).notices.map { it.id },
        )
    }

    @Test
    fun `조회 중 다시 시도 요청은 중복 조회를 만들지 않는다`() = runTest {
        val completion = CompletableDeferred<List<Notice>>()
        val repository = FakeNoticeRepository(
            mutableListOf({ completion.await() }),
        )
        val viewModel = createViewModel(repository)
        assertSame(SettingsNoticeUiState.Loading, viewModel.uiState.value)

        viewModel.retryLoad()

        assertEquals(1, repository.fetchCallCount)
        completion.complete(listOf(notice()))
        advanceUntilIdle()
        assertEquals(1, repository.fetchCallCount)
    }

    @Test
    fun `공지를 선택하고 해제하면 선택 ID도 함께 저장하고 삭제한다`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val repository = FakeNoticeRepository(mutableListOf({ listOf(notice()) }))
        val viewModel = createViewModel(
            repository = repository,
            savedStateHandle = savedStateHandle,
        )
        advanceUntilIdle()

        viewModel.selectNotice("notice-1")

        assertEquals(
            "notice-1",
            (viewModel.uiState.value as SettingsNoticeUiState.Content).selectedNotice?.id,
        )
        assertEquals("notice-1", savedStateHandle.get<String>("selectedNoticeId"))
        assertEquals(
            false,
            (viewModel.uiState.value as SettingsNoticeUiState.Content).hasUnreadNotices,
        )
        advanceUntilIdle()
        assertEquals(listOf("notice-1"), repository.markedNoticeIds)

        viewModel.clearNoticeSelection()

        assertNull((viewModel.uiState.value as SettingsNoticeUiState.Content).selectedNotice)
        assertNull(savedStateHandle.get<String>("selectedNoticeId"))
    }

    @Test
    fun `로딩 중 선택과 해제 요청은 저장 상태를 바꾸지 않는다`() = runTest {
        val completion = CompletableDeferred<List<Notice>>()
        val savedStateHandle = SavedStateHandle(mapOf("selectedNoticeId" to "notice-1"))
        val viewModel = createViewModel(
            repository = FakeNoticeRepository(mutableListOf({ completion.await() })),
            savedStateHandle = savedStateHandle,
        )

        viewModel.selectNotice("notice-1")
        viewModel.clearNoticeSelection()

        assertSame(SettingsNoticeUiState.Loading, viewModel.uiState.value)
        assertEquals("notice-1", savedStateHandle.get<String>("selectedNoticeId"))
        completion.complete(listOf(notice()))
        advanceUntilIdle()
    }

    @Test
    fun `목록에 없는 공지 선택 요청은 기존 선택을 유지한다`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(
            repository = FakeNoticeRepository(mutableListOf({ listOf(notice()) })),
            savedStateHandle = savedStateHandle,
        )
        advanceUntilIdle()
        viewModel.selectNotice("notice-1")

        viewModel.selectNotice("missing")

        assertEquals(
            "notice-1",
            (viewModel.uiState.value as SettingsNoticeUiState.Content).selectedNotice?.id,
        )
        assertEquals("notice-1", savedStateHandle.get<String>("selectedNoticeId"))
    }

    @Test
    fun `공지 일부만 읽으면 나머지 공지를 읽지 않은 상태로 유지한다`() = runTest {
        val secondNotice = notice().copy(id = "notice-2")
        val viewModel = createViewModel(
            FakeNoticeRepository(mutableListOf({ listOf(notice(), secondNotice) })),
        )
        advanceUntilIdle()

        viewModel.selectNotice("notice-1")

        val state = viewModel.uiState.value as SettingsNoticeUiState.Content
        assertEquals(true, state.hasUnreadNotices)
        assertEquals(false, state.isUnread("notice-1"))
        assertEquals(true, state.isUnread("notice-2"))
    }

    @Test
    fun `읽은 공지 뒤 새 ID 공지를 조회하면 다시 읽지 않은 상태가 된다`() = runTest {
        val newNotice = notice().copy(id = "notice-2")
        val repository = FakeNoticeRepository(
            responses = mutableListOf(
                { listOf(notice()) },
                { listOf(newNotice, notice()) },
            ),
            initialReadNoticeIds = setOf("notice-1"),
        )
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        assertEquals(
            false,
            (viewModel.uiState.value as SettingsNoticeUiState.Content).hasUnreadNotices,
        )

        viewModel.refreshNotices()
        advanceUntilIdle()

        val state = viewModel.uiState.value as SettingsNoticeUiState.Content
        assertEquals(true, state.hasUnreadNotices)
        assertEquals(true, state.isUnread("notice-2"))
    }

    @Test
    fun `저장된 공지가 조회 목록에 있으면 본문 선택을 복원한다`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("selectedNoticeId" to "notice-1"))

        val viewModel = createViewModel(
            repository = FakeNoticeRepository(mutableListOf({ listOf(notice()) })),
            savedStateHandle = savedStateHandle,
        )
        advanceUntilIdle()

        assertEquals(
            "notice-1",
            (viewModel.uiState.value as SettingsNoticeUiState.Content).selectedNotice?.id,
        )
    }

    @Test
    fun `저장된 공지가 조회 목록에 없으면 목록으로 복귀한다`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("selectedNoticeId" to "removed"))

        val viewModel = createViewModel(
            repository = FakeNoticeRepository(mutableListOf({ listOf(notice()) })),
            savedStateHandle = savedStateHandle,
        )
        advanceUntilIdle()

        assertNull((viewModel.uiState.value as SettingsNoticeUiState.Content).selectedNotice)
        assertNull(savedStateHandle.get<String>("selectedNoticeId"))
    }

    private fun createViewModel(
        repository: NoticeRepository,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): SettingsNoticeViewModel {
        return SettingsNoticeViewModel(
            getPublishedNoticesUseCase = GetPublishedNoticesUseCase(repository),
            getReadNoticeIdsUseCase = GetReadNoticeIdsUseCase(repository),
            markNoticeReadUseCase = MarkNoticeReadUseCase(repository),
            savedStateHandle = savedStateHandle,
        )
    }

    private fun notice(): Notice {
        return Notice(
            id = "notice-1",
            title = "서비스 업데이트 안내",
            body = "새 기능을 안내합니다.",
            publishedAtMillis = 1_754_000_000_000,
            updatedAtMillis = null,
        )
    }

    private class FakeNoticeRepository(
        private val responses: MutableList<suspend () -> List<Notice>>,
        initialReadNoticeIds: Set<String> = emptySet(),
    ) : NoticeRepository {
        private val readNoticeIds = initialReadNoticeIds.toMutableSet()
        val markedNoticeIds = mutableListOf<String>()
        var fetchCallCount = 0
            private set

        override suspend fun getPublishedNotices(): List<Notice> {
            fetchCallCount += 1
            return responses.removeAt(0).invoke()
        }

        override suspend fun getReadNoticeIds(): Set<String> = readNoticeIds.toSet()

        override suspend fun markNoticeRead(noticeId: String) {
            markedNoticeIds += noticeId
            readNoticeIds += noticeId
        }
    }
}
