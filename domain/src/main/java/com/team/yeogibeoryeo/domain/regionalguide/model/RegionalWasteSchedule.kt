package com.team.yeogibeoryeo.domain.regionalguide.model

data class RegionalWasteSchedule(
    val wasteType: RegionalWasteType,
    val disposalDays: String? = null,
    val disposalStartTime: String? = null,
    val disposalEndTime: String? = null,
    val disposalMethod: String? = null
)
