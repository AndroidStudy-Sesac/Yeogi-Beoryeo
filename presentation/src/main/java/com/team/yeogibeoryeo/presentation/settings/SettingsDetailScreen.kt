package com.team.yeogibeoryeo.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.settings.components.SettingsDetailContent
import com.team.yeogibeoryeo.presentation.settings.components.SettingsInfoRow
import com.team.yeogibeoryeo.presentation.settings.components.SettingsParagraph
import com.team.yeogibeoryeo.presentation.settings.components.SettingsScaffold
import com.team.yeogibeoryeo.presentation.settings.components.SettingsSection

@Composable
internal fun SettingsDetailScreen(
    detailType: SettingsDetailType,
    appVersionName: String,
    onBackClick: () -> Unit,
    onOpenAppSettingsClick: () -> Unit,
    onClearLocationCacheClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsScaffold(
        title = stringResource(detailType.titleResId),
        onBackClick = onBackClick,
        modifier = modifier,
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                horizontal = SettingsLayoutDefaults.detailHorizontalPadding,
                vertical = SettingsLayoutDefaults.detailVerticalPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(SettingsLayoutDefaults.detailItemSpacing),
        ) {
            when (detailType) {
                SettingsDetailType.Notice -> item { NoticeDetail() }
                SettingsDetailType.Contact -> item { ContactDetail() }
                SettingsDetailType.AppInfo -> item {
                    AppInfoDetail(appVersionName = appVersionName)
                }
                SettingsDetailType.LocationPermission -> item {
                    LocationPermissionDetail(onOpenAppSettingsClick = onOpenAppSettingsClick)
                }
                SettingsDetailType.Terms -> item { TermsDetail() }
                SettingsDetailType.Sources -> item { SourcesDetail() }
                SettingsDetailType.Cache -> item {
                    CacheDetail(onClearLocationCacheClick = onClearLocationCacheClick)
                }
            }
        }
    }
}

@Composable
private fun NoticeDetail() {
    SettingsDetailContent {
        SettingsSection(
            title = stringResource(R.string.settings_notice_empty_title),
            description = stringResource(R.string.settings_notice_empty_description),
        )
    }
}

@Composable
private fun ContactDetail() {
    SettingsDetailContent {
        Text(
            text = stringResource(R.string.settings_contact_email_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AppInfoDetail(
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

@Composable
private fun LocationPermissionDetail(
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

@Composable
private fun TermsDetail() {
    SettingsDetailContent {
        SettingsSection(
            title = stringResource(R.string.settings_terms_title),
            description = stringResource(R.string.settings_terms_service_description),
        )
        SettingsParagraph(text = stringResource(R.string.settings_terms_info))
        SettingsParagraph(text = stringResource(R.string.settings_terms_location))
        SettingsParagraph(text = stringResource(R.string.settings_terms_responsibility))
        SettingsParagraph(text = stringResource(R.string.settings_terms_privacy))
    }
}

@Composable
private fun SourcesDetail() {
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

@Composable
private fun CacheDetail(
    onClearLocationCacheClick: () -> Unit,
) {
    SettingsDetailContent {
        SettingsSection(
            title = stringResource(R.string.settings_cache_detail_title),
            description = stringResource(R.string.settings_cache_confirm_description),
        )
        Button(onClick = onClearLocationCacheClick) {
            Text(text = stringResource(R.string.settings_cache_delete_action))
        }
    }
}
