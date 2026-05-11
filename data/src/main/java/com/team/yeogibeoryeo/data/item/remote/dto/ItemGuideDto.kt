@file:OptIn(InternalSerializationApi::class)

package com.team.yeogibeoryeo.data.item.remote.dto

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
data class ItemGuideDto(
    val itemNm: String,
    val dschgMthd: String,
)
