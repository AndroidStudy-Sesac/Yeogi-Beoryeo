package com.team.yeogibeoryeo.presentation.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.team.yeogibeoryeo.presentation.settings.SettingsLayoutDefaults

@Composable
internal fun SettingsDetailContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SettingsLayoutDefaults.detailContentSpacing),
    ) {
        content()
    }
}
