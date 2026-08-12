package com.team.yeogibeoryeo.data.operationnotice.mapper

import com.team.yeogibeoryeo.data.operationnotice.remote.OperationNoticeDto
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNotice
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeFeature
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeSeverity
import java.net.URI
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

fun OperationNoticeDto.toDomainOrNull(): OperationNotice? {
    if (!enabled) return null

    val normalizedId = id.trim()
    val normalizedTitle = title.trim()
    val normalizedMessage = message.trim()
    if (normalizedId.isEmpty() || normalizedTitle.isEmpty() || normalizedMessage.isEmpty()) return null

    val severity = OperationNoticeSeverity.fromRemoteValue(severity.trim()) ?: return null
    val features = affectedFeatures.toOperationNoticeFeaturesOrNull() ?: return null
    val startsAtMillis = startsAt.toEpochMillisOrNull()
    val endsAtMillis = endsAt.toEpochMillisOrNull()
    if (startsAt.hasText() && startsAtMillis == null) return null
    if (endsAt.hasText() && endsAtMillis == null) return null

    val normalizedActionLabel = actionLabel?.trim()?.takeIf(String::isNotEmpty)
    val normalizedActionUrl = actionUrl?.trim()?.takeIf(String::isNotEmpty)
    val safeActionUrl = normalizedActionUrl?.takeIf { url -> url.hasAllowedScheme() }
    val shouldShowAction = normalizedActionLabel != null && safeActionUrl != null

    return OperationNotice(
        id = normalizedId,
        severity = severity,
        priority = priority,
        title = normalizedTitle,
        message = normalizedMessage,
        affectedFeatures = features,
        startsAtMillis = startsAtMillis,
        endsAtMillis = endsAtMillis,
        minVersionCode = minVersionCode,
        maxVersionCode = maxVersionCode,
        actionLabel = normalizedActionLabel?.takeIf { shouldShowAction },
        actionUrl = safeActionUrl?.takeIf { shouldShowAction },
    )
}

private fun List<String>.toOperationNoticeFeaturesOrNull(): Set<OperationNoticeFeature>? {
    if (isEmpty()) return emptySet()

    val features =
        mapNotNull { value -> OperationNoticeFeature.fromRemoteValue(value.trim()) }
            .toSet()
    return features.takeIf { it.isNotEmpty() }
}

private fun String?.toEpochMillisOrNull(): Long? {
    val value = this?.trim()?.takeIf(String::isNotEmpty) ?: return null
    return try {
        OffsetDateTime.parse(value).toInstant().toEpochMilli()
    } catch (exception: DateTimeParseException) {
        null
    }
}

private fun String?.hasText(): Boolean = !this?.trim().isNullOrEmpty()

private fun String.hasAllowedScheme(): Boolean =
    runCatching {
        when (URI(this).scheme?.lowercase()) {
            "http", "https" -> true
            else -> false
        }
    }.getOrDefault(false)
