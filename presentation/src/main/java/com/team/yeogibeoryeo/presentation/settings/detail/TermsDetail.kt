package com.team.yeogibeoryeo.presentation.settings.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.settings.components.SettingsDetailContent
import com.team.yeogibeoryeo.presentation.settings.components.SettingsParagraph
import com.team.yeogibeoryeo.presentation.settings.components.SettingsSection

@Composable
internal fun TermsDetail() {
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
