package com.team.yeogibeoryeo.presentation.settings.detail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.ADMINISTRATIVE_CODE_URL
import com.team.yeogibeoryeo.presentation.common.DISPOSAL_API_URL
import com.team.yeogibeoryeo.presentation.common.DISPOSAL_PORTAL_URL
import com.team.yeogibeoryeo.presentation.common.REGIONAL_WASTE_API_URL
import com.team.yeogibeoryeo.presentation.settings.components.SettingsDetailContent
import com.team.yeogibeoryeo.presentation.settings.components.SettingsInfoRow
import com.team.yeogibeoryeo.presentation.settings.components.SettingsSection
import com.team.yeogibeoryeo.common.R as CommonR

@Composable
internal fun SourcesDetail(
    onOpenNaverMapLegalNoticeClick: () -> Unit,
    onOpenNaverMapOpenSourceLicenseClick: () -> Unit,
    onOpenSourceClick: (String) -> Unit,
) {
    SettingsDetailContent {
        SettingsSection(
            title = stringResource(R.string.settings_sources_data_title),
            description = stringResource(R.string.settings_sources_data_description),
        )
        SettingsSection(
            title = stringResource(R.string.settings_sources_non_affiliation_title),
            description = stringResource(R.string.settings_sources_non_affiliation_description),
        )
        SettingsInfoRow(
            label = stringResource(R.string.settings_source_climate_ministry_title),
            value = stringResource(R.string.settings_source_climate_ministry_description),
        )
        SourceLinkButton(
            label = stringResource(R.string.settings_source_disposal_portal_link),
            onClick = { onOpenSourceClick(DISPOSAL_PORTAL_URL) },
        )
        SourceLinkButton(
            label = stringResource(R.string.settings_source_disposal_api_link),
            onClick = { onOpenSourceClick(DISPOSAL_API_URL) },
        )
        SettingsInfoRow(
            label = stringResource(R.string.settings_source_interior_ministry_title),
            value = stringResource(R.string.settings_source_interior_ministry_description),
        )
        SourceLinkButton(
            label = stringResource(R.string.settings_source_regional_waste_api_link),
            onClick = { onOpenSourceClick(REGIONAL_WASTE_API_URL) },
        )
        SourceLinkButton(
            label = stringResource(R.string.settings_source_administrative_code_link),
            onClick = { onOpenSourceClick(ADMINISTRATIVE_CODE_URL) },
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

@Composable
private fun SourceLinkButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(CommonR.drawable.ic_action_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(SourceLinkIconSize),
        )
    }
}

private val SourceLinkIconSize = 20.dp
