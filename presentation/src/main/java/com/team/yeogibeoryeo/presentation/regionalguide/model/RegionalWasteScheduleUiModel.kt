package com.team.yeogibeoryeo.presentation.regionalguide.model

import androidx.annotation.StringRes

data class RegionalWasteScheduleUiModel(
    val wasteTypeName: String,
    val disposalDays: String? = null,
    val disposalTime: String? = null,
    val disposalTimeFormat: RegionalWasteScheduleTimeFormat? = null,
    val disposalMethod: String? = null,
    val disposalPlace: String? = null,
)

data class RegionalWasteScheduleTimeFormat(
    @param:StringRes val resId: Int,
    val args: List<String>,
)
