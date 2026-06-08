package com.team.yeogibeoryeo.presentation.map

data class MapLocationNotice(
    val title: String,
    val message: String,
    val action: MapLocationNoticeAction? = null,
)

enum class MapLocationNoticeAction {
    OpenAppSettings,
    OpenLocationSettings,
}
