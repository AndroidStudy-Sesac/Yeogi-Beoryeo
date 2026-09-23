package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@Composable
fun EmptySearchResult(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: () -> Unit = {},
    suggestedQueries: List<String> = emptyList(),
    onSuggestionClick: (String) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    ItemSearchStatusContent(
        title = {
            ItemSearchStatusTitle(text = title)
        },
        description = {
            ItemSearchStatusDescription(text = description)
        },
        modifier = modifier,
        action = if (actionLabel != null || suggestedQueries.isNotEmpty()) {
            {
                suggestedQueries.forEach { query ->
                    OutlinedButton(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            onSuggestionClick(query)
                        },
                    ) {
                        Text(text = query)
                    }
                }
                if (actionLabel != null) {
                    Button(onClick = onActionClick) {
                        Text(text = actionLabel)
                    }
                }
            }
        } else null,
    )
}
