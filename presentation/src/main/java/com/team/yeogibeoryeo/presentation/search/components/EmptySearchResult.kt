package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun EmptySearchResult(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    ItemSearchStatusContent(
        title = {
            ItemSearchStatusTitle(text = title)
        },
        description = {
            ItemSearchStatusDescription(text = description)
        },
        modifier = modifier,
    )
}
