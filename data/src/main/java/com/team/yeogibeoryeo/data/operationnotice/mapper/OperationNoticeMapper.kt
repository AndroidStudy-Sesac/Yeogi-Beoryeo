package com.team.yeogibeoryeo.data.operationnotice.mapper

import com.team.yeogibeoryeo.data.operationnotice.remote.OperationNoticeDto
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNotice
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeFeature
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeSeverity
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

fun OperationNoticeDto.toDomainOrNull(): OperationNotice? {
    if (!enabled) return null

    val normalizedId = id.trim()
    val normalizedTitle = title.trim()
    val normalizedMessage = message.trim()
    if (normalizedId.isEmpty() || normalizedTitle.isEmpty() || normalizedMessage.isEmpty()) return null

    val severity = OperationNoticeSeverity.fromRemoteValue(severity.trim()) ?: OperationNoticeSeverity.INFO
    val features =
        affectedFeatures
            .mapNotNull { value -> OperationNoticeFeature.fromRemoteValue(value.trim()) }
            .toSet()

    return OperationNotice(
        id = normalizedId,
        severity = severity,
        priority = priority,
        title = normalizedTitle,
        message = normalizedMessage,
        affectedFeatures = features,
        startsAtMillis = startsAt.toEpochMillisOrNull(),
        endsAtMillis = endsAt.toEpochMillisOrNull(),
        minVersionCode = minVersionCode,
        maxVersionCode = maxVersionCode,
        actionLabel = actionLabel?.trim()?.takeIf(String::isNotEmpty),
        actionUrl = actionUrl?.trim()?.takeIf(String::isNotEmpty),
    )
}

private fun String?.toEpochMillisOrNull(): Long? {
    val value = this?.trim()?.takeIf(String::isNotEmpty) ?: return null
    return try {
        OffsetDateTime.parse(value).toInstant().toEpochMilli()
    } catch (exception: DateTimeParseException) {
        null
    }
}

