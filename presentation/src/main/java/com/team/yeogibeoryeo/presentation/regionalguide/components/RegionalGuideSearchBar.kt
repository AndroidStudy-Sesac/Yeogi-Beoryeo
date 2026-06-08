package com.team.yeogibeoryeo.presentation.regionalguide.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.common.R as CommonR
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.components.SearchBarField

@Composable
fun RegionalGuideSearchBar(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onSearchClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    candidateContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    SearchBarField(
        modifier = modifier.fillMaxWidth(),
        keyword = keyword,
        onKeywordChange = onKeywordChange,
        onSearch = { onSearchClick(it) },
        placeholder = "지역명 또는 주소를 검색해주세요.",
        trailingContent = {
            if (keyword.isNotBlank()) {
                IconButton(onClick = { onKeywordChange("") }) {
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
                        onSearchClick(keyword)
                    }
                },
                enabled = keyword.isNotBlank(),
            ) {
                Icon(
                    painter = painterResource(id = CommonR.drawable.ic_action_search),
                    contentDescription = stringResource(R.string.search_action),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        candidateContent = candidateContent,
        minHeight = 56.dp,
    )
}

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
