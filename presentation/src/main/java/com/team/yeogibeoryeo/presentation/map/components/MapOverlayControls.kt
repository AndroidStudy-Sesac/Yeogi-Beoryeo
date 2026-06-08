package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.presentation.map.mapper.toDisplayName

@Composable
fun MapOverlayControls(
    keyword: String,
    selectedTypes: Set<CollectionSpotType>,
    onKeywordChanged: (String) -> Unit,
    onSearchClick: () -> Unit,
    onTypeClick: (CollectionSpotType) -> Unit,
    onCurrentLocationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MapOverlaySearchArea(
            keyword = keyword,
            onKeywordChanged = onKeywordChanged,
            onSearchClick = onSearchClick,
        )

        MapOverlayQuickControlArea(
            selectedTypes = selectedTypes,
            onTypeClick = onTypeClick,
            onCurrentLocationClick = onCurrentLocationClick,
        )
    }
}

@Composable
private fun MapOverlaySearchArea(
    keyword: String,
    onKeywordChanged: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MapSearchBar(
        keyword = keyword,
        onKeywordChanged = onKeywordChanged,
        onSearchClick = onSearchClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    )
}

@Composable
private fun MapOverlayQuickControlArea(
    selectedTypes: Set<CollectionSpotType>,
    onTypeClick: (CollectionSpotType) -> Unit,
    onCurrentLocationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        if (selectedTypes.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(end = CurrentLocationButtonReservedWidth),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                selectedTypes.forEach { type ->
                    FilterChip(
                        selected = true,
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

        CurrentLocationButton(
            onClick = onCurrentLocationClick,
            modifier = Modifier.align(Alignment.CenterEnd),
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
                selectedTypes = emptySet(),
                onKeywordChanged = {},
                onSearchClick = {},
                onTypeClick = {},
                onCurrentLocationClick = {},
            )
        }
    }
}

private val CurrentLocationButtonReservedWidth = 176.dp

@Preview(showBackground = true)
@Composable
private fun MapOverlayControlsSelectedPreview() {
    MaterialTheme {
        Surface {
            MapOverlayControls(
                keyword = "문래동",
                selectedTypes = setOf(
                    CollectionSpotType.BATTERY_BIN,
                    CollectionSpotType.SMALL_E_WASTE_BIN,
                ),
                onKeywordChanged = {},
                onSearchClick = {},
                onTypeClick = {},
                onCurrentLocationClick = {},
            )
        }
    }
}
