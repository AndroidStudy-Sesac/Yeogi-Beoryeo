package com.team.yeogibeoryeo.presentation.map

import androidx.annotation.StringRes
import com.team.yeogibeoryeo.presentation.R

object MapLocationNotices {
    @StringRes
    val SpotSearchFailureMessageResId: Int = R.string.map_spot_search_failure_message

    @StringRes
    val CurrentLocationSpotSearchFailureMessageResId: Int =
        R.string.map_current_location_spot_search_failure_message

    val PermissionDenied = MapLocationNotice(
        titleResId = R.string.map_location_permission_denied_title,
        messageResId = R.string.map_location_permission_denied_message,
        action = MapLocationNoticeAction.OpenAppSettings,
    )

    val LocationServiceDisabled = MapLocationNotice(
        titleResId = R.string.map_location_service_disabled_title,
        messageResId = R.string.map_location_service_disabled_message,
        action = MapLocationNoticeAction.OpenLocationSettings,
    )

    val CurrentLocationUnavailable = MapLocationNotice(
        titleResId = R.string.map_current_location_unavailable_title,
        messageResId = R.string.map_current_location_unavailable_message,
    )
}
