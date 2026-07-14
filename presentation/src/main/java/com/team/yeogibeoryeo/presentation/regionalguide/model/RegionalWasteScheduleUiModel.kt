package com.team.yeogibeoryeo.presentation.regionalguide.model

data class RegionalWasteScheduleUiModel(
    val wasteTypeName: String,
    val disposalDays: String?,
    val disposalTime: String,
    val disposalMethod: String,
    val disposalPlace: String? = null,
)
