package com.team.yeogibeoryeo.presentation.settings.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.settings.components.SettingsDetailContent
import com.team.yeogibeoryeo.presentation.settings.components.SettingsInfoRow

@Composable
internal fun ContactDetail() {
    SettingsDetailContent {
        SettingsInfoRow(
            label = stringResource(R.string.settings_contact_email_label),
            value = stringResource(R.string.settings_contact_email),
        )
    }
}
