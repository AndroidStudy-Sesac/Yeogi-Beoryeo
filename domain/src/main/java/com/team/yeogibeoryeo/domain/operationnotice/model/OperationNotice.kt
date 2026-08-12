package com.team.yeogibeoryeo.domain.operationnotice.model

data class OperationNotice(
    val id: String,
    val severity: OperationNoticeSeverity,
    val priority: Int,
    val title: String,
    val message: String,
    val affectedFeatures: Set<OperationNoticeFeature>,
    val startsAtMillis: Long?,
    val endsAtMillis: Long?,
    val minVersionCode: Int?,
    val maxVersionCode: Int?,
    val actionLabel: String?,
    val actionUrl: String?,
)

