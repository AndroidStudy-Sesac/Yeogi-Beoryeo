package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun EmptySearchResult(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: () -> Unit = {},
) {
    ItemSearchStatusContent(
        title = {
            ItemSearchStatusTitle(text = title)
        },
        description = {
            ItemSearchStatusDescription(text = description)
        },
        modifier = modifier,
        action = actionLabel?.let { label ->
            {
                Button(onClick = onActionClick) {
                    Text(text = label)
                }
            }
        },
    )
}
