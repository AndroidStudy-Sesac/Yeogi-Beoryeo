package com.team.yeogibeoryeo.presentation.operationnotice

import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNotice
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeSeverity

data class OperationNoticeUiModel(
    val id: String,
    val severity: OperationNoticeSeverity,
    val title: String,
    val message: String,
    val actionLabel: String?,
    val actionUrl: String?,
) {
    val isDismissible: Boolean
        get() = severity != OperationNoticeSeverity.CRITICAL
}

fun OperationNotice.toUiModel(): OperationNoticeUiModel =
    OperationNoticeUiModel(
        id = id,
        severity = severity,
        title = title,
        message = message,
        actionLabel = actionLabel,
        actionUrl = actionUrl,
    )

