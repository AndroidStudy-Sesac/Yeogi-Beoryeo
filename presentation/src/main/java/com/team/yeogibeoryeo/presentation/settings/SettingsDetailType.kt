package com.team.yeogibeoryeo.presentation.settings

import androidx.annotation.StringRes
import com.team.yeogibeoryeo.presentation.R

enum class SettingsDetailType(
    @param:StringRes val titleResId: Int,
) {
    Notice(R.string.settings_notice_title),
    Contact(R.string.settings_contact_title),
    AppInfo(R.string.settings_app_info_title),
    LocationPermission(R.string.settings_location_permission_title),
    Terms(R.string.settings_terms_title),
    Sources(R.string.settings_sources_title),
    Cache(R.string.settings_cache_title),
}
