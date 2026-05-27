package com.team.yeogibeoryeo.presentation.regionalguide

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.team.yeogibeoryeo.presentation.regionalguide.components.RegionSelectorSection
import com.team.yeogibeoryeo.presentation.regionalguide.components.RegionalGuideEmptyResult
import com.team.yeogibeoryeo.presentation.regionalguide.components.RegionalGuideSearchBar
import com.team.yeogibeoryeo.presentation.regionalguide.components.RegionalGuideSummaryCard
import com.team.yeogibeoryeo.presentation.regionalguide.components.RegionalWasteScheduleCard
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalWasteScheduleUiModel

@Composable
fun RegionalGuideRoute(
    modifier: Modifier = Modifier,
    initialKeyword: String? = null,
    initialAddress: String? = null,
    viewModel: RegionalGuideViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchKeyword by viewModel.searchKeyword.collectAsStateWithLifecycle()
    val regionSelectorUiState by viewModel.regionSelectorUiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialKeyword, initialAddress) {
        when {
            !initialAddress.isNullOrBlank() -> viewModel.loadByAddress(initialAddress)
            !initialKeyword.isNullOrBlank() -> {
                viewModel.onSearchKeywordChanged(initialKeyword)
                viewModel.searchByKeyword(initialKeyword)
            }
        }
    }

    RegionalGuideScreen(
        uiState = uiState,
        searchKeyword = searchKeyword,
        regionSelectorUiState = regionSelectorUiState,
        onSearchKeywordChange = viewModel::onSearchKeywordChanged,
        onSearchClick = viewModel::searchCurrentKeyword,
        onRetryClick = viewModel::retryLastRequest,
        onSidoSelected = viewModel::onSidoSelected,
        onSigunguSelected = viewModel::onSigunguSelected,
        onEupmyeondongSelected = viewModel::onEupmyeondongSelected,
        onRegionSelectionSearchClick = viewModel::onRegionSelectionSearchClick,
        modifier = modifier,
    )
}

