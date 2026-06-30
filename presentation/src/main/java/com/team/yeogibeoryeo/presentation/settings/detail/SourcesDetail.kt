package com.team.yeogibeoryeo.presentation.settings.detail

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.settings.components.SettingsDetailContent
import com.team.yeogibeoryeo.presentation.settings.components.SettingsInfoRow
import com.team.yeogibeoryeo.presentation.settings.components.SettingsParagraph
import com.team.yeogibeoryeo.presentation.settings.components.SettingsSection

@Composable
internal fun SourcesDetail() {
    SettingsDetailContent {
        SettingsSection(
            title = stringResource(R.string.settings_sources_title),
            description = stringResource(R.string.settings_sources_data_description),
        )
        SettingsInfoRow(
            label = stringResource(R.string.settings_source_disposal_home_title),
            value = stringResource(R.string.settings_source_disposal_home_description),
        )
        SettingsInfoRow(
            label = stringResource(R.string.settings_source_recycling_api_title),
            value = stringResource(R.string.settings_source_recycling_api_description),
        )
        SettingsInfoRow(
            label = stringResource(R.string.settings_source_household_api_title),
            value = stringResource(R.string.settings_source_household_api_description),
        )
        SettingsInfoRow(
            label = stringResource(R.string.settings_source_admin_region_title),
            value = stringResource(R.string.settings_source_admin_region_description),
        )
        SettingsParagraph(text = stringResource(R.string.settings_sources_usage_condition_description))
        SettingsSection(
            title = stringResource(R.string.settings_open_source_title),
            description = stringResource(R.string.settings_tabler_usage_description),
        )
        SettingsInfoRow(
            label = stringResource(R.string.settings_tabler_icons_title),
            value = stringResource(R.string.settings_tabler_usage_title),
        )
        Text(
            text = stringResource(R.string.settings_tabler_license_text),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
