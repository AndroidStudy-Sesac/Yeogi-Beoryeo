package com.team.yeogibeoryeo.presentation.settings

import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheEntry
import com.team.yeogibeoryeo.domain.spot.repository.RecentCurrentLocationSpotCacheRepository
import com.team.yeogibeoryeo.domain.spot.usecase.ClearRecentCurrentLocationSpotsUseCase
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.cache.RecentCurrentLocationCacheClearNotifier
import com.team.yeogibeoryeo.presentation.search.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsCacheViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `위치 캐시 삭제 성공 시 완료 메시지를 표시한다`() =
        runTest {
            val repository = FakeRecentCurrentLocationSpotCacheRepository()
            val viewModel = createViewModel(repository)
            val event = async { viewModel.events.first() }
            runCurrent()

            viewModel.clearLocationCache()
            advanceUntilIdle()

            assertEquals(1, repository.clearCallCount)
            assertFalse(viewModel.uiState.value.isClearingLocationCache)
            assertEquals(
                R.string.settings_cache_delete_success_message,
                (event.await() as SettingsCacheEvent.ShowLocationCacheMessage).messageResId,
            )
        }

    @Test
    fun `위치 캐시 삭제 실패 시 실패 메시지를 표시한다`() =
        runTest {
            val repository = FakeRecentCurrentLocationSpotCacheRepository(
                clearThrowable = IllegalStateException("clear failed"),
            )
            val viewModel = createViewModel(repository)
            val event = async { viewModel.events.first() }
            runCurrent()

            viewModel.clearLocationCache()
            advanceUntilIdle()

            assertEquals(1, repository.clearCallCount)
            assertFalse(viewModel.uiState.value.isClearingLocationCache)
            assertEquals(
                R.string.settings_cache_delete_failure_message,
                (event.await() as SettingsCacheEvent.ShowLocationCacheMessage).messageResId,
            )
        }

    @Test
    fun `위치 캐시 삭제 중에는 중복 삭제 요청을 무시한다`() =
        runTest {
            val clearCompletion = CompletableDeferred<Unit>()
            val repository = FakeRecentCurrentLocationSpotCacheRepository(
                clearCompletion = clearCompletion,
            )
            val viewModel = createViewModel(repository)

            viewModel.clearLocationCache()
            repository.clearStarted.await()
            assertTrue(viewModel.uiState.value.isClearingLocationCache)

            viewModel.clearLocationCache()
            assertEquals(1, repository.clearCallCount)

            val event = async { viewModel.events.first() }
            runCurrent()
            clearCompletion.complete(Unit)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isClearingLocationCache)
            assertEquals(
                R.string.settings_cache_delete_success_message,
                (event.await() as SettingsCacheEvent.ShowLocationCacheMessage).messageResId,
            )
        }

    @Test
    fun `위치 캐시 삭제 요청 시 영속 캐시 삭제 완료 전 현재 위치 검색 무효화 이벤트를 전송한다`() =
        runTest {
            val notifier = RecentCurrentLocationCacheClearNotifier()
            val clearCompletion = CompletableDeferred<Unit>()
            val repository = FakeRecentCurrentLocationSpotCacheRepository(
                clearCompletion = clearCompletion,
            )
            val viewModel = createViewModel(
                repository = repository,
                recentCurrentLocationCacheClearNotifier = notifier,
            )
            val clearEvent = async { notifier.events.first() }
            runCurrent()

            viewModel.clearLocationCache()
            repository.clearStarted.await()

            clearEvent.await()
            assertTrue(viewModel.uiState.value.isClearingLocationCache)

            clearCompletion.complete(Unit)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isClearingLocationCache)
        }

    private fun createViewModel(
        repository: RecentCurrentLocationSpotCacheRepository,
        recentCurrentLocationCacheClearNotifier: RecentCurrentLocationCacheClearNotifier =
            RecentCurrentLocationCacheClearNotifier(),
    ): SettingsCacheViewModel {
        return SettingsCacheViewModel(
            clearRecentCurrentLocationSpotsUseCase =
                ClearRecentCurrentLocationSpotsUseCase(repository),
            recentCurrentLocationCacheClearNotifier = recentCurrentLocationCacheClearNotifier,
        )
    }

    private class FakeRecentCurrentLocationSpotCacheRepository(
        private val clearThrowable: Throwable? = null,
        private val clearCompletion: CompletableDeferred<Unit>? = null,
    ) : RecentCurrentLocationSpotCacheRepository {
        val clearStarted = CompletableDeferred<Unit>()
        var clearCallCount = 0
            private set

        override suspend fun getRecentCurrentLocationSpots(): RecentCurrentLocationSpotCacheEntry? {
            return null
        }

        override suspend fun saveRecentCurrentLocationSpots(entry: RecentCurrentLocationSpotCacheEntry) = Unit

        override suspend fun clearRecentCurrentLocationSpots() {
            clearCallCount += 1
            clearStarted.complete(Unit)
            clearCompletion?.await()
            clearThrowable?.let { throwable -> throw throwable }
        }
    }
}
