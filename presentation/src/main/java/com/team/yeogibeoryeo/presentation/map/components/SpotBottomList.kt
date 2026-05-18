package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType

@Composable
fun SpotBottomList(
    spots: List<CollectionSpot>,
    selectedSpot: CollectionSpot?,
    onSpotClick: (CollectionSpot) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = spots,
            key = { spot -> spot.id },
        ) { spot ->
            SpotBottomCard(
                spot = spot,
                isSelected = spot.id == selectedSpot?.id,
                onClick = {
                    onSpotClick(spot)
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpotBottomListPreview() {
    val previewSpots = listOf(
        CollectionSpot(
            id = "1",
            name = "폐건전지 수거함",
            type = CollectionSpotType.BATTERY_BIN,
            address = "서울특별시 영등포구 문래동",
            detailLocation = "주민센터 앞",
            coordinate = null,
            distanceMeter = 120,
            isBookmarked = false,
        ),
        CollectionSpot(
            id = "2",
            name = "중소형 폐가전 수거함",
            type = CollectionSpotType.SMALL_E_WASTE_BIN,
            address = "서울특별시 구로구 구로동",
            detailLocation = "아파트 관리사무소 옆",
            coordinate = null,
            distanceMeter = 350,
            isBookmarked = false,
        ),
        CollectionSpot(
            id = "3",
            name = "재활용센터",
            type = CollectionSpotType.RECYCLING_CENTER,
            address = "서울특별시 성동구 용답동",
            detailLocation = null,
            coordinate = null,
            distanceMeter = null,
            isBookmarked = false,
        ),
    )

    MaterialTheme {
        Surface {
            SpotBottomList(
                spots = previewSpots,
                selectedSpot = previewSpots[1],
                onSpotClick = {},
            )
        }
    }
}