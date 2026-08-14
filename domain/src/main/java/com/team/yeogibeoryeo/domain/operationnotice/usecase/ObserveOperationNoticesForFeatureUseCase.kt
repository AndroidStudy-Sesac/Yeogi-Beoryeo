package com.team.yeogibeoryeo.domain.operationnotice.usecase

import com.team.yeogibeoryeo.domain.app.AppVersionProvider
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNotice
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeFeature
import com.team.yeogibeoryeo.domain.operationnotice.policy.OperationNoticeDisplayPolicy
import com.team.yeogibeoryeo.domain.operationnotice.repository.DismissedOperationNoticeRepository
import com.team.yeogibeoryeo.domain.operationnotice.repository.OperationNoticeRepository
import com.team.yeogibeoryeo.domain.time.TimeProvider
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

class ObserveOperationNoticesForFeatureUseCase
@Inject
constructor(
    private val operationNoticeRepository: OperationNoticeRepository,
    private val dismissedOperationNoticeRepository: DismissedOperationNoticeRepository,
    private val timeProvider: TimeProvider,
    private val appVersionProvider: AppVersionProvider,
    private val operationNoticeDisplayPolicy: OperationNoticeDisplayPolicy,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(feature: OperationNoticeFeature): Flow<List<OperationNotice>> =
        combine(
            operationNoticeRepository.observeOperationNotices(),
            dismissedOperationNoticeRepository.observeDismissedNoticeIds(),
        ) { notices, dismissedIds -> OperationNoticeSource(notices, dismissedIds) }
            .flatMapLatest { source ->
                flow {
                    while (true) {
                        val nowMillis = timeProvider.currentTimeMillis()
                        emit(source.visibleNotices(feature = feature, nowMillis = nowMillis))

                        val delayMillis =
                            source.nextEvaluationDelayMillis(
                                feature = feature,
                                nowMillis = nowMillis,
                            ) ?: break
                        delay(delayMillis)
                    }
                }
            }
            .distinctUntilChanged()

    private fun OperationNoticeSource.visibleNotices(
        feature: OperationNoticeFeature,
        nowMillis: Long,
    ): List<OperationNotice> =
        candidates()
            .filter { notice -> notice.isActive(nowMillis = nowMillis) }
            .toList()
            .let { notices ->
                operationNoticeDisplayPolicy.visibleNotices(
                    notices = notices,
                    feature = feature,
                    dismissedNoticeIds = dismissedIds,
                )
            }

    private fun OperationNoticeSource.nextEvaluationDelayMillis(
        feature: OperationNoticeFeature,
        nowMillis: Long,
    ): Long? =
        operationNoticeDisplayPolicy
            .applicableNotices(notices = candidates().toList(), feature = feature)
            .mapNotNull { notice -> notice.nextBoundaryDelayMillis(nowMillis) }
            .minOrNull()

    private fun OperationNoticeSource.candidates(): Sequence<OperationNotice> =
        notices
            .asSequence()
            .filter { notice -> notice.matchesVersion(appVersionProvider.versionCode) }

    private fun OperationNotice.isActive(nowMillis: Long): Boolean {
        if (startsAtMillis != null && nowMillis < startsAtMillis) return false
        if (endsAtMillis != null && nowMillis > endsAtMillis) return false
        return true
    }

    private fun OperationNotice.matchesVersion(versionCode: Int): Boolean {
        if (minVersionCode != null && versionCode < minVersionCode) return false
        if (maxVersionCode != null && versionCode > maxVersionCode) return false
        return true
    }

}

private data class OperationNoticeSource(
    val notices: List<OperationNotice>,
    val dismissedIds: Set<String>,
)

private fun OperationNotice.nextBoundaryDelayMillis(nowMillis: Long): Long? {
    val startDelayMillis =
        startsAtMillis
            ?.takeIf { startsAtMillis -> startsAtMillis > nowMillis }
            ?.let { startsAtMillis -> startsAtMillis - nowMillis }
    val endDelayMillis =
        endsAtMillis
            ?.takeIf { endsAtMillis -> endsAtMillis >= nowMillis && endsAtMillis < Long.MAX_VALUE }
            ?.let { endsAtMillis -> endsAtMillis - nowMillis + 1 }

    return listOfNotNull(startDelayMillis, endDelayMillis).minOrNull()
}
