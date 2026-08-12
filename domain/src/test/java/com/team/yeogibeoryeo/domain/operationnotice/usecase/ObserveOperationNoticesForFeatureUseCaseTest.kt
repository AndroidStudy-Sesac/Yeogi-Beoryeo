package com.team.yeogibeoryeo.domain.operationnotice.usecase

import com.team.yeogibeoryeo.domain.app.AppVersionProvider
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNotice
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeFeature
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeSeverity
import com.team.yeogibeoryeo.domain.operationnotice.repository.DismissedOperationNoticeRepository
import com.team.yeogibeoryeo.domain.operationnotice.repository.OperationNoticeRepository
import com.team.yeogibeoryeo.domain.time.TimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveOperationNoticesForFeatureUseCaseTest {

    @Test
    fun `홈 공지는 유효한 공지만 우선순위 순서로 반환한다`() = runBlocking {
        val useCase = createUseCase(
            notices = listOf(
                notice(id = "low", severity = OperationNoticeSeverity.INFO, priority = 100),
                notice(id = "high", severity = OperationNoticeSeverity.WARNING, priority = 1),
                notice(id = "future", startsAtMillis = TEST_NOW + 1),
                notice(id = "other", affectedFeatures = setOf(OperationNoticeFeature.COLLECTION_SPOT_MAP)),
            ),
        )

        val result = useCase(OperationNoticeFeature.HOME).first()

        assertEquals(listOf("high", "low"), result.map { it.id })
    }

    @Test
    fun `닫은 공지 ID는 표시하지 않는다`() = runBlocking {
        val dismissedRepository = FakeDismissedOperationNoticeRepository(setOf("dismissed"))
        val useCase = createUseCase(
            notices = listOf(
                notice(id = "dismissed"),
                notice(id = "visible"),
            ),
            dismissedRepository = dismissedRepository,
        )

        val result = useCase(OperationNoticeFeature.HOME).first()

        assertEquals(listOf("visible"), result.map { it.id })
    }

    @Test
    fun `버전 범위 밖 공지는 표시하지 않는다`() = runBlocking {
        val useCase = createUseCase(
            notices = listOf(
                notice(id = "old", maxVersionCode = 3),
                notice(id = "new", minVersionCode = 5),
                notice(id = "current", minVersionCode = 4, maxVersionCode = 4),
            ),
        )

        val result = useCase(OperationNoticeFeature.HOME).first()

        assertEquals(listOf("current"), result.map { it.id })
    }

    @Test
    fun `구독 중 startsAt을 지나면 예정 공지를 표시한다`() = runTest {
        val useCase = createUseCase(
            notices = listOf(
                notice(
                    id = "scheduled",
                    startsAtMillis = 100,
                    endsAtMillis = 200,
                ),
            ),
            timeProvider = FakeTimeProvider { testScheduler.currentTime },
        )
        val results = mutableListOf<List<String>>()

        val job =
            launch {
                useCase(OperationNoticeFeature.HOME)
                    .map { notices -> notices.map { notice -> notice.id } }
                    .take(2)
                    .toList(results)
            }

        runCurrent()
        assertEquals(listOf(emptyList<String>()), results)

        advanceTimeBy(100)
        runCurrent()

        assertEquals(listOf(emptyList(), listOf("scheduled")), results)
        job.cancel()
    }

    @Test
    fun `구독 중 endsAt을 지나면 종료 공지를 숨긴다`() = runTest {
        val useCase = createUseCase(
            notices = listOf(
                notice(
                    id = "expired",
                    startsAtMillis = 0,
                    endsAtMillis = 100,
                ),
            ),
            timeProvider = FakeTimeProvider { testScheduler.currentTime },
        )
        val results = mutableListOf<List<String>>()

        val job =
            launch {
                useCase(OperationNoticeFeature.HOME)
                    .map { notices -> notices.map { notice -> notice.id } }
                    .take(2)
                    .toList(results)
            }

        runCurrent()
        assertEquals(listOf(listOf("expired")), results)

        advanceTimeBy(101)
        runCurrent()

        assertEquals(listOf(listOf("expired"), emptyList()), results)
        job.cancel()
    }

    private fun createUseCase(
        notices: List<OperationNotice>,
        dismissedRepository: DismissedOperationNoticeRepository = FakeDismissedOperationNoticeRepository(),
        timeProvider: TimeProvider = FakeTimeProvider(TEST_NOW),
    ): ObserveOperationNoticesForFeatureUseCase =
        ObserveOperationNoticesForFeatureUseCase(
            operationNoticeRepository = FakeOperationNoticeRepository(notices),
            dismissedOperationNoticeRepository = dismissedRepository,
            timeProvider = timeProvider,
            appVersionProvider = FakeAppVersionProvider(versionCode = 4),
        )

    private fun notice(
        id: String,
        severity: OperationNoticeSeverity = OperationNoticeSeverity.INFO,
        priority: Int = 0,
        affectedFeatures: Set<OperationNoticeFeature> = emptySet(),
        startsAtMillis: Long? = TEST_NOW - 1,
        endsAtMillis: Long? = TEST_NOW + 1,
        minVersionCode: Int? = null,
        maxVersionCode: Int? = null,
    ): OperationNotice =
        OperationNotice(
            id = id,
            severity = severity,
            priority = priority,
            title = "title",
            message = "message",
            affectedFeatures = affectedFeatures,
            startsAtMillis = startsAtMillis,
            endsAtMillis = endsAtMillis,
            minVersionCode = minVersionCode,
            maxVersionCode = maxVersionCode,
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

    private class FakeDismissedOperationNoticeRepository(
        dismissedIds: Set<String> = emptySet(),
    ) : DismissedOperationNoticeRepository {
        private val dismissedIds = MutableStateFlow(dismissedIds)

        override fun observeDismissedNoticeIds(): Flow<Set<String>> = dismissedIds

        override suspend fun dismissNotice(id: String) {
            dismissedIds.value = dismissedIds.value + id
        }
    }

    private class FakeTimeProvider(
        private val nowMillis: () -> Long,
    ) : TimeProvider {
        constructor(nowMillis: Long) : this({ nowMillis })

        override fun currentTimeMillis(): Long = nowMillis()
    }

    private class FakeAppVersionProvider(
        override val versionCode: Int,
    ) : AppVersionProvider

    private companion object {
        const val TEST_NOW = 1_000L
    }
}
