package com.team.yeogibeoryeo.presentation.common.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SearchBarField(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable (Boolean) -> Unit)? = null,
    candidateContent: (@Composable ColumnScope.() -> Unit)? = null,
    minHeight: Dp = SearchBarFieldDefaults.minHeight,
    searchEnabled: (String) -> Boolean = { it.isNotBlank() },
) {
    val hasCandidates = candidateContent != null
    val searchFieldInteractionSource = remember { MutableInteractionSource() }
    val isSearchFieldFocused by searchFieldInteractionSource.collectIsFocusedAsState()
    val isSearchEnabled = searchEnabled(keyword)
    val collapsedShape = MaterialTheme.shapes.medium
    val expandedShape = collapsedShape.copy(
        bottomStart = SearchBarFieldDefaults.connectedCornerSize,
        bottomEnd = SearchBarFieldDefaults.connectedCornerSize,
    )
    val candidateShape = collapsedShape.copy(
        topStart = SearchBarFieldDefaults.connectedCornerSize,
        topEnd = SearchBarFieldDefaults.connectedCornerSize,
    )

    fun submitSearch() {
        if (!isSearchEnabled) return

        onSearch(keyword)
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                        submitSearch()
                        true
                    } else {
                        false
                    }
                },
            value = keyword,
            onValueChange = onKeywordChange,
            singleLine = true,
            placeholder = {
                Text(text = placeholder)
            },
            leadingIcon = leadingContent?.let {
                { it() }
            },
            trailingIcon = trailingContent?.let {
                { it(isSearchFieldFocused) }
            },
            interactionSource = searchFieldInteractionSource,
            shape = if (hasCandidates) {
                expandedShape
            } else {
                collapsedShape
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.background,
                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    submitSearch()
                },
            ),
        )

        if (candidateContent != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = candidateShape,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    candidateContent()
                }
            }
        }
    }
}

internal object SearchBarFieldDefaults {
    val minHeight: Dp = 56.dp
    val connectedCornerSize = CornerSize(0.dp)
}
