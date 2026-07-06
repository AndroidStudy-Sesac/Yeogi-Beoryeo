package com.team.yeogibeoryeo.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
    onClearLocationCacheClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
) {
    SettingsDetailScreen(
        detailType = detailType,
        appVersionName = appVersionName,
        onBackClick = onBackClick,
        onOpenAppSettingsClick = onOpenAppSettingsClick,
        onClearLocationCacheClick = onClearLocationCacheClick,
        modifier = modifier,
        onBottomBarVisibilityChanged = onBottomBarVisibilityChanged,
    )
}
