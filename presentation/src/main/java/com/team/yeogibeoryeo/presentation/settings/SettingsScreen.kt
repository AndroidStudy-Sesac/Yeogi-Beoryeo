package com.team.yeogibeoryeo.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.settings.components.SettingsListItem
import com.team.yeogibeoryeo.presentation.settings.components.SettingsScaffold

@Composable
internal fun SettingsScreen(
    onBackClick: () -> Unit,
    onDetailClick: (SettingsDetailType) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsScaffold(
        title = stringResource(R.string.settings_title),
        onBackClick = onBackClick,
        modifier = modifier,
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                horizontal = SettingsLayoutDefaults.listHorizontalPadding,
                vertical = SettingsLayoutDefaults.listVerticalPadding,
            ),
        ) {
            items(SettingsMenuItems) { item ->
                SettingsListItem(
                    title = stringResource(item.titleResId),
                    onClick = { onDetailClick(item) },
                )
            }
        }
    }
}

private val SettingsMenuItems = listOf(
    SettingsDetailType.Notice,
    SettingsDetailType.Contact,
    SettingsDetailType.AppInfo,
    SettingsDetailType.LocationPermission,
    SettingsDetailType.Terms,
    SettingsDetailType.Sources,
    SettingsDetailType.Cache,
)
