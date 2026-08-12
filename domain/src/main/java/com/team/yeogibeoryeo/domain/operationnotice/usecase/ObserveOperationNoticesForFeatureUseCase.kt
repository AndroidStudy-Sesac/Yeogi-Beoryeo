package com.team.yeogibeoryeo.domain.operationnotice.usecase

import com.team.yeogibeoryeo.domain.app.AppVersionProvider
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNotice
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeFeature
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
        candidates(feature)
            .filter { notice -> notice.isActive(nowMillis = nowMillis) }
            .sortedWith(OperationNoticeDisplayComparator)
            .toList()

    private fun OperationNoticeSource.nextEvaluationDelayMillis(
        feature: OperationNoticeFeature,
        nowMillis: Long,
    ): Long? =
        candidates(feature)
            .mapNotNull { notice -> notice.nextBoundaryDelayMillis(nowMillis) }
            .minOrNull()

    private fun OperationNoticeSource.candidates(feature: OperationNoticeFeature): Sequence<OperationNotice> =
        notices
            .asSequence()
            .filter { notice -> notice.id !in dismissedIds }
            .filter { notice -> notice.matchesVersion(appVersionProvider.versionCode) }
            .filter { notice -> notice.matchesFeature(feature) }

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

    private fun OperationNotice.matchesFeature(feature: OperationNoticeFeature): Boolean =
        when {
            affectedFeatures.isEmpty() -> feature == OperationNoticeFeature.HOME
            else -> feature in affectedFeatures
        }

    private companion object {
        val OperationNoticeDisplayComparator =
            compareByDescending<OperationNotice> { it.severity.sortRank }
                .thenByDescending { it.priority }
                .thenByDescending { it.startsAtMillis ?: Long.MIN_VALUE }
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
