package com.team.yeogibeoryeo.presentation.settings.detail

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.settings.components.SettingsDetailContent

@Composable
internal fun ContactDetail() {
    SettingsDetailContent {
        Text(
            text = stringResource(R.string.settings_contact_email_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
