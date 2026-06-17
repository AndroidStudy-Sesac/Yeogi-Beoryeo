package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun MapOverlayControls(
    keyword: String,
    onKeywordChanged: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MapSearchBar(
            keyword = keyword,
            onKeywordChanged = onKeywordChanged,
            onSearchClick = onSearchClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(12.dp),
                    clip = false,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MapOverlayControlsPreview() {
    MaterialTheme {
        Surface {
            MapOverlayControls(
                keyword = "",
                onKeywordChanged = {},
                onSearchClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MapOverlayControlsKeywordPreview() {
    MaterialTheme {
        Surface {
            MapOverlayControls(
                keyword = "문래동",
                onKeywordChanged = {},
                onSearchClick = {},
            )
        }
    }
}
