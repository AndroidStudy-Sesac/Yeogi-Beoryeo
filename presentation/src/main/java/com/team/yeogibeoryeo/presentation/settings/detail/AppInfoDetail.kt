package com.team.yeogibeoryeo.presentation.settings.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.settings.components.SettingsDetailContent
import com.team.yeogibeoryeo.presentation.settings.components.SettingsInfoRow
import com.team.yeogibeoryeo.presentation.settings.components.SettingsSection

@Composable
internal fun AppInfoDetail(
    appVersionName: String,
) {
    SettingsDetailContent {
        SettingsSection(
            title = stringResource(R.string.settings_app_info_title),
            description = stringResource(R.string.settings_app_info_description),
        )
        SettingsInfoRow(
            label = stringResource(R.string.settings_app_info_version_label),
            value = appVersionName,
        )
        SettingsInfoRow(
            label = stringResource(R.string.settings_app_info_features_label),
            value = stringResource(R.string.settings_app_info_features),
        )
    }
}
