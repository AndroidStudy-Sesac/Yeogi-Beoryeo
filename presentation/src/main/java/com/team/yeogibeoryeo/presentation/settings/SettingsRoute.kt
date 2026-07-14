package com.team.yeogibeoryeo.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsRoute(
    onBackClick: () -> Unit,
    onDetailClick: (SettingsDetailType) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsScreen(
        onBackClick = onBackClick,
        onDetailClick = onDetailClick,
        modifier = modifier,
    )
}

@Composable
fun SettingsDetailRoute(
    detailType: SettingsDetailType,
    appVersionName: String,
    onBackClick: () -> Unit,
    onOpenAppSettingsClick: () -> Unit,
    onOpenPrivacyPolicyClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
    cacheViewModel: SettingsCacheViewModel = hiltViewModel(),
) {
    val cacheUiState by cacheViewModel.uiState.collectAsStateWithLifecycle()

    SettingsDetailScreen(
        detailType = detailType,
        appVersionName = appVersionName,
        onBackClick = onBackClick,
        onOpenAppSettingsClick = onOpenAppSettingsClick,
        onOpenPrivacyPolicyClick = onOpenPrivacyPolicyClick,
        onClearLocationCacheClick = cacheViewModel::clearLocationCache,
        cacheUiState = cacheUiState,
        cacheEvents = cacheViewModel.events,
        modifier = modifier,
        onBottomBarVisibilityChanged = onBottomBarVisibilityChanged,
    )
}
