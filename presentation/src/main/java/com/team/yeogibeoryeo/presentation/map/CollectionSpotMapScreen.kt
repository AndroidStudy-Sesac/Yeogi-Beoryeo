package com.team.yeogibeoryeo.presentation.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.presentation.map.components.EmptySpotResult
import com.team.yeogibeoryeo.presentation.map.components.MapSearchBar
import com.team.yeogibeoryeo.presentation.map.components.SpotBottomList
import com.team.yeogibeoryeo.presentation.map.components.SpotFilterChipRow

@Composable
fun CollectionSpotMapScreen(
    modifier: Modifier = Modifier,
    viewModel: CollectionSpotMapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CollectionSpotMapContent(
        uiState = uiState,
        onKeywordChanged = viewModel::onSearchKeywordChanged,
        onSearchClick = viewModel::searchByKeyword,
        onTypeClick = viewModel::onSpotTypeClick,
        onSpotClick = viewModel::onSpotClick,
        modifier = modifier,
    )
}

@Composable
private fun CollectionSpotMapContent(
    uiState: CollectionSpotMapUiState,
    onKeywordChanged: (String) -> Unit,
    onSearchClick: () -> Unit,
    onTypeClick: (CollectionSpotType) -> Unit,
    onSpotClick: (CollectionSpot) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        MapSearchBar(
            keyword = uiState.searchKeyword,
            onKeywordChanged = onKeywordChanged,
            onSearchClick = onSearchClick,
        )

        SpotFilterChipRow(
            selectedTypes = uiState.selectedTypes,
            onTypeClick = onTypeClick,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "지도 영역",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.45f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                EmptySpotResult(
                    title = "수거 장소를 불러오지 못했습니다.",
                    description = uiState.errorMessage,
                )
            }

            uiState.spots.isEmpty() && uiState.searchKeyword.isNotBlank() -> {
                EmptySpotResult()
            }

            uiState.spots.isNotEmpty() -> {
                SpotBottomList(
                    spots = uiState.spots,
                    onSpotClick = onSpotClick,
                    modifier = Modifier.weight(0.45f),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CollectionSpotMapContentPreview() {
    MaterialTheme {
        Surface {
            CollectionSpotMapContent(
                uiState = CollectionSpotMapUiState(
                    searchKeyword = "문래동",
                ),
                onKeywordChanged = {},
                onSearchClick = {},
                onTypeClick = {},
                onSpotClick = {},
            )
        }
    }
}