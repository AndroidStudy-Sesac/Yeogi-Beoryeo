package com.team.yeogibeoryeo.presentation.settings.detail

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.settings.components.SettingsDetailContent
import com.team.yeogibeoryeo.presentation.settings.components.SettingsInfoRow
import com.team.yeogibeoryeo.presentation.settings.components.SettingsSection

@Composable
internal fun LocationPermissionDetail(
    onOpenAppSettingsClick: () -> Unit,
) {
    SettingsDetailContent {
        SettingsSection(
            title = stringResource(R.string.settings_location_permission_title),
            description = stringResource(R.string.settings_location_permission_description),
        )
        SettingsInfoRow(
            label = stringResource(R.string.settings_location_permission_used_permissions_label),
            value = stringResource(R.string.settings_location_permission_used_permissions),
        )
        SettingsInfoRow(
            label = stringResource(R.string.settings_location_permission_stored_info_label),
            value = stringResource(R.string.settings_location_permission_stored_info),
        )
        Button(onClick = onOpenAppSettingsClick) {
            Text(text = stringResource(R.string.settings_open_app_settings))
        }
    }
}
