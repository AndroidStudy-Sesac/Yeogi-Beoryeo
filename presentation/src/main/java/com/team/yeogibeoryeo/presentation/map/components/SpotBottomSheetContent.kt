package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType

@Composable
fun SpotBottomSheetContent(
    spots: List<CollectionSpot>,
    selectedSpot: CollectionSpot?,
    isLoading: Boolean,
    hasSearched: Boolean,
    selectedTypes: Set<CollectionSpotType>,
    locationNoticeMessage: String?,
    errorMessage: String?,
    onTypeClick: (CollectionSpotType) -> Unit,
    onSpotClick: (CollectionSpot) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        SpotBottomSheetHeader(
            resultCount = spots.size,
            hasSearched = hasSearched,
        )

        if (hasSearched && !isLoading) {
            SpotFilterChipRow(
                selectedTypes = selectedTypes,
                onTypeClick = onTypeClick,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        when {
            isLoading -> {
                SpotBottomSheetLoading()
            }

            locationNoticeMessage != null -> {
                EmptySpotResult(
                    title = "현재 위치 검색 안내",
                    description = locationNoticeMessage,
                )
            }

            errorMessage != null -> {
                EmptySpotResult(
                    title = "수거 장소를 불러오지 못했습니다.",
                    description = errorMessage,
                )
            }

            hasSearched && spots.isEmpty() -> {
                EmptySpotResult()
            }

            spots.isNotEmpty() -> {
                SpotBottomList(
                    spots = spots,
                    selectedSpot = selectedSpot,
                    onSpotClick = onSpotClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = SpotBottomSheetListMaxHeight),
                )
            }
        }
    }
}

@Composable
fun SpotBottomSheetHeader(
    resultCount: Int,
    hasSearched: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = if (hasSearched) {
                "검색 결과 ${resultCount}개"
            } else {
                "수거 장소 검색"
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun SpotBottomSheetLoading(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun SpotDetailBottomSheetContent(
    spot: CollectionSpot,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 8.dp),
    ) {
        SpotBottomCard(
            spot = spot,
            isSelected = true,
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "목록으로",
            modifier = Modifier
                .align(Alignment.End)
                .clickable(onClick = onCloseClick)
                .padding(top = 12.dp, bottom = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SpotBottomSheetContentPreview() {
    MaterialTheme {
        Surface {
            SpotBottomSheetContent(
                spots = sampleSpotBottomSheetSpots(),
                selectedSpot = sampleSpotBottomSheetSpots()[1],
                isLoading = false,
                hasSearched = true,
                selectedTypes = setOf(CollectionSpotType.BATTERY_BIN),
                locationNoticeMessage = null,
                errorMessage = null,
                onTypeClick = {},
                onSpotClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpotBottomSheetContentLoadingPreview() {
    MaterialTheme {
        Surface {
            SpotBottomSheetContent(
                spots = emptyList(),
                selectedSpot = null,
                isLoading = true,
                hasSearched = true,
                selectedTypes = emptySet(),
                locationNoticeMessage = null,
                errorMessage = null,
                onTypeClick = {},
                onSpotClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpotBottomSheetContentEmptyPreview() {
    MaterialTheme {
        Surface {
            SpotBottomSheetContent(
                spots = emptyList(),
                selectedSpot = null,
                isLoading = false,
                hasSearched = true,
                selectedTypes = emptySet(),
                locationNoticeMessage = null,
                errorMessage = null,
                onTypeClick = {},
                onSpotClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpotDetailBottomSheetContentPreview() {
    MaterialTheme {
        Surface {
            SpotDetailBottomSheetContent(
                spot = sampleSpotBottomSheetSpots()[0],
                onCloseClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpotBottomSheetHeaderPreview() {
    MaterialTheme {
        Surface {
            SpotBottomSheetHeader(
                resultCount = 3,
                hasSearched = true,
            )
        }
    }
}

private fun sampleSpotBottomSheetSpots(): List<CollectionSpot> {
    return listOf(
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
}

private val SpotBottomSheetListMaxHeight = 560.dp
