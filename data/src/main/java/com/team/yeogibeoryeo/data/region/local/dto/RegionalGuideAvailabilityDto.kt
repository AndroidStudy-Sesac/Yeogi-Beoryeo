package com.team.yeogibeoryeo.data.region.local.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegionalGuideAvailabilityDto(
    val sidoName: String,
    val sigunguName: String,
    val managementZoneName: String,
    val targetRegionName: String
)
