package com.team.yeogibeoryeo.presentation.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.presentation.map.components.CollectionSpotNaverMap
import com.team.yeogibeoryeo.presentation.map.components.CurrentLocationButton
import com.team.yeogibeoryeo.presentation.map.components.EmptySpotResult
import com.team.yeogibeoryeo.presentation.map.components.MapSearchBar
import com.team.yeogibeoryeo.presentation.map.components.SpotBottomList
import com.team.yeogibeoryeo.presentation.map.components.SpotFilterChipRow

@Composable
fun CollectionSpotMapScreen(
    modifier: Modifier = Modifier,
    viewModel: CollectionSpotMapViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            val coordinate = getLastKnownCoordinate(context)

            if (coordinate != null) {
                viewModel.searchByCurrentLocation(
                    latitude = coordinate.latitude,
                    longitude = coordinate.longitude,
                )
            } else {
                viewModel.onCurrentLocationNotFound()
            }
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    CollectionSpotMapContent(
        uiState = uiState,
        onKeywordChanged = viewModel::onSearchKeywordChanged,
        onSearchClick = viewModel::searchByKeyword,
        onCurrentLocationClick = {
            if (hasFineLocationPermission(context)) {
                val coordinate = getLastKnownCoordinate(context)

                if (coordinate != null) {
                    viewModel.searchByCurrentLocation(
                        latitude = coordinate.latitude,
                        longitude = coordinate.longitude,
                    )
                } else {
                    viewModel.onCurrentLocationNotFound()
                }
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        },
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
    onCurrentLocationClick: () -> Unit,
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            CurrentLocationButton(
                onClick = onCurrentLocationClick,
            )
        }

        CollectionSpotNaverMap(
            spots = uiState.spots,
            selectedSpot = uiState.selectedSpot,
            onSpotClick = onSpotClick,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

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

            uiState.locationNoticeMessage != null -> {
                EmptySpotResult(
                    title = "현재 위치 검색 안내",
                    description = uiState.locationNoticeMessage,
                )
            }

            uiState.errorMessage != null -> {
                EmptySpotResult(
                    title = "수거 장소를 불러오지 못했습니다.",
                    description = uiState.errorMessage,
                )
            }

            uiState.hasSearched && uiState.spots.isEmpty() -> {
                EmptySpotResult()
            }

            uiState.spots.isNotEmpty() -> {
                SpotBottomList(
                    spots = uiState.spots,
                    selectedSpot = uiState.selectedSpot,
                    onSpotClick = onSpotClick,
                    modifier = Modifier.weight(0.45f),
                )
            }
        }
    }
}

private fun hasFineLocationPermission(
    context: Context,
): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("MissingPermission")
private fun getLastKnownCoordinate(
    context: Context,
): Coordinate? {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    return try {
        val providers = locationManager.getProviders(true)

        providers
            .asSequence()
            .mapNotNull { provider ->
                locationManager.getLastKnownLocation(provider)
            }
            .maxByOrNull { location ->
                location.time
            }
            ?.let { location ->
                Coordinate(
                    latitude = location.latitude,
                    longitude = location.longitude,
                )
            }
    } catch (exception: SecurityException) {
        null
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
                    hasSearched = true,
                ),
                onKeywordChanged = {},
                onSearchClick = {},
                onCurrentLocationClick = {},
                onTypeClick = {},
                onSpotClick = {},
            )
        }
    }
}