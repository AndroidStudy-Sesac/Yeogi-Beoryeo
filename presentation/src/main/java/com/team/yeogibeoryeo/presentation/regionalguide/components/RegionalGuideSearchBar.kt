package com.team.yeogibeoryeo.presentation.regionalguide.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RegionalGuideSearchBar(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    candidateContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val hasCandidates = candidateContent != null

    fun submitSearch() {
        if (keyword.isBlank()) return

        focusManager.clearFocus()
        keyboardController?.hide()
        onSearchClick()
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SEARCH_FIELD_MIN_HEIGHT),
            value = keyword,
            onValueChange = onKeywordChange,
            singleLine = true,
            placeholder = {
                Text(text = "지역명 또는 주소를 검색해주세요.")
            },
            trailingIcon = {
                IconButton(
                    onClick = ::submitSearch,
                    enabled = keyword.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "검색"
                    )
                }
            },
            shape = if (hasCandidates) {
                SEARCH_FIELD_EXPANDED_SHAPE
            } else {
                SEARCH_FIELD_DEFAULT_SHAPE
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    submitSearch()
                }
            )
        )

        if (candidateContent != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = SEARCH_CANDIDATE_CONTAINER_SHAPE,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = SEARCH_CANDIDATE_ELEVATION,
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

private val SEARCH_FIELD_MIN_HEIGHT = 56.dp
private val SEARCH_FIELD_DEFAULT_SHAPE = RoundedCornerShape(12.dp)
private val SEARCH_FIELD_EXPANDED_SHAPE = RoundedCornerShape(
    topStart = 12.dp,
    topEnd = 12.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp,
)
private val SEARCH_CANDIDATE_CONTAINER_SHAPE = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 0.dp,
    bottomStart = 12.dp,
    bottomEnd = 12.dp,
)
private val SEARCH_CANDIDATE_ELEVATION = 3.dp

@Preview(showBackground = true)
@Composable
private fun RegionalGuideSearchBarPreview() {
    MaterialTheme {
        RegionalGuideSearchBar(
            keyword = "영등포구",
            onKeywordChange = {},
            onSearchClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
