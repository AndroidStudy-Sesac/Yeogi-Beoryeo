package com.team.yeogibeoryeo.presentation.map

import androidx.annotation.StringRes

data class MapLocationNotice(
    @param:StringRes val titleResId: Int,
    @param:StringRes val messageResId: Int,
    val action: MapLocationNoticeAction? = null,
)

enum class MapLocationNoticeAction {
    OpenAppSettings,
    OpenLocationSettings,
}
