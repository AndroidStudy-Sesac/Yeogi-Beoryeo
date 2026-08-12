package com.team.yeogibeoryeo.presentation.settings

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsRoute(
    onBackClick: () -> Unit,
    onDetailClick: (SettingsDetailType) -> Unit,
    onAppGuideClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsScreen(
        onBackClick = onBackClick,
        onDetailClick = onDetailClick,
        onAppGuideClick = onAppGuideClick,
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
    onOpenNaverMapLegalNoticeClick: () -> Unit,
    onOpenNaverMapOpenSourceLicenseClick: () -> Unit,
    onOpenSourceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
    cacheViewModel: SettingsCacheViewModel = hiltViewModel(),
) {
    val cacheUiState by cacheViewModel.uiState.collectAsStateWithLifecycle()
    val noticeRouteState = if (detailType == SettingsDetailType.Notice) {
        val noticeViewModel: SettingsNoticeViewModel = hiltViewModel()
        val noticeUiState by noticeViewModel.uiState.collectAsStateWithLifecycle()
        SettingsNoticeRouteState(
            uiState = noticeUiState,
            onNoticeClick = noticeViewModel::selectNotice,
            onRetryClick = noticeViewModel::retryLoad,
            onClearSelection = noticeViewModel::clearNoticeSelection,
        )
    } else {
        null
    }
    val hasSelectedNotice =
        (noticeRouteState?.uiState as? SettingsNoticeUiState.Content)?.selectedNotice != null
    val detailBackClick = if (hasSelectedNotice) {
        {
            noticeRouteState?.onClearSelection?.invoke()
            Unit
        }
    } else {
        onBackClick
    }

    BackHandler(enabled = hasSelectedNotice) {
        noticeRouteState?.onClearSelection?.invoke()
    }

    SettingsDetailScreen(
        detailType = detailType,
        appVersionName = appVersionName,
        onBackClick = detailBackClick,
        onOpenAppSettingsClick = onOpenAppSettingsClick,
        onOpenPrivacyPolicyClick = onOpenPrivacyPolicyClick,
        onOpenNaverMapLegalNoticeClick = onOpenNaverMapLegalNoticeClick,
        onOpenNaverMapOpenSourceLicenseClick = onOpenNaverMapOpenSourceLicenseClick,
        onOpenSourceClick = onOpenSourceClick,
        onClearLocationCacheClick = cacheViewModel::clearLocationCache,
        cacheUiState = cacheUiState,
        cacheEvents = cacheViewModel.events,
        noticeUiState = noticeRouteState?.uiState,
        onNoticeClick = noticeRouteState?.onNoticeClick ?: {},
        onNoticeRetryClick = noticeRouteState?.onRetryClick ?: {},
        modifier = modifier,
        onBottomBarVisibilityChanged = onBottomBarVisibilityChanged,
    )
}

private data class SettingsNoticeRouteState(
    val uiState: SettingsNoticeUiState,
    val onNoticeClick: (String) -> Unit,
    val onRetryClick: () -> Unit,
    val onClearSelection: () -> Unit,
)
