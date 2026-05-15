@file:OptIn(InternalSerializationApi::class)

package com.team.yeogibeoryeo.data.spot.remote.dto

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
data class SpotItemDto(
    val spotNm: String? = null,
    val addrBase: String? = null,
    val addrDtl: String? = null,
)