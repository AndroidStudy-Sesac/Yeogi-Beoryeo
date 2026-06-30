package com.team.yeogibeoryeo.presentation.settings.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.settings.components.SettingsDetailContent
import com.team.yeogibeoryeo.presentation.settings.components.SettingsSection

@Composable
internal fun NoticeDetail() {
    SettingsDetailContent {
        SettingsSection(
            title = stringResource(R.string.settings_notice_empty_title),
            description = stringResource(R.string.settings_notice_empty_description),
        )
    }
}
