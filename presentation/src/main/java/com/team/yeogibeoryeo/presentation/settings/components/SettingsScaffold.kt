package com.team.yeogibeoryeo.presentation.settings.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.team.yeogibeoryeo.presentation.common.components.AppBackButton
import com.team.yeogibeoryeo.presentation.common.components.AppTopBar
import com.team.yeogibeoryeo.presentation.common.effects.BottomBarVisibilityOnScrollEffect

@Composable
internal fun SettingsScaffold(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState? = null,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    if (listState != null) {
        BottomBarVisibilityOnScrollEffect(
            listState = listState,
            onBottomBarVisibilityChanged = onBottomBarVisibilityChanged,
        )
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                navigationIcon = {
                    AppBackButton(onClick = onBackClick)
                },
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
        content = content,
    )
}
