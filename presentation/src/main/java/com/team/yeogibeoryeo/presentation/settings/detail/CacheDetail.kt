package com.team.yeogibeoryeo.presentation.settings.detail

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.settings.components.SettingsDetailContent
import com.team.yeogibeoryeo.presentation.settings.components.SettingsSection

@Composable
internal fun CacheDetail(
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
