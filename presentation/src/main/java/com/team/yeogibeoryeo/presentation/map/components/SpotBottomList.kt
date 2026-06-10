package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    onSpotFavoriteClick: (CollectionSpot) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        start = 16.dp,
        top = 12.dp,
        end = 16.dp,
        bottom = 96.dp,
    ),
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selectedSpot?.id, spots) {
        val selectedIndex = spots.indexOfFirst { spot ->
            spot == selectedSpot
        }.takeIf { index ->
            index >= 0
        } ?: spots.indexOfFirst { spot ->
            spot.id == selectedSpot?.id
        }

        if (selectedIndex >= 0) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(
            items = spots,
            key = { index, spot -> "${spot.id}_$index" },
        ) { _, spot ->
            SpotBottomCard(
                spot = spot,
                isSelected = spot == selectedSpot,
                onClick = {
                    onSpotClick(spot)
                },
                onFavoriteClick = onSpotFavoriteClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpotBottomListPreview() {
    MaterialTheme {
        Surface {
            SpotBottomList(
                spots = listOf(
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
                ),
                selectedSpot = CollectionSpot(
                    id = "2",
                    name = "중소형 폐가전 수거함",
                    type = CollectionSpotType.SMALL_E_WASTE_BIN,
                    address = "서울특별시 구로구 구로동",
                    detailLocation = "아파트 관리사무소 옆",
                    coordinate = null,
                    distanceMeter = 350,
                    isBookmarked = false,
                ),
                onSpotClick = {},
                onSpotFavoriteClick = {},
            )
        }
    }
}
