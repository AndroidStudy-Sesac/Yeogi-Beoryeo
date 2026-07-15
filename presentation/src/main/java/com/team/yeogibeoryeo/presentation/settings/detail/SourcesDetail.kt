package com.team.yeogibeoryeo.presentation.settings.detail

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.settings.components.SettingsDetailContent
import com.team.yeogibeoryeo.presentation.settings.components.SettingsInfoRow
import com.team.yeogibeoryeo.presentation.settings.components.SettingsSection

@Composable
internal fun SourcesDetail(
    onOpenNaverMapLegalNoticeClick: () -> Unit,
    onOpenNaverMapOpenSourceLicenseClick: () -> Unit,
) {
    SettingsDetailContent {
        SettingsSection(
            title = stringResource(R.string.settings_sources_data_title),
            description = stringResource(R.string.settings_sources_data_description),
        )
        SettingsInfoRow(
            label = stringResource(R.string.settings_source_climate_ministry_title),
            value = stringResource(R.string.settings_source_climate_ministry_description),
        )
        SettingsInfoRow(
            label = stringResource(R.string.settings_source_interior_ministry_title),
            value = stringResource(R.string.settings_source_interior_ministry_description),
        )
        SettingsSection(
            title = stringResource(R.string.settings_sources_usage_title),
            description = stringResource(R.string.settings_sources_usage_description),
        )
        SettingsSection(
            title = stringResource(R.string.settings_naver_map_title),
            description = stringResource(R.string.settings_naver_map_usage_description),
        )
        Button(onClick = onOpenNaverMapLegalNoticeClick) {
            Text(text = stringResource(R.string.settings_naver_map_legal_notice_action))
        }
        Button(onClick = onOpenNaverMapOpenSourceLicenseClick) {
            Text(text = stringResource(R.string.settings_naver_map_open_source_license_action))
        }
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
        SettingsSection(
            title = stringResource(R.string.settings_pretendard_title),
            description = stringResource(R.string.settings_pretendard_usage_description),
        )
        SettingsInfoRow(
            label = stringResource(R.string.settings_pretendard_title),
            value = stringResource(R.string.settings_pretendard_license_title),
        )
        Text(
            text = stringResource(R.string.settings_pretendard_license_text),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
