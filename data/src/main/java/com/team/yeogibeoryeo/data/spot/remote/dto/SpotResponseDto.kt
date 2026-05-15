@file:OptIn(InternalSerializationApi::class)

package com.team.yeogibeoryeo.data.spot.remote.dto

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SpotResponseDto(
    val response: SpotResponseBodyDto,
)

@Serializable
data class SpotResponseBodyDto(
    val header: SpotHeaderDto,
    val body: SpotBodyDto,
)

@Serializable
data class SpotHeaderDto(
    val resultCode: String,
    val resultMsg: String,
)

@Serializable
data class SpotBodyDto(
    val items: SpotItemsDto? = null,
    val numOfRows: JsonElement? = null,
    val pageNo: JsonElement? = null,
    val totalCount: JsonElement? = null,
)

@Serializable
data class SpotItemsDto(
    val item: List<SpotItemDto> = emptyList(),
)