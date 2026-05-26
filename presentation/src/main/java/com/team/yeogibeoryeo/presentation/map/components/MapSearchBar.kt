package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun MapSearchBar(
    keyword: String,
    onKeywordChanged: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        OutlinedTextField(
            value = keyword,
            onValueChange = onKeywordChanged,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            placeholder = {
                Text(text = "동네 또는 주소를 검색해주세요.")
            },
            singleLine = true,
            trailingIcon = {
                IconButton(
                    onClick = onSearchClick,
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "검색",
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearchClick()
                },
            ),
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