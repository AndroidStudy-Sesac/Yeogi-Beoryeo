package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.presentation.map.mapper.toDisplayName

@Composable
fun SpotFilterChipRow(
    types: List<CollectionSpotType>,
    selectedTypes: Set<CollectionSpotType>,
    onTypeClick: (CollectionSpotType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        types.forEach { type ->
            FilterChip(
                selected = type in selectedTypes,
                onClick = {
                    onTypeClick(type)
                },
                label = {
                    Text(text = type.toDisplayName())
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpotFilterChipRowPreview() {
    MaterialTheme {
        Surface {
            SpotFilterChipRow(
                types = MapSpotFilterChipPolicy.visibleTypes,
                selectedTypes = emptySet(),
                onTypeClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpotFilterChipRowSelectedPreview() {
    MaterialTheme {
        Surface {
            SpotFilterChipRow(
                types = MapSpotFilterChipPolicy.visibleTypes,
                selectedTypes = setOf(
                    CollectionSpotType.BATTERY_BIN,
                    CollectionSpotType.RECYCLING_CENTER,
                ),
                onTypeClick = {},
            )
        }
    }
}
