package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.team.yeogibeoryeo.common.R as CommonR
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.components.SearchBarField

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MapSearchBar(
    keyword: String,
    onKeywordChanged: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun submitSearch() {
        focusManager.clearFocus()
        keyboardController?.hide()
        onSearchClick()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        SearchBarField(
            modifier = Modifier.fillMaxWidth(),
            keyword = keyword,
            onKeywordChange = onKeywordChanged,
            onSearch = {
                submitSearch()
            },
            placeholder = "동네 또는 주소를 검색해주세요.",
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (keyword.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            },
            trailingContent = {
                if (keyword.isNotBlank()) {
                    IconButton(onClick = { onKeywordChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "검색어 지우기",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                IconButton(
                    onClick = {
                        if (keyword.isNotBlank()) {
                            submitSearch()
                        }
                    },
                    enabled = keyword.isNotBlank(),
                ) {
                    Icon(
                        painter = painterResource(id = CommonR.drawable.ic_action_search),
                        contentDescription = stringResource(R.string.search_action),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            minHeight = 56.dp,
            searchEnabled = { it.isNotBlank() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MapSearchBarPreview() {
    MaterialTheme {
        Surface {
            MapSearchBar(
                keyword = "",
                onKeywordChanged = {},
                onSearchClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MapSearchBarWithKeywordPreview() {
    MaterialTheme {
        Surface {
            MapSearchBar(
                keyword = "용답동",
                onKeywordChanged = {},
                onSearchClick = {},
            )
        }
    }
}
