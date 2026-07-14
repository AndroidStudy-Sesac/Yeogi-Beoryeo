package com.team.yeogibeoryeo.presentation.regionalguide.model

data class RegionalWasteScheduleUiModel(
    val wasteTypeName: String,
    val disposalDays: String? = null,
    val disposalTime: String? = null,
    val disposalMethod: String? = null,
    val disposalPlace: String? = null,
)
