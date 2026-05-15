@file:OptIn(InternalSerializationApi::class)

package com.team.yeogibeoryeo.data.item.remote.dto

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
data class ItemGuideResponseDto(
    val response: ItemGuideResponseBodyDto,
)

@Serializable
data class ItemGuideResponseBodyDto(
    val header: ItemGuideHeaderDto,
    val body: ItemGuideBodyDto,
)

@Serializable
data class ItemGuideHeaderDto(
    val resultCode: String,
    val resultMsg: String,
)

@Serializable
data class ItemGuideBodyDto(
    val items: ItemGuideItemsDto?,
    val numOfRows: Int,
    val pageNo: Int,
    val totalCount: Int,
)

@Serializable
data class ItemGuideItemsDto(
    val item: List<ItemGuideDto>,
)
