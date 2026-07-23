package com.team.yeogibeoryeo.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.R

@Composable
fun AppTopBar(
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    title: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .heightIn(min = AppTopBarDefaults.height)
            .padding(horizontal = AppTopBarDefaults.horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(AppTopBarDefaults.buttonSize),
            contentAlignment = Alignment.Center,
        ) {
            navigationIcon()
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = AppTopBarDefaults.titleStartPadding),
            contentAlignment = Alignment.CenterStart,
        ) {
            title()
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

@Composable
fun AppBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.fillMaxSize(),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back_action),
            modifier = Modifier.size(AppTopBarDefaults.iconSize),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

internal object AppTopBarDefaults {
    val height = 64.dp
    val horizontalPadding = 12.dp
    val buttonSize = 48.dp
    val iconSize = 24.dp
    val titleStartPadding = 8.dp
}
