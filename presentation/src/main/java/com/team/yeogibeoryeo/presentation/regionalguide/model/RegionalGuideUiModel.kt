package com.team.yeogibeoryeo.presentation.regionalguide.model

data class RegionalGuideUiModel(
    val regionName: String,
    val managementZoneName: String?,
    val targetRegionName: String?,
    val disposalPlaceType: String?,
    val disposalPlaceDescription: String?,
    val schedules: List<RegionalWasteScheduleUiModel>,
    val uncollectedDays: String?,
    val departmentInfo: String?
)