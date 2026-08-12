package com.team.yeogibeoryeo.presentation.operationnotice

import com.team.yeogibeoryeo.domain.app.AppVersionProvider
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNotice
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeFeature
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeSeverity
import com.team.yeogibeoryeo.domain.operationnotice.policy.OperationNoticeDisplayPolicy
import com.team.yeogibeoryeo.domain.operationnotice.repository.DismissedOperationNoticeRepository
import com.team.yeogibeoryeo.domain.operationnotice.repository.OperationNoticeRepository
import com.team.yeogibeoryeo.domain.operationnotice.usecase.DismissOperationNoticeUseCase
import com.team.yeogibeoryeo.domain.operationnotice.usecase.ObserveOperationNoticesForFeatureUseCase
import com.team.yeogibeoryeo.domain.time.TimeProvider
import com.team.yeogibeoryeo.presentation.search.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OperationNoticeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `홈 ViewModel은 홈 대상 첫 공지를 UI 모델로 노출한다`() = runTest {
        val viewModel = HomeOperationNoticeViewModel(
            observeOperationNoticesForFeatureUseCase = createObserveUseCase(
                notices = listOf(
                    notice(id = "map", affectedFeatures = setOf(OperationNoticeFeature.COLLECTION_SPOT_MAP)),
                    notice(id = "home", title = "홈 공지", affectedFeatures = setOf(OperationNoticeFeature.HOME)),
                ),
            ),
            dismissOperationNoticeUseCase = DismissOperationNoticeUseCase(FakeDismissedRepository()),
        )

        val notice = viewModel.notice.first { notice -> notice != null }

        assertEquals("home", notice?.id)
        assertEquals("홈 공지", notice?.title)
    }

    @Test
    fun `지도 ViewModel은 지도 대상 첫 공지를 UI 모델로 노출한다`() = runTest {
        val viewModel = MapOperationNoticeViewModel(
            observeOperationNoticesForFeatureUseCase = createObserveUseCase(
                notices = listOf(
                    notice(id = "home", affectedFeatures = setOf(OperationNoticeFeature.HOME)),
                    notice(id = "map", title = "지도 공지", affectedFeatures = setOf(OperationNoticeFeature.COLLECTION_SPOT_MAP)),
                ),
            ),
            dismissOperationNoticeUseCase = DismissOperationNoticeUseCase(FakeDismissedRepository()),
        )

        val notice = viewModel.notice.first { notice -> notice != null }

        assertEquals("map", notice?.id)
        assertEquals("지도 공지", notice?.title)
    }

    @Test
    fun `홈 ViewModel dismissNotice는 닫기 usecase에 ID를 전달한다`() = runTest {
        val dismissedRepository = FakeDismissedRepository()
        val viewModel = HomeOperationNoticeViewModel(
            observeOperationNoticesForFeatureUseCase = createObserveUseCase(emptyList()),
            dismissOperationNoticeUseCase = DismissOperationNoticeUseCase(dismissedRepository),
        )

        viewModel.dismissNotice("notice-1")

        assertEquals(listOf("notice-1"), dismissedRepository.dismissedIds)
    }

    @Test
    fun `지도 ViewModel dismissNotice는 닫기 usecase에 ID를 전달한다`() = runTest {
        val dismissedRepository = FakeDismissedRepository()
        val viewModel = MapOperationNoticeViewModel(
            observeOperationNoticesForFeatureUseCase = createObserveUseCase(emptyList()),
            dismissOperationNoticeUseCase = DismissOperationNoticeUseCase(dismissedRepository),
        )

        viewModel.dismissNotice("notice-1")

        assertEquals(listOf("notice-1"), dismissedRepository.dismissedIds)
    }

    private fun createObserveUseCase(
        notices: List<OperationNotice>,
    ): ObserveOperationNoticesForFeatureUseCase =
        ObserveOperationNoticesForFeatureUseCase(
            operationNoticeRepository = FakeOperationNoticeRepository(notices),
            dismissedOperationNoticeRepository = FakeDismissedRepository(),
            timeProvider = FakeTimeProvider,
            appVersionProvider = FakeAppVersionProvider,
            operationNoticeDisplayPolicy = OperationNoticeDisplayPolicy(),
        )

    private fun notice(
        id: String,
        title: String = "운영 공지",
        affectedFeatures: Set<OperationNoticeFeature>,
    ): OperationNotice =
        OperationNotice(
            id = id,
            severity = OperationNoticeSeverity.WARNING,
            priority = 0,
            title = title,
            message = "공지 내용",
            affectedFeatures = affectedFeatures,
            startsAtMillis = null,
            endsAtMillis = null,
            minVersionCode = null,
            maxVersionCode = null,
            actionLabel = null,
            actionUrl = null,
        )

    private class FakeOperationNoticeRepository(
        notices: List<OperationNotice>,
    ) : OperationNoticeRepository {
        private val notices = MutableStateFlow(notices)

        override fun observeOperationNotices(): Flow<List<OperationNotice>> = notices

        override suspend fun refreshOperationNotices() = Unit
    }

    private class FakeDismissedRepository : DismissedOperationNoticeRepository {
        val dismissedIds = mutableListOf<String>()

        override fun observeDismissedNoticeIds(): Flow<Set<String>> =
            MutableStateFlow(emptySet())

        override suspend fun dismissNotice(id: String) {
            dismissedIds += id
        }
    }

    private object FakeTimeProvider : TimeProvider {
        override fun currentTimeMillis(): Long = 1_000L
    }

    private object FakeAppVersionProvider : AppVersionProvider {
        override val versionCode: Int = 1
    }
}
