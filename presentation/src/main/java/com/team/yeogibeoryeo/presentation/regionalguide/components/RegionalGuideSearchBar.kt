package com.team.yeogibeoryeo.presentation.regionalguide.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun RegionalGuideSearchBar(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = keyword,
            onValueChange = onKeywordChange,
            singleLine = true,
            label = {
                Text(text = "지역명")
            },
            placeholder = {
                Text(text = "예: 영등포구, 세종 새롬동")
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearchClick()
                }
            )
        )

        Button(
            onClick = onSearchClick,
            enabled = keyword.isNotBlank()
        ) {
            Text(text = "검색")
        }
    }
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
