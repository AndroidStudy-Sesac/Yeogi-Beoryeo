package com.team.yeogibeoryeo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.Marker
import com.naver.maps.map.compose.MarkerState
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun PublicWasteSpotMapScreen() {
    val context = LocalContext.current

    var inputAddr by remember {
        mutableStateOf("용답동")
    }

    var searchAddr by remember {
        mutableStateOf("용답동")
    }

    var spots by remember {
        mutableStateOf<List<WasteSpot>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    fun requestSearch() {
        val trimmed = inputAddr.trim()
        if (trimmed.isNotEmpty()) {
            searchAddr = trimmed
        }
    }

    LaunchedEffect(searchAddr) {
        isLoading = true
        errorMessage = null

        try {
            val result = withContext(Dispatchers.IO) {
                val fetchedSpots = fetchWasteSpotsByAddress(
                    addr = searchAddr,
                    serviceKey = BuildConfig.PUBLIC_DATA_SERVICE_KEY
                )

                geocodeWasteSpotsWithAndroid(
                    context = context,
                    spots = fetchedSpots
                )
            }

            spots = result.filter {
                it.latitude != null && it.longitude != null
            }
        } catch (e: Exception) {
            spots = emptyList()
            errorMessage = e.message ?: "수거함 데이터를 불러오지 못했습니다."
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputAddr,
                onValueChange = { inputAddr = it },
                modifier = Modifier.weight(1f),
                label = { Text("지역명 검색") },
                placeholder = { Text("예: 용답동, 하계동, 노원구") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        requestSearch()
                    }
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    requestSearch()
                }
            ) {
                Text("검색")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val firstSpot = spots.firstOrNull()

            key(firstSpot?.latitude, firstSpot?.longitude) {
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition(
                        LatLng(
                            firstSpot?.latitude ?: 37.5665,
                            firstSpot?.longitude ?: 126.9780
                        ),
                        if (firstSpot != null) 14.0 else 11.0
                    )
                }

                NaverMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    spots.forEach { spot ->
                        val latitude = spot.latitude
                        val longitude = spot.longitude

                        if (latitude != null && longitude != null) {
                            Marker(
                                state = MarkerState(
                                    position = LatLng(latitude, longitude)
                                ),
                                captionText = spot.spotName,
                                subCaptionText = spot.detailLocation.ifBlank {
                                    spot.address
                                }
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                )
            }
        }

        Text(
            text = "검색 지역: $searchAddr / 중소형 수거함 ${spots.size}개",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )

        WasteSpotList(
            spots = spots,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

@Composable
private fun WasteSpotList(
    spots: List<WasteSpot>,
    modifier: Modifier = Modifier
) {
    if (spots.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("표시할 중소형 수거함이 없습니다.")
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(spots) { spot ->
            WasteSpotCard(spot = spot)
        }
    }
}

@Composable
private fun WasteSpotCard(
    spot: WasteSpot
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📍 ${spot.spotName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "주소: ${spot.address}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = "상세위치: ${spot.detailLocation.ifBlank { "정보 없음" }}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}