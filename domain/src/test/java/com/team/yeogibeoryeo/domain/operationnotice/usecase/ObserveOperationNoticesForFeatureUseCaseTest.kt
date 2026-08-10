package com.team.yeogibeoryeo.domain.operationnotice.usecase

import com.team.yeogibeoryeo.domain.app.AppVersionProvider
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNotice
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeFeature
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeSeverity
import com.team.yeogibeoryeo.domain.operationnotice.repository.DismissedOperationNoticeRepository
import com.team.yeogibeoryeo.domain.operationnotice.repository.OperationNoticeRepository
import com.team.yeogibeoryeo.domain.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

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

    private fun createUseCase(
        notices: List<OperationNotice>,
        dismissedRepository: DismissedOperationNoticeRepository = FakeDismissedOperationNoticeRepository(),
    ): ObserveOperationNoticesForFeatureUseCase =
        ObserveOperationNoticesForFeatureUseCase(
            operationNoticeRepository = FakeOperationNoticeRepository(notices),
            dismissedOperationNoticeRepository = dismissedRepository,
            timeProvider = FakeTimeProvider(TEST_NOW),
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
        private val nowMillis: Long,
    ) : TimeProvider {
        override fun currentTimeMillis(): Long = nowMillis
    }

    private class FakeAppVersionProvider(
        override val versionCode: Int,
    ) : AppVersionProvider

    private companion object {
        const val TEST_NOW = 1_000L
    }
}

