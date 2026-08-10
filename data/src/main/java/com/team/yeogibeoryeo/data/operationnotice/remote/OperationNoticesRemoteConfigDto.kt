package com.team.yeogibeoryeo.data.operationnotice.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OperationNoticesRemoteConfigDto(
    @SerialName("schemaVersion")
    val schemaVersion: Int = 0,
    @SerialName("notices")
    val notices: List<OperationNoticeDto> = emptyList(),
)

@Serializable
data class OperationNoticeDto(
    @SerialName("id")
    val id: String = "",
    @SerialName("enabled")
    val enabled: Boolean = false,
    @SerialName("severity")
    val severity: String = "",
    @SerialName("priority")
    val priority: Int = 0,
    @SerialName("title")
    val title: String = "",
    @SerialName("message")
    val message: String = "",
    @SerialName("affectedFeatures")
    val affectedFeatures: List<String> = emptyList(),
    @SerialName("startsAt")
    val startsAt: String? = null,
    @SerialName("endsAt")
    val endsAt: String? = null,
    @SerialName("minVersionCode")
    val minVersionCode: Int? = null,
    @SerialName("maxVersionCode")
    val maxVersionCode: Int? = null,
    @SerialName("actionLabel")
    val actionLabel: String? = null,
    @SerialName("actionUrl")
    val actionUrl: String? = null,
)

