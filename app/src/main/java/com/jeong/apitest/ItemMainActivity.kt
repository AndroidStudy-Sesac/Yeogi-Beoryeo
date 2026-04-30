package com.jeong.apitest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jeong.apitest.ui.theme.ApiTestTheme
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ItemMainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        val contentType = "application/json".toMediaType()

        val wasteRetrofit = Retrofit.Builder()
            .baseUrl("https://apis.data.go.kr/1482000/WasteRecyclingService/")
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
        val wasteService = wasteRetrofit.create(ItemWasteService::class.java)

        val householdRetrofit = Retrofit.Builder()
            .baseUrl("https://apis.data.go.kr/1741000/household_waste_info/")
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
        val householdService = householdRetrofit.create(ItemHouseholdWasteService::class.java)

        val repository = ItemWasteRepository(wasteService, householdService)

        setContent {
            ApiTestTheme {
                val viewModel: ItemWasteViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return ItemWasteViewModel(repository) as T
                        }
                    }
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                ) { innerPadding ->
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    ItemWasteMainScreen(
                        uiState = uiState,
                        onTabSelected = viewModel::onTabSelected,
                        onItemQueryChange = viewModel::onItemQueryChange,
                        onRegionQueryChange = viewModel::onRegionQueryChange,
                        onLatitudeChange = viewModel::onLatitudeChange,
                        onLongitudeChange = viewModel::onLongitudeChange,
                        onRadiusChange = viewModel::onRadiusChange,
                        onAdministrativeCodeChange = viewModel::onAdministrativeCodeChange,
                        onUpdatedFromChange = viewModel::onUpdatedFromChange,
                        onUpdatedUntilChange = viewModel::onUpdatedUntilChange,
                        onBaseDateFromChange = viewModel::onBaseDateFromChange,
                        onBaseDateUntilChange = viewModel::onBaseDateUntilChange,
                        onNumOfRowsChange = viewModel::onNumOfRowsChange,
                        onSearch = viewModel::search,
                        onLoadMore = viewModel::loadMore,
                        toggleAdvancedSettings = viewModel::toggleAdvancedSettings,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ItemWasteMainScreen(
    uiState: ItemWasteUiState,
    onTabSelected: (Int) -> Unit,
    onItemQueryChange: (String) -> Unit,
    onRegionQueryChange: (String) -> Unit,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onAdministrativeCodeChange: (String) -> Unit,
    onUpdatedFromChange: (String) -> Unit,
    onUpdatedUntilChange: (String) -> Unit,
    onBaseDateFromChange: (String) -> Unit,
    onBaseDateUntilChange: (String) -> Unit,
    onNumOfRowsChange: (String) -> Unit,
    onSearch: (SearchType) -> Unit,
    onLoadMore: () -> Unit,
    toggleAdvancedSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf("품목 검색", "우리 동네 정보")

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = uiState.selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = uiState.selectedTab == index,
                    onClick = { onTabSelected(index) },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }

        when (uiState.selectedTab) {
            0 -> ItemSearchScreen(
                uiState = uiState,
                onItemQueryChange = onItemQueryChange,
                onSearch = onSearch,
                onLoadMore = onLoadMore
            )

            1 -> LocalInfoScreen(
                uiState = uiState,
                onRegionQueryChange = onRegionQueryChange,
                onLatitudeChange = onLatitudeChange,
                onLongitudeChange = onLongitudeChange,
                onRadiusChange = onRadiusChange,
                onAdministrativeCodeChange = onAdministrativeCodeChange,
                onUpdatedFromChange = onUpdatedFromChange,
                onUpdatedUntilChange = onUpdatedUntilChange,
                onBaseDateFromChange = onBaseDateFromChange,
                onBaseDateUntilChange = onBaseDateUntilChange,
                onNumOfRowsChange = onNumOfRowsChange,
                onSearch = onSearch,
                onLoadMore = onLoadMore,
                toggleAdvancedSettings = toggleAdvancedSettings
            )
        }
    }
}

