package com.team.yeogibeoryeo.data.region.local.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdministrativeRegionDto(
    val adminCode: String,
    val sidoName: String,
    val sigunguName: String,
    val eupmyeondongName: String,
    val fullName: String,
    val createdDate: String? = null
)