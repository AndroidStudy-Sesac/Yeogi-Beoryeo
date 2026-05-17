package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.presentation.map.mapper.toDisplayName

@Composable
fun SpotBottomCard(
    spot: CollectionSpot,
    onClick: (CollectionSpot) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onClick(spot)
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row {
                Text(
                    text = spot.type.toDisplayName(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                spot.distanceMeter?.let { distanceMeter ->
                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${distanceMeter}m",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = spot.name,
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = spot.address,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            spot.detailLocation?.let { detailLocation ->
                Text(
                    text = detailLocation,
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpotBottomCardPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.padding(16.dp),
        ) {
            SpotBottomCard(
                spot = CollectionSpot(
                    id = "1",
                    name = "폐건전지 수거함",
                    type = CollectionSpotType.BATTERY_BIN,
                    address = "서울특별시 영등포구 문래동",
                    detailLocation = "주민센터 앞",
                    coordinate = null,
                    distanceMeter = 120,
                    isBookmarked = false,
                ),
                onClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpotBottomCardWithoutDetailPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.padding(16.dp),
        ) {
            SpotBottomCard(
                spot = CollectionSpot(
                    id = "2",
                    name = "재활용센터",
                    type = CollectionSpotType.RECYCLING_CENTER,
                    address = "서울특별시 구로구 구로동",
                    detailLocation = null,
                    coordinate = null,
                    distanceMeter = null,
                    isBookmarked = false,
                ),
                onClick = {},
            )
        }
    }
}