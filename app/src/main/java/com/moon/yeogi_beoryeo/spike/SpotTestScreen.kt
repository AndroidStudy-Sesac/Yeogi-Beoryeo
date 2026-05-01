package com.moon.yeogi_beoryeo.spike

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SpotTestScreen(viewModel: SpotViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "🚮 쓰레기 배출 정보 검색",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("동 이름 입력 (예: 문래동)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = { viewModel.searchByKeyword(searchText) },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("검색")
            }

            IconButton(
                onClick = {
                    viewModel.searchByLocation(sggName = "영등포구", dongName = "문래동")
                },
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text("🗺️", style = TextStyle(fontSize = 24.sp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        when (val state = uiState) {
            is SpikeUiState.Loading -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is SpikeUiState.Error -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is SpikeUiState.Success -> {
                Column(modifier = Modifier.weight(1f)) {
                    Text("🗓️ 구청 배출 가이드", style = MaterialTheme.typography.titleMedium)
                    ScheduleList(state.schedules, modifier = Modifier.weight(0.6f))

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("📍 수거함/배출처 위치", style = MaterialTheme.typography.titleMedium)
                    LocationList(state.locations, modifier = Modifier.weight(0.4f))
                }
            }
        }
    }
}

@Composable
fun ScheduleList(schedules: List<SpotDetailItem>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        items(schedules) { item ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏠 ${item.sggName} 배출 안내", style = MaterialTheme.typography.headlineSmall)
                    }
                    Text(
                        "배출 장소: ${item.placeType} (${item.placeDetail})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    WasteDetailSection("일반 쓰레기", item.generalDays, item.generalStart, item.generalEnd, item.generalMethod)
                    WasteDetailSection("음식물 쓰레기", item.foodDays, item.foodStart, item.foodEnd, item.foodMethod)
                    WasteDetailSection("재활용품", item.recyclingDays, item.recyclingStart, item.recyclingEnd, item.recyclingMethod)

                    if (!item.bulkMethod.isNullOrEmpty()) {
                        Text("대형 폐기물", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                        Text(item.bulkMethod, style = MaterialTheme.typography.bodySmall)
                    }

                    if (!item.uncollectedDay.isNullOrEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.padding(top = 12.dp).fillMaxWidth()
                        ) {
                            Text(
                                "⚠️ 수거 안 함: ${item.uncollectedDay}",
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WasteDetailSection(title: String, days: String?, start: String?, end: String?, method: String?) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        val formattedDays = days?.replace("+", ", ") ?: "정보 없음"
        Text("📅 요일: $formattedDays", style = MaterialTheme.typography.bodyMedium)

        if (!start.isNullOrEmpty() && !end.isNullOrEmpty()) {
            Text("⏰ 시간: $start ~ $end", style = MaterialTheme.typography.bodyMedium)
        }

        if (!method.isNullOrEmpty()) {
            Text(
                "💡 방법: $method",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LocationList(locations: List<SpotBasicItem>, modifier: Modifier = Modifier) {
    if (locations.isEmpty()) {
        Text(
            "조회된 수거함 위치 정보가 없습니다.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    LazyColumn(modifier = modifier) {
        items(locations) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val icon = when {
                        item.spotName?.contains("판매") == true -> "🏪"
                        item.spotName?.contains("수거") == true -> "🗑️"
                        else -> "📍"
                    }

                    Text(
                        text = "$icon ${item.spotName ?: "미지정 수거처"}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.address ?: "주소 정보 없음",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (!item.addressDetail.isNullOrEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "상세: ${item.addressDetail}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}