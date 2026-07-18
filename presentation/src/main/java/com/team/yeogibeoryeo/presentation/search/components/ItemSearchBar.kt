package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.team.yeogibeoryeo.common.R as CommonR
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.components.SearchBarField
import com.team.yeogibeoryeo.presentation.search.ItemSearchLayoutDefaults

@Composable
fun ItemSearchBar(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String,
    iconSize: Dp = ItemSearchLayoutDefaults.size.iconSmall,
) {
    val size = ItemSearchLayoutDefaults.size

    SearchBarField(
        modifier = modifier.fillMaxWidth(),
        keyword = keyword,
        onKeywordChange = onKeywordChange,
        onSearch = {
            if (keyword.isNotBlank()) {
                onSearchClick()
            }
        },
        placeholder = placeholder,
        trailingContent = { isFocused ->
            val searchIconColor = when {
                keyword.isNotBlank() || isFocused -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            IconButton(
                onClick = {
                    if (keyword.isNotBlank()) {
                        onSearchClick()
                    }
                },
                enabled = keyword.isNotBlank(),
            ) {
                Icon(
                    painter = painterResource(id = CommonR.drawable.ic_action_search),
                    contentDescription = stringResource(R.string.search_action),
                    modifier = Modifier.size(iconSize),
                    tint = searchIconColor,
                )
            }
        },
        minHeight = size.searchFieldHeight,
        shadowElevation = ItemSearchLayoutDefaults.elevation.searchField,
        searchEnabled = { it.isNotBlank() },
    )
}

@Preview(showBackground = true)
@Composable
private fun ItemSearchBarPreview() {
    ItemSearchBar(
        keyword = "비닐",
        onKeywordChange = {},
        onSearchClick = {},
        placeholder = "품목 검색",
        modifier = Modifier.fillMaxWidth(),
    )
}
