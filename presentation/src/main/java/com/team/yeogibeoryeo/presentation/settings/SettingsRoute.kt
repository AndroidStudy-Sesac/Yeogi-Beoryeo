package com.team.yeogibeoryeo.presentation.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

@Composable
fun SettingsRoute(
    onBackClick: () -> Unit,
    onDetailClick: (SettingsDetailType) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsScaffold(
        title = stringResource(R.string.settings_title),
        onBackClick = onBackClick,
        modifier = modifier,
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                horizontal = SettingsLayoutDefaults.listHorizontalPadding,
                vertical = SettingsLayoutDefaults.listVerticalPadding,
            ),
        ) {
            items(SettingsMenuItems) { item ->
                SettingsListItem(
                    title = stringResource(item.titleResId),
                    onClick = { onDetailClick(item) },
                )
            }
        }
    }
}

@Composable
fun SettingsDetailRoute(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScaffold(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_action),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        content = content,
    )
}

@Composable
private fun SettingsListItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SettingsLayoutDefaults.listItemVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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

@Composable
private fun SettingsDetailContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SettingsLayoutDefaults.detailContentSpacing),
    ) {
        content()
    }
}

@Composable
private fun SettingsSection(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SettingsLayoutDefaults.sectionSpacing),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SettingsLayoutDefaults.infoRowSpacing),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsParagraph(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private val SettingsMenuItems = listOf(
    SettingsDetailType.Notice,
    SettingsDetailType.Contact,
    SettingsDetailType.AppInfo,
    SettingsDetailType.LocationPermission,
    SettingsDetailType.Terms,
    SettingsDetailType.Sources,
    SettingsDetailType.Cache,
)

private object SettingsLayoutDefaults {
    val listHorizontalPadding: Dp = 24.dp
    val listVerticalPadding: Dp = 16.dp
    val listItemVerticalPadding: Dp = 24.dp
    val detailHorizontalPadding: Dp = 24.dp
    val detailVerticalPadding: Dp = 24.dp
    val detailItemSpacing: Dp = 24.dp
    val detailContentSpacing: Dp = 20.dp
    val sectionSpacing: Dp = 8.dp
    val infoRowSpacing: Dp = 8.dp
}
