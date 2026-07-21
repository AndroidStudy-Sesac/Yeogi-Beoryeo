package com.team.yeogibeoryeo.presentation.regionalguide.model

data class RegionalWasteScheduleUiModel(
    val wasteTypeName: String,
    val disposalDays: String? = null,
    val disposalTime: RegionalWasteScheduleTime? = null,
    val disposalMethod: String? = null,
    val disposalPlace: String? = null,
)

sealed interface RegionalWasteScheduleTime {
    data class Value(
        val value: String,
    ) : RegionalWasteScheduleTime

    data class Range(
        val startTime: String,
        val endTime: String,
    ) : RegionalWasteScheduleTime

    data class After(
        val startTime: String,
    ) : RegionalWasteScheduleTime

    data class Before(
        val endTime: String,
    ) : RegionalWasteScheduleTime
}
