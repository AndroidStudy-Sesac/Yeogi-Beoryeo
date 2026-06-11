package com.team.yeogibeoryeo.presentation.map

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naver.maps.map.compose.LocationTrackingMode
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.presentation.map.components.CollectionSpotNaverMap
import com.team.yeogibeoryeo.presentation.map.components.CurrentLocationSearchLoadingOverlay
import com.team.yeogibeoryeo.presentation.map.components.MapOverlayControls
import com.team.yeogibeoryeo.presentation.map.components.MapResultBottomSheetPeekHeight
import com.team.yeogibeoryeo.presentation.map.components.MapSheetLevel
import com.team.yeogibeoryeo.presentation.map.components.MapSpotDetailBottomSheetPeekHeight
import com.team.yeogibeoryeo.presentation.map.components.MyLocationButton
import com.team.yeogibeoryeo.presentation.map.components.SpotBottomSheetContent
import com.team.yeogibeoryeo.presentation.map.components.SpotDetailBottomSheetContent
import com.team.yeogibeoryeo.presentation.map.components.ThreeStepMapBottomSheet
import com.team.yeogibeoryeo.presentation.map.location.rememberFineLocationPermissionGranted
import com.team.yeogibeoryeo.presentation.map.location.rememberCurrentLocationSearchRequester

@Composable
fun CollectionSpotMapScreen(
    modifier: Modifier = Modifier,
    viewModel: CollectionSpotMapViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hasFineLocationPermission = rememberFineLocationPermissionGranted()
    var hasGrantedLocationPermissionInSession by rememberSaveable {
        mutableStateOf(false)
    }
    val isLocationPermissionGranted =
        hasFineLocationPermission || hasGrantedLocationPermissionInSession
    var locationTrackingMode by remember {
        mutableStateOf(LocationTrackingMode.None)
    }

    LaunchedEffect(hasFineLocationPermission) {
        if (!hasFineLocationPermission) {
            hasGrantedLocationPermissionInSession = false
        }
    }

    val requestCurrentLocationSearch = rememberCurrentLocationSearchRequester(
        onGranted = {
            hasGrantedLocationPermissionInSession = true
            locationTrackingMode = LocationTrackingMode.NoFollow
            viewModel.searchByCurrentLocation()
        },
        onDenied = viewModel::onLocationPermissionDenied,
    )
    val requestMyLocationTracking = rememberCurrentLocationSearchRequester(
        onGranted = {
            hasGrantedLocationPermissionInSession = true
            locationTrackingMode = LocationTrackingMode.Follow
        },
        onDenied = viewModel::onLocationPermissionDenied,
    )

    LaunchedEffect(Unit) {
        viewModel.searchByCurrentLocationOnMapEntryIfPermitted()
    }

    CollectionSpotMapContent(
        uiState = uiState,
        isLocationPermissionGranted = isLocationPermissionGranted,
        locationTrackingMode = locationTrackingMode,
        onLocationTrackingModeChange = { mode ->
            locationTrackingMode = mode
        },
        onKeywordChanged = viewModel::onSearchKeywordChanged,
        onSearchClick = viewModel::searchByKeyword,
        onCurrentLocationClick = {
            requestCurrentLocationSearch()
        },
        onMyLocationPermissionRequest = {
            requestMyLocationTracking()
        },
        onLocationNoticeActionClick = { action ->
            context.startActivity(action.toIntent(context.packageName))
        },
        onTypeClick = viewModel::onSpotTypeClick,
        onSpotClick = viewModel::onSpotClick,
        onSpotFavoriteClick = viewModel::onSpotFavoriteClick,
        modifier = modifier,
    )
}

@Composable
private fun CollectionSpotMapContent(
    uiState: CollectionSpotMapUiState,
    isLocationPermissionGranted: Boolean,
    locationTrackingMode: LocationTrackingMode,
    onLocationTrackingModeChange: (LocationTrackingMode) -> Unit,
    onKeywordChanged: (String) -> Unit,
    onSearchClick: () -> Unit,
    onCurrentLocationClick: () -> Unit,
    onMyLocationPermissionRequest: () -> Unit,
    onLocationNoticeActionClick: (MapLocationNoticeAction) -> Unit,
    onTypeClick: (CollectionSpotType) -> Unit,
    onSpotClick: (CollectionSpot) -> Unit,
    onSpotFavoriteClick: (CollectionSpot) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCurrentLocationSearching = uiState.isLoading &&
        uiState.searchMode == MapSearchMode.CURRENT_LOCATION
    val shouldShowBottomSheet = uiState.shouldShowBottomSheet && !isCurrentLocationSearching
    var mapUiMode by remember { mutableStateOf(MapUiMode.Browsing) }
    var sheetLevel by remember { mutableStateOf(MapSheetLevel.Hidden) }
    var sheetRevealRequest by remember { mutableStateOf(0) }
    val selectedSpot = uiState.selectedSpot
    val hasNoticeOrError = uiState.locationNotice != null ||
        uiState.locationNoticeMessage != null ||
        uiState.errorMessage != null
    val mapLocationTrackingMode = when {
        !isLocationPermissionGranted -> LocationTrackingMode.None
        locationTrackingMode == LocationTrackingMode.None -> LocationTrackingMode.NoFollow
        else -> locationTrackingMode
    }

    LaunchedEffect(isLocationPermissionGranted) {
        if (!isLocationPermissionGranted) {
            onLocationTrackingModeChange(LocationTrackingMode.None)
        }
    }

    LaunchedEffect(
        uiState.hasSearched,
        uiState.isLoading,
        uiState.searchMode,
        uiState.errorMessage,
        uiState.locationNotice,
        uiState.locationNoticeMessage,
    ) {
        when {
            isCurrentLocationSearching -> {
                mapUiMode = MapUiMode.Browsing
                sheetLevel = MapSheetLevel.Hidden
            }

            uiState.isLoading || hasNoticeOrError -> {
                mapUiMode = MapUiMode.ResultList
                sheetLevel = if (hasNoticeOrError) {
                    MapSheetLevel.Medium
                } else {
                    MapSheetLevel.Peek
                }
            }

            uiState.hasSearched && mapUiMode == MapUiMode.Browsing -> {
                mapUiMode = MapUiMode.ResultList
                sheetLevel = MapSheetLevel.Peek
            }
        }
    }

    LaunchedEffect(selectedSpot?.id, uiState.spots) {
        if (selectedSpot == null && mapUiMode == MapUiMode.SpotDetail) {
            mapUiMode = if (uiState.hasSearched) {
                sheetLevel = MapSheetLevel.Peek
                MapUiMode.ResultList
            } else {
                sheetLevel = MapSheetLevel.Hidden
                MapUiMode.Browsing
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        CollectionSpotNaverMap(
            spots = uiState.spots,
            selectedSpot = uiState.selectedSpot,
            isLocationPermissionGranted = isLocationPermissionGranted,
            locationTrackingMode = mapLocationTrackingMode,
            onSpotClick = { spot ->
                onLocationTrackingModeChange(LocationTrackingMode.NoFollow)
                mapUiMode = MapUiMode.SpotDetail
                sheetLevel = MapSheetLevel.Medium
                sheetRevealRequest += 1
                onSpotClick(spot)
            },
            onMapClick = {
                onLocationTrackingModeChange(LocationTrackingMode.NoFollow)
                if (mapUiMode == MapUiMode.Browsing) {
                    mapUiMode = MapUiMode.ResultList.takeIf { shouldShowBottomSheet } ?: MapUiMode.Browsing
                    sheetLevel = MapSheetLevel.Peek.takeIf { shouldShowBottomSheet } ?: MapSheetLevel.Hidden
                } else {
                    mapUiMode = MapUiMode.Browsing
                    sheetLevel = MapSheetLevel.Hidden
                }
            },
            modifier = Modifier
                .fillMaxSize(),
        )

        if (mapUiMode != MapUiMode.SpotDetail) {
            MapOverlayControls(
                keyword = uiState.searchKeyword,
                selectedTypes = uiState.selectedTypes,
                onKeywordChanged = onKeywordChanged,
                onSearchClick = {
                    onLocationTrackingModeChange(LocationTrackingMode.NoFollow)
                    mapUiMode = MapUiMode.ResultList
                    sheetLevel = MapSheetLevel.Peek
                    onSearchClick()
                },
                onTypeClick = { type ->
                    mapUiMode = MapUiMode.ResultList
                    sheetLevel = MapSheetLevel.Peek
                    onTypeClick(type)
                },
                onCurrentLocationClick = {
                    onLocationTrackingModeChange(LocationTrackingMode.NoFollow)
                    mapUiMode = MapUiMode.ResultList
                    sheetLevel = MapSheetLevel.Peek
                    onCurrentLocationClick()
                },
            )
        }

        if (mapUiMode != MapUiMode.SpotDetail) {
            MyLocationButton(
                isTracking = mapLocationTrackingMode == LocationTrackingMode.Follow,
                onClick = {
                    if (isLocationPermissionGranted) {
                        onLocationTrackingModeChange(LocationTrackingMode.Follow)
                    } else {
                        onMyLocationPermissionRequest()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = MyLocationButtonHorizontalPadding,
                        bottom = myLocationButtonBottomPadding(
                            sheetLevel = sheetLevel,
                            shouldShowBottomSheet = shouldShowBottomSheet,
                        ),
                    ),
            )
        }

        if (isCurrentLocationSearching) {
            CurrentLocationSearchLoadingOverlay()
        }

        if (shouldShowBottomSheet) {
            ThreeStepMapBottomSheet(
                sheetLevel = sheetLevel,
                revealKey = "$mapUiMode-${selectedSpot?.id}-$sheetRevealRequest",
                onSheetLevelChanged = { level ->
                    sheetLevel = level
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                when {
                    mapUiMode == MapUiMode.SpotDetail && selectedSpot != null -> {
                        SpotDetailBottomSheetContent(
                            spot = selectedSpot,
                            onFavoriteClick = onSpotFavoriteClick,
                            onCloseClick = {
                                mapUiMode = MapUiMode.ResultList
                                sheetLevel = MapSheetLevel.Peek
                            },
                        )
                    }

                    mapUiMode == MapUiMode.ResultList -> {
                        SpotBottomSheetContent(
                            spots = uiState.spots,
                            selectedSpot = selectedSpot,
                            isLoading = uiState.isLoading,
                            hasSearched = uiState.hasSearched,
                            selectedTypes = uiState.selectedTypes,
                            locationNotice = uiState.locationNotice,
                            locationNoticeMessage = uiState.locationNoticeMessage,
                            errorMessage = uiState.errorMessage,
                            onTypeClick = onTypeClick,
                            onLocationNoticeActionClick = onLocationNoticeActionClick,
                            onSpotFavoriteClick = onSpotFavoriteClick,
                            onSpotClick = { spot ->
                                onLocationTrackingModeChange(LocationTrackingMode.NoFollow)
                                mapUiMode = MapUiMode.SpotDetail
                                sheetLevel = MapSheetLevel.Medium
                                sheetRevealRequest += 1
                                onSpotClick(spot)
                            },
                        )
                    }
                }
            }
        }
    }
}

private val CollectionSpotMapUiState.shouldShowBottomSheet: Boolean
    get() = isLoading ||
        locationNotice != null ||
        locationNoticeMessage != null ||
        errorMessage != null ||
        hasSearched ||
        spots.isNotEmpty()

private enum class MapUiMode {
    Browsing,
    ResultList,
    SpotDetail,
}

private fun myLocationButtonBottomPadding(
    sheetLevel: MapSheetLevel,
    shouldShowBottomSheet: Boolean,
) = if (!shouldShowBottomSheet) {
    MyLocationButtonBottomPadding
} else {
    when (sheetLevel) {
        MapSheetLevel.Hidden -> MyLocationButtonBottomPadding
        MapSheetLevel.Peek -> MapResultBottomSheetPeekHeight + MyLocationButtonBottomPadding
        MapSheetLevel.Medium -> MapSpotDetailBottomSheetPeekHeight + MyLocationButtonBottomPadding
        MapSheetLevel.Expanded -> MyLocationButtonBottomPadding
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
                isLocationPermissionGranted = true,
                locationTrackingMode = LocationTrackingMode.NoFollow,
                onLocationTrackingModeChange = {},
                onKeywordChanged = {},
                onSearchClick = {},
                onCurrentLocationClick = {},
                onMyLocationPermissionRequest = {},
                onLocationNoticeActionClick = {},
                onTypeClick = {},
                onSpotClick = {},
                onSpotFavoriteClick = {},
            )
        }
    }
}

private val MyLocationButtonHorizontalPadding = 16.dp
private val MyLocationButtonBottomPadding = 16.dp

private fun MapLocationNoticeAction.toIntent(packageName: String): Intent {
    return when (this) {
        MapLocationNoticeAction.OpenAppSettings -> Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        ).apply {
            data = Uri.fromParts("package", packageName, null)
        }

        MapLocationNoticeAction.OpenLocationSettings -> Intent(
            Settings.ACTION_LOCATION_SOURCE_SETTINGS,
        )
    }
}
