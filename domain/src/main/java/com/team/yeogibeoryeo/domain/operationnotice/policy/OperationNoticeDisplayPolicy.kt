package com.team.yeogibeoryeo.domain.operationnotice.policy

import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNotice
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeFeature
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeSeverity
import javax.inject.Inject

class OperationNoticeDisplayPolicy
@Inject
constructor() {
    fun visibleNotices(
        notices: List<OperationNotice>,
        feature: OperationNoticeFeature,
        dismissedNoticeIds: Set<String>,
    ): List<OperationNotice> =
        applicableNotices(notices = notices, feature = feature)
            .filterNot { notice -> notice.isDismissed(dismissedNoticeIds) }
            .sortedWith(OperationNoticeDisplayComparator)

    fun applicableNotices(
        notices: List<OperationNotice>,
        feature: OperationNoticeFeature,
    ): List<OperationNotice> =
        notices.filter { notice -> notice.isApplicableTo(feature) }

    private fun OperationNotice.isApplicableTo(feature: OperationNoticeFeature): Boolean =
        when {
            affectedFeatures.isEmpty() -> feature == OperationNoticeFeature.HOME
            else -> feature in affectedFeatures
        }

    private fun OperationNotice.isDismissed(dismissedNoticeIds: Set<String>): Boolean =
        severity != OperationNoticeSeverity.CRITICAL && id in dismissedNoticeIds

    private companion object {
        val OperationNoticeDisplayComparator =
            compareByDescending<OperationNotice> { it.severity.sortRank }
                .thenByDescending { it.priority }
                .thenByDescending { it.startsAtMillis ?: Long.MIN_VALUE }
    }
}
