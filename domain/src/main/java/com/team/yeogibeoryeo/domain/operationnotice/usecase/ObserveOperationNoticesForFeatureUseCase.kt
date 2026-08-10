package com.team.yeogibeoryeo.domain.operationnotice.usecase

import com.team.yeogibeoryeo.domain.app.AppVersionProvider
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNotice
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeFeature
import com.team.yeogibeoryeo.domain.operationnotice.repository.DismissedOperationNoticeRepository
import com.team.yeogibeoryeo.domain.operationnotice.repository.OperationNoticeRepository
import com.team.yeogibeoryeo.domain.time.TimeProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveOperationNoticesForFeatureUseCase
@Inject
constructor(
    private val operationNoticeRepository: OperationNoticeRepository,
    private val dismissedOperationNoticeRepository: DismissedOperationNoticeRepository,
    private val timeProvider: TimeProvider,
    private val appVersionProvider: AppVersionProvider,
) {
    operator fun invoke(feature: OperationNoticeFeature): Flow<List<OperationNotice>> =
        combine(
            operationNoticeRepository.observeOperationNotices(),
            dismissedOperationNoticeRepository.observeDismissedNoticeIds(),
        ) { notices, dismissedIds ->
            notices
                .asSequence()
                .filter { notice -> notice.id !in dismissedIds }
                .filter { notice -> notice.isActive(nowMillis = timeProvider.currentTimeMillis()) }
                .filter { notice -> notice.matchesVersion(appVersionProvider.versionCode) }
                .filter { notice -> notice.matchesFeature(feature) }
                .sortedWith(OperationNoticeDisplayComparator)
                .toList()
        }

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
