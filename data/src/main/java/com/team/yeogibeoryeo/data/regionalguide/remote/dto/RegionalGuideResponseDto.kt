@file:OptIn(InternalSerializationApi::class)

package com.team.yeogibeoryeo.data.regionalguide.remote.dto

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 공공데이터 API 실제 응답 구조 (response -> body -> items -> item)
 */

@Serializable
data class RegionalGuideRootDto(
    @SerialName("response") val response: RegionalGuideResponseDto? = null
)

@Serializable
data class RegionalGuideResponseDto(
    @SerialName("body") val body: RegionalGuideBodyDto? = null
)

@Serializable
data class RegionalGuideBodyDto(
    @SerialName("pageNo") val pageNo: Int? = null,
    @SerialName("numOfRows") val numOfRows: Int? = null,
    @SerialName("totalCount") val totalCount: Int? = null,
    @SerialName("items") val items: RegionalGuideItemsDto? = null
)

@Serializable
data class RegionalGuideItemsDto(
    @SerialName("item") val item: List<RegionalGuideItemDto> = emptyList()
)
