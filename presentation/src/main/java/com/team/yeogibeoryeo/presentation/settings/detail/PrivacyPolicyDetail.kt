package com.team.yeogibeoryeo.presentation.settings.detail

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.settings.components.SettingsDetailContent
import com.team.yeogibeoryeo.presentation.settings.components.SettingsSection

@Composable
internal fun PrivacyPolicyDetail(
    onOpenPrivacyPolicyClick: () -> Unit,
) {
    SettingsDetailContent {
        SettingsSection(
            title = stringResource(R.string.settings_privacy_policy_title),
            description = stringResource(R.string.settings_privacy_policy_description),
        )
        Button(onClick = onOpenPrivacyPolicyClick) {
            Text(text = stringResource(R.string.settings_open_privacy_policy))
        }
    }
}
