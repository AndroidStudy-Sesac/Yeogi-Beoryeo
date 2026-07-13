package com.team.yeogibeoryeo.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.team.yeogibeoryeo.presentation.common.components.MessageSnackbar
import com.team.yeogibeoryeo.presentation.settings.components.SettingsScaffold
import com.team.yeogibeoryeo.presentation.settings.detail.AppInfoDetail
import com.team.yeogibeoryeo.presentation.settings.detail.CacheDetail
import com.team.yeogibeoryeo.presentation.settings.detail.ContactDetail
import com.team.yeogibeoryeo.presentation.settings.detail.LocationPermissionDetail
import com.team.yeogibeoryeo.presentation.settings.detail.NoticeDetail
import com.team.yeogibeoryeo.presentation.settings.detail.SourcesDetail
import com.team.yeogibeoryeo.presentation.settings.detail.TermsDetail
import kotlinx.coroutines.flow.SharedFlow

@Composable
internal fun SettingsDetailScreen(
    detailType: SettingsDetailType,
    appVersionName: String,
    onBackClick: () -> Unit,
    onOpenAppSettingsClick: () -> Unit,
    onClearLocationCacheClick: () -> Unit,
    cacheUiState: SettingsCacheUiState,
    cacheEvents: SharedFlow<SettingsCacheEvent>,
    modifier: Modifier = Modifier,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
) {
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentContext = rememberUpdatedState(LocalContext.current)

    LaunchedEffect(cacheEvents) {
        cacheEvents.collect { event ->
            when (event) {
                is SettingsCacheEvent.ShowLocationCacheMessage -> {
                    snackbarHostState.showSnackbar(currentContext.value.getString(event.messageResId))
                }
            }
        }
    }

    SettingsScaffold(
        title = stringResource(detailType.titleResId),
        onBackClick = onBackClick,
        modifier = modifier,
        listState = listState,
        onBottomBarVisibilityChanged = onBottomBarVisibilityChanged,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                MessageSnackbar(message = snackbarData.visuals.message)
            }
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(MaterialTheme.colorScheme.background),
            state = listState,
            contentPadding = PaddingValues(
                start = SettingsLayoutDefaults.detailHorizontalPadding,
                top = SettingsLayoutDefaults.detailTopPadding,
                end = SettingsLayoutDefaults.detailHorizontalPadding,
                bottom = SettingsLayoutDefaults.detailVerticalPadding,
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
                    CacheDetail(
                        onClearLocationCacheClick = onClearLocationCacheClick,
                        isClearingLocationCache = cacheUiState.isClearingLocationCache,
                    )
                }
            }
        }
    }
}