@Composable
fun RegionalGuideScreen(
    uiState: RegionalGuideUiState,
    searchKeyword: String,
    regionSelectorUiState: RegionSelectorUiState,
    onSearchKeywordChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onRetryClick: () -> Unit,
    onSidoSelected: (String) -> Unit,
    onSigunguSelected: (String) -> Unit,
    onEupmyeondongSelected: (String) -> Unit,
    onRegionSelectionSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isRegionSelectorExpanded by rememberSaveable { mutableStateOf(false) }
    val isRegionSelectorCompact =
        uiState !is RegionalGuideUiState.Idle &&
            !isRegionSelectorExpanded &&
            regionSelectorUiState.selectedRegionText != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(20.dp)
    ) {
        Text(
            text = "지역별 배출 가이드",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "지역명을 입력하면 생활쓰레기, 음식물쓰레기, 재활용품 배출 정보를 확인할 수 있어요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        RegionalGuideSearchBar(
            keyword = searchKeyword,
            onKeywordChange = onSearchKeywordChange,
            onSearchClick = onSearchClick,
        )

        Spacer(modifier = Modifier.height(16.dp))

        RegionSelectorSection(
            uiState = regionSelectorUiState,
            compact = isRegionSelectorCompact,
            onSidoSelected = onSidoSelected,
            onSigunguSelected = onSigunguSelected,
            onEupmyeondongSelected = onEupmyeondongSelected,
            onSearchClick = {
                isRegionSelectorExpanded = false
                onRegionSelectionSearchClick()
            },
            onChangeClick = {
                isRegionSelectorExpanded = true
            },
        )

        Spacer(modifier = Modifier.height(20.dp))

        RegionalGuideContent(
            uiState = uiState,
            onRetryClick = onRetryClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RegionalGuideContent(
    uiState: RegionalGuideUiState,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        RegionalGuideUiState.Idle -> {
            Spacer(modifier = modifier)
        }

        is RegionalGuideUiState.Loading -> {
            RegionalGuideLoadingContent(
                query = uiState.query,
                modifier = modifier
            )
        }

        is RegionalGuideUiState.Success -> {
            RegionalGuideSuccessContent(
                guide = uiState.guide,
                modifier = modifier
            )
        }

        is RegionalGuideUiState.Empty -> {
            RegionalGuideEmptyResult(
                message = uiState.message,
                modifier = modifier
            )
        }

        is RegionalGuideUiState.Error -> {
            RegionalGuideErrorContent(
                message = uiState.message,
                onRetryClick = onRetryClick,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun RegionalGuideLoadingContent(
    query: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator()

        Text(
            text = "\"$query\" 배출 가이드를 불러오는 중입니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RegionalGuideSuccessContent(
    guide: RegionalGuideUiModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            RegionalGuideSummaryCard(guide = guide)
        }

        item {
            Text(
                text = "배출 요일 및 시간",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(
            items = guide.schedules,
            key = { schedule -> schedule.wasteTypeName }
        ) { schedule ->
            RegionalWasteScheduleCard(schedule = schedule)
        }
    }
}

@Composable
private fun RegionalGuideErrorContent(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "오류가 발생했습니다",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            TextButton(
                onClick = onRetryClick
            ) {
                Text(text = "다시 시도")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegionalGuideScreenIdlePreview() {
    MaterialTheme {
        RegionalGuideScreen(
            uiState = RegionalGuideUiState.Idle,
            searchKeyword = "",
            regionSelectorUiState = RegionSelectorUiState(
                sidoOptions = listOf("서울특별시", "경기도", "인천광역시"),
            ),
            onSearchKeywordChange = {},
            onSearchClick = {},
            onRetryClick = {},
            onSidoSelected = {},
            onSigunguSelected = {},
            onEupmyeondongSelected = {},
            onRegionSelectionSearchClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegionalGuideScreenLoadingPreview() {
    MaterialTheme {
        RegionalGuideScreen(
            uiState = RegionalGuideUiState.Loading(
                query = "영등포구"
            ),
            searchKeyword = "영등포구",
            regionSelectorUiState = RegionSelectorUiState(
                sidoOptions = listOf("서울특별시", "경기도", "인천광역시"),
            ),
            onSearchKeywordChange = {},
            onSearchClick = {},
            onRetryClick = {},
            onSidoSelected = {},
            onSigunguSelected = {},
            onEupmyeondongSelected = {},
            onRegionSelectionSearchClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegionalGuideScreenSuccessPreview() {
    MaterialTheme {
        RegionalGuideScreen(
            uiState = RegionalGuideUiState.Success(
                query = "영등포구",
                guide = RegionalGuideUiModel(
                    regionName = "서울특별시 영등포구 문래동",
                    managementZoneName = "영등포구",
                    targetRegionName = "문래동",
                    disposalPlaceType = "문전수거",
                    disposalPlaceDescription = "집 앞 지정 장소에 배출",
                    schedules = listOf(
                        RegionalWasteScheduleUiModel(
                            wasteTypeName = "일반쓰레기",
                            disposalDays = "월, 수, 금",
                            disposalTime = "18:00 ~ 24:00",
                            disposalMethod = "종량제 봉투에 담아 배출",
                        ),
                        RegionalWasteScheduleUiModel(
                            wasteTypeName = "음식물쓰레기",
                            disposalDays = "화, 목, 일",
                            disposalTime = "18:00 ~ 24:00",
                            disposalMethod = "음식물 전용 용기에 담아 배출",
                        ),
                        RegionalWasteScheduleUiModel(
                            wasteTypeName = "재활용품",
                            disposalDays = "목",
                            disposalTime = "18:00 ~ 24:00",
                            disposalMethod = "품목별로 분리하여 배출",
                        ),
                    ),
                    uncollectedDays = "토요일",
                    departmentInfo = "청소행정과 02-0000-0000",
                ),
            ),
            searchKeyword = "영등포구",
            regionSelectorUiState = RegionSelectorUiState(
                sidoOptions = listOf("서울특별시", "경기도", "인천광역시"),
                sigunguOptions = listOf("구로구", "영등포구", "종로구"),
                selectedSido = "서울특별시",
                selectedSigungu = "영등포구",
                eupmyeondongOptions = listOf("문래동", "당산동", "여의동"),
                selectedEupmyeondong = "문래동",
            ),
            onSearchKeywordChange = {},
            onSearchClick = {},
            onRetryClick = {},
            onSidoSelected = {},
            onSigunguSelected = {},
            onEupmyeondongSelected = {},
            onRegionSelectionSearchClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegionalGuideScreenEmptyPreview() {
    MaterialTheme {
        RegionalGuideScreen(
            uiState = RegionalGuideUiState.Empty(
                query = "없는 지역",
                message = "해당 지역의 배출 가이드 정보가 없습니다."
            ),
            searchKeyword = "없는 지역",
            regionSelectorUiState = RegionSelectorUiState(
                sidoOptions = listOf("서울특별시", "경기도", "인천광역시"),
            ),
            onSearchKeywordChange = {},
            onSearchClick = {},
            onRetryClick = {},
            onSidoSelected = {},
            onSigunguSelected = {},
            onEupmyeondongSelected = {},
            onRegionSelectionSearchClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegionalGuideScreenErrorPreview() {
    MaterialTheme {
        RegionalGuideScreen(
            uiState = RegionalGuideUiState.Error(
                query = "영등포구",
                message = "지역별 배출 가이드를 조회하는 중 오류가 발생했습니다."
            ),
            searchKeyword = "영등포구",
            regionSelectorUiState = RegionSelectorUiState(
                sidoOptions = listOf("서울특별시", "경기도", "인천광역시"),
            ),
            onSearchKeywordChange = {},
            onSearchClick = {},
            onRetryClick = {},
            onSidoSelected = {},
            onSigunguSelected = {},
            onEupmyeondongSelected = {},
            onRegionSelectionSearchClick = {},
        )
    }
}