@Composable
fun ItemSearchScreen(
    uiState: ItemWasteUiState,
    onItemQueryChange: (String) -> Unit,
    onSearch: (SearchType) -> Unit,
    onLoadMore: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = uiState.itemQuery,
            onValueChange = onItemQueryChange,
            label = { Text("버리려는 물건을 입력하세요 (예: 유리, 형광등)") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )

        Button(
            onClick = { onSearch(SearchType.ITEM) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("분리배출 방법 찾기")
        }

        ItemResultList(uiState, SearchType.ITEM, onLoadMore)
    }
}

@Composable
fun LocalInfoScreen(
    uiState: ItemWasteUiState,
    onRegionQueryChange: (String) -> Unit,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onAdministrativeCodeChange: (String) -> Unit,
    onUpdatedFromChange: (String) -> Unit,
    onUpdatedUntilChange: (String) -> Unit,
    onBaseDateFromChange: (String) -> Unit,
    onBaseDateUntilChange: (String) -> Unit,
    onNumOfRowsChange: (String) -> Unit,
    onSearch: (SearchType) -> Unit,
    onLoadMore: () -> Unit,
    toggleAdvancedSettings: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.5f
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = uiState.regionQuery,
                    onValueChange = onRegionQueryChange,
                    label = { Text("지역명 입력 (예: 노원구, 하계동)") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    placeholder = {
                        Text(uiState.lastDetectedSgg?.let { "자동 인식됨: $it" }
                            ?: "예: 하계동, 노원구")
                    },
                    supportingText = {
                        if (uiState.regionQuery.isBlank() && uiState.lastDetectedSgg != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "장소 검색 기반 '${uiState.lastDetectedSgg}' 자동 사용 중",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            Text("요일 정보는 '구' 또는 '시' 단위 검색이 정확합니다.", fontSize = 11.sp)
                        }
                    }
                )

                TextButton(
                    onClick = toggleAdvancedSettings,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (uiState.showAdvancedSettings) "상세 설정 닫기" else "모든 API 파라미터 설정")
                }

                if (uiState.showAdvancedSettings) {
                    Text("📍 위치 및 반경 설정 (기후부 API)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.latitudeText,
                            onValueChange = onLatitudeChange,
                            label = { Text("위도") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = uiState.longitudeText,
                            onValueChange = onLongitudeChange,
                            label = { Text("경도") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        "검색 반경: ${uiState.radiusValue.toInt()}m",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Slider(
                        value = uiState.radiusValue,
                        onValueChange = onRadiusChange,
                        valueRange = 100f..5000f
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text("🔍 상세 필터링 (행안부 API)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = uiState.administrativeCode,
                        onValueChange = onAdministrativeCodeChange,
                        label = { Text("개방자치단체코드") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        "🕒 데이터 갱신 시점 범위",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.updatedFrom,
                            onValueChange = onUpdatedFromChange,
                            label = { Text("이후(GTE)") },
                            placeholder = { Text("YYYYMMDD...") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = uiState.updatedUntil,
                            onValueChange = onUpdatedUntilChange,
                            label = { Text("이전(LT)") },
                            placeholder = { Text("YYYYMMDD...") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("📅 데이터 기준일자 범위", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.baseDateFrom,
                            onValueChange = onBaseDateFromChange,
                            label = { Text("이후(GTE)") },
                            placeholder = { Text("YYYYMMDD") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = uiState.baseDateUntil,
                            onValueChange = onBaseDateUntilChange,
                            label = { Text("이전(LT)") },
                            placeholder = { Text("YYYYMMDD") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onSearch(SearchType.SPOT) },
                modifier = Modifier.weight(1f)
            ) {
                Text("수거 장소 찾기", fontSize = 12.sp)
            }
            Button(
                onClick = { onSearch(SearchType.HOUSEHOLD_INFO) },
                modifier = Modifier.weight(1f)
            ) {
                Text("배출 요일/시간", fontSize = 12.sp)
            }
        }

        ItemResultList(uiState, uiState.lastSearchType, onLoadMore)
    }
}

@Composable
fun ItemResultList(uiState: ItemWasteUiState, currentType: SearchType?, onLoadMore: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading && uiState.currentPage == 1) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        Column {
            uiState.errorMessage?.let {
                Text(
                    it,
                    color = Color.Red,
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontSize = 13.sp
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                when (currentType) {
                    SearchType.ITEM -> {
                        itemsIndexed(uiState.itemsResult) { index, item ->
                            ItemGuideCard(item)
                            if (index == uiState.itemsResult.lastIndex && !uiState.isLoading) {
                                SideEffect { onLoadMore() }
                            }
                        }
                    }

                    SearchType.SPOT -> {
                        itemsIndexed(uiState.spotsResult) { index, spot ->
                            SpotInfoCard(spot)
                            if (index == uiState.spotsResult.lastIndex && !uiState.isLoading) {
                                SideEffect { onLoadMore() }
                            }
                        }
                    }

                    SearchType.HOUSEHOLD_INFO -> {
                        val trimmedQuery = uiState.regionQuery.trim()
                        val isSggInput =
                            trimmedQuery.endsWith("시") || trimmedQuery.endsWith("군") || trimmedQuery.endsWith(
                                "구"
                            )

                        val searchBase =
                            if ((trimmedQuery.endsWith("동") || trimmedQuery.endsWith("읍") || trimmedQuery.endsWith(
                                    "면"
                                )) && trimmedQuery.length > 1
                            ) {
                                trimmedQuery.dropLast(1)
                            } else {
                                trimmedQuery
                            }

                        // 검색어와 일치하는 항목(matches)과 그 외 항목(others)으로 분리하여 합침 (정렬 효과)
                        val (matches, others) = if (trimmedQuery.isNotBlank() && !isSggInput) {
                            uiState.householdResult.partition {
                                it.MNG_ZONE_TRGT_RGN_NM?.contains(
                                    trimmedQuery,
                                    ignoreCase = true
                                ) == true ||
                                        it.MNG_ZONE_TRGT_RGN_NM?.contains(
                                            searchBase,
                                            ignoreCase = true
                                        ) == true
                            }
                        } else {
                            uiState.householdResult to emptyList()
                        }

                        val displayList = matches + others

                        if (uiState.householdResult.isNotEmpty()) {
                            item {
                                val sggName = uiState.householdResult.firstOrNull()?.SGG_NM ?: ""
                                val hasNoMatch =
                                    matches.isEmpty() && trimmedQuery.isNotBlank() && !isSggInput

                                Surface(
                                    color = if (hasNoMatch) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .padding(bottom = 8.dp)
                                        .fillMaxWidth()
                                ) {
                                    Text(
                                        text = when {
                                            hasNoMatch -> "💡 '$trimmedQuery'와 일치하는 구역이 없어 $sggName 전체를 표시합니다."
                                            matches.isNotEmpty() -> "📍 '$trimmedQuery' 맞춤 정보와 함께 $sggName 전체를 표시합니다."
                                            else -> "📍 $sggName 지역 정보 표시 중"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        ),
                                        color = if (hasNoMatch) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        itemsIndexed(displayList) { index, info ->
                            LocalScheduleCard(info, uiState.regionQuery)
                            if (index == displayList.lastIndex && !uiState.isLoading) {
                                SideEffect { onLoadMore() }
                            }
                        }
                    }

                    else -> {}
                }

                if (uiState.isLoading && uiState.currentPage > 1) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemGuideCard(item: ItemInfoResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "분리배출",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(item.itemNm, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Row {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.dschgMthd,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SpotInfoCard(spot: ItemSpotResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                alpha = 0.3f
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "📍 ${spot.spotNm}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("기본주소: ${spot.addrBase}", fontSize = 14.sp)
            if (spot.addrDtl.isNotBlank() && spot.addrDtl != "null") {
                Text(
                    "상세위치: ${spot.addrDtl}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LocalScheduleCard(info: ItemHouseholdWasteInfo, userQuery: String) {
    val trimmed = userQuery.trim()
    val searchBase =
        if ((trimmed.endsWith("동") || trimmed.endsWith("읍") || trimmed.endsWith("면")) && trimmed.length > 1) {
            trimmed.dropLast(1)
        } else {
            trimmed
        }
    val isTargetMatch = trimmed.isNotBlank() && (
            info.MNG_ZONE_TRGT_RGN_NM?.contains(trimmed, ignoreCase = true) == true ||
                    info.MNG_ZONE_TRGT_RGN_NM?.contains(searchBase, ignoreCase = true) == true
            )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = if (isTargetMatch) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(if (isTargetMatch) 6.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isTargetMatch) {
                Text(
                    "✨ 내 지역 맞춤 정보 (검색어 포함)",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            val displayTitle = remember(info) {
                val sgg = info.SGG_NM ?: ""
                val zone = info.MNG_ZONE_NM?.takeIf { it != "없음" && !it.contains("권역") } ?: ""
                val target = info.MNG_ZONE_TRGT_RGN_NM?.takeIf { it != "없음" } ?: ""

                listOf(sgg, zone, target)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
            }
            Text(
                text = displayTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isTargetMatch) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32)
            )
            Text(
                "배출 장소: ${info.EMSN_PLC} (${info.EMSN_PLC_TYPE})",
                fontSize = 13.sp,
                color = Color.Gray
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            ScheduleRow(
                "일반",
                info.LF_WST_EMSN_DOW,
                info.LF_WST_EMSN_MTHD,
                info.LF_WST_EMSN_BGNG_TM,
                info.LF_WST_EMSN_END_TM,
                Color(0xFF5D4037)
            )
            ScheduleRow(
                "음식물",
                info.FOD_WST_EMSN_DOW,
                info.FOD_WST_EMSN_MTHD,
                info.FOD_WST_EMSN_BGNG_TM,
                info.FOD_WST_EMSN_END_TM,
                Color(0xFFF57C00)
            )
            ScheduleRow(
                "재활용",
                info.RCYCL_EMSN_DOW,
                info.RCYCL_EMSN_MTHD,
                info.RCYCL_EMSN_BGNG_TM,
                info.RCYCL_EMSN_END_TM,
                Color(0xFF1976D2)
            )

            if (!info.TMPRY_BULK_WASTE_EMSN_MTHD.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                ScheduleRow(
                    "대형 폐기물",
                    "별도 문의",
                    info.TMPRY_BULK_WASTE_EMSN_MTHD,
                    info.TMPRY_BULK_WASTE_EMSN_BGNG_TM,
                    info.TMPRY_BULK_WASTE_EMSN_END_TM,
                    Color(0xFF7E57C2)
                )
                if (!info.TMPRY_BULK_WASTE_EMSN_PLC.isNullOrBlank()) {
                    Text(
                        "📍 대형 배출장소: ${info.TMPRY_BULK_WASTE_EMSN_PLC}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }

            if (!info.UNCLLT_DAY.isNullOrBlank() && info.UNCLLT_DAY != "없음") {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(4.dp)) {
                    Text(
                        "⚠️ 미수거일: ${info.UNCLLT_DAY}",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!info.MNG_DEPT_NM.isNullOrBlank()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${info.MNG_DEPT_NM} (${info.MNG_DEPT_TELNO ?: "번호 없음"})",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleRow(
    label: String,
    dow: String?,
    method: String?,
    startTime: String?,
    endTime: String?,
    color: Color
) {
    if (dow.isNullOrBlank() || dow == "null") return
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
            Spacer(modifier = Modifier.width(8.dp))
            Text("요일: $dow", fontSize = 13.sp, fontWeight = FontWeight.Medium)

            if (!startTime.isNullOrBlank() && !endTime.isNullOrBlank() && startTime != "null" && endTime != "null") {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                    Text(
                        text = "⏰ $startTime ~ $endTime",
                        fontSize = 11.sp,
                        color = color,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Text(
            text = method ?: "",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
