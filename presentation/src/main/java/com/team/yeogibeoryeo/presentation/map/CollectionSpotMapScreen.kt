package com.team.yeogibeoryeo.presentation.map

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naver.maps.map.compose.LocationTrackingMode
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.map.components.CollectionSpotNaverMap
import com.team.yeogibeoryeo.presentation.map.components.MapSearchLoadingOverlay
import com.team.yeogibeoryeo.presentation.map.components.MapCenterSearchButton
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
import com.team.yeogibeoryeo.presentation.map.model.FavoriteSpotMapMoveRequest

@Composable
fun CollectionSpotMapScreen(
    modifier: Modifier = Modifier,
    initialSpotType: CollectionSpotType? = null,
    favoriteSpotMoveRequest: FavoriteSpotMapMoveRequest? = null,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
    onRegionalGuideClick: (String) -> Unit = {},
    viewModel: CollectionSpotMapViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hasFineLocationPermission = rememberFineLocationPermissionGranted()
    var hasGrantedLocationPermissionInSession by rememberSaveable {
        mutableStateOf(false)
    }
    var previousFineLocationPermission by rememberSaveable {
        mutableStateOf(hasFineLocationPermission)
    }
    val isLocationPermissionGranted =
        hasFineLocationPermission || hasGrantedLocationPermissionInSession
    val currentLocationNotice by rememberUpdatedState(uiState.locationNotice)
    val currentHasFineLocationPermission by rememberUpdatedState(hasFineLocationPermission)
    var locationTrackingMode by remember {
        mutableStateOf(LocationTrackingMode.None)
    }

    LaunchedEffect(hasFineLocationPermission) {
        if (!hasFineLocationPermission) {
            hasGrantedLocationPermissionInSession = false
            if (previousFineLocationPermission) {
                viewModel.onLocationPermissionRevoked()
            }
        } else if (!previousFineLocationPermission) {
            locationTrackingMode = LocationTrackingMode.NoFollow
            viewModel.searchByCurrentLocation()
        }
        previousFineLocationPermission = hasFineLocationPermission
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (
                event == Lifecycle.Event.ON_RESUME &&
                currentHasFineLocationPermission &&
                currentLocationNotice.shouldRetryCurrentLocationSearchOnResume()
            ) {
                previousFineLocationPermission = true
                locationTrackingMode = LocationTrackingMode.NoFollow
                viewModel.searchByCurrentLocation()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val requestCurrentLocationSearch = rememberCurrentLocationSearchRequester(
        onGranted = {
            hasGrantedLocationPermissionInSession = true
            previousFineLocationPermission = true
            locationTrackingMode = LocationTrackingMode.NoFollow
            viewModel.searchByCurrentLocation()
        },
        onDenied = viewModel::onLocationPermissionDenied,
    )
    LaunchedEffect(favoriteSpotMoveRequest, initialSpotType) {
        val request = favoriteSpotMoveRequest
        if (request == null) {
            viewModel.searchByCurrentLocationOnMapEntryIfPermitted(initialSpotType)
        } else {
            locationTrackingMode = LocationTrackingMode.NoFollow
            viewModel.showFavoriteSpot(request)
        }
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
        onMapCenterSearchClick = viewModel::searchByMapCenter,
        onLocationNoticeActionClick = { action ->
            context.startActivity(action.toIntent(context.packageName))
        },
        onTypeClick = viewModel::onSpotTypeClick,
        onSpotClick = viewModel::onSpotClick,
        onSpotDetailDismiss = viewModel::clearSelectedSpot,
        onSpotFavoriteClick = viewModel::onSpotFavoriteClick,
        onBottomBarVisibilityChanged = onBottomBarVisibilityChanged,
        onRegionalGuideClick = onRegionalGuideClick,
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
    onMapCenterSearchClick: (Coordinate) -> Unit,
    onLocationNoticeActionClick: (MapLocationNoticeAction) -> Unit,
    onTypeClick: (CollectionSpotType) -> Unit,
    onSpotClick: (CollectionSpot) -> Unit,
    onSpotDetailDismiss: () -> Unit,
    onSpotFavoriteClick: (CollectionSpot) -> Unit,
    onBottomBarVisibilityChanged: (Boolean) -> Unit,
    onRegionalGuideClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSpotSearchLoading = uiState.isLoading &&
        uiState.searchMode in setOf(
            MapSearchMode.KEYWORD,
            MapSearchMode.CURRENT_LOCATION,
            MapSearchMode.MAP_CENTER,
        )
    val shouldShowBottomSheet = uiState.shouldShowBottomSheet && !isSpotSearchLoading
    var mapUiMode by remember { mutableStateOf(MapUiMode.Browsing) }
    var sheetLevel by remember { mutableStateOf(MapSheetLevel.Hidden) }
    var sheetRevealRequest by remember { mutableIntStateOf(0) }
    var mapCenterCoordinate by remember { mutableStateOf<Coordinate?>(null) }
    var shouldShowMapCenterSearchButton by remember { mutableStateOf(false) }
    val selectedSpot = uiState.selectedSpot
    val selectedSpotMoveRequestSequence = uiState.favoriteSpotMoveRequestSequence
    val hasNoticeOrError = uiState.locationNotice != null ||
        uiState.locationNoticeMessage != null ||
        uiState.errorMessage != null
    val mapLocationTrackingMode = when {
        !isLocationPermissionGranted -> LocationTrackingMode.None
        locationTrackingMode == LocationTrackingMode.None -> LocationTrackingMode.NoFollow
        else -> locationTrackingMode
    }
    val hasResultListToReturn = uiState.hasSearched || uiState.spots.isNotEmpty()

    BackHandler(enabled = mapUiMode == MapUiMode.SpotDetail && selectedSpot != null) {
        onSpotDetailDismiss()
        mapUiMode = if (hasResultListToReturn) {
            sheetLevel = MapSheetLevel.Peek
            MapUiMode.ResultList
        } else {
            sheetLevel = MapSheetLevel.Hidden
            MapUiMode.Browsing
        }
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
            isSpotSearchLoading -> {
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

    LaunchedEffect(selectedSpotMoveRequestSequence) {
        if (selectedSpotMoveRequestSequence > 0 && selectedSpot != null) {
            mapUiMode = MapUiMode.SpotDetail
            sheetLevel = MapSheetLevel.Medium
            sheetRevealRequest += 1
        }
    }

    LaunchedEffect(shouldShowBottomSheet, sheetLevel, mapUiMode) {
        val shouldHideBottomBar =
            shouldShowBottomSheet &&
                sheetLevel != MapSheetLevel.Hidden &&
                mapUiMode != MapUiMode.Browsing

        onBottomBarVisibilityChanged(!shouldHideBottomBar)
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        val density = LocalDensity.current
        val navigationBarBottomPadding = with(density) {
            WindowInsets.navigationBars.getBottom(density).toDp()
        }

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
                    onBottomBarVisibilityChanged(true)
                }
            },
            onCameraCenterChanged = { coordinate ->
                mapCenterCoordinate = coordinate
            },
            onUserCameraMove = {
                onLocationTrackingModeChange(LocationTrackingMode.NoFollow)
                if (!uiState.isLoading && mapUiMode != MapUiMode.SpotDetail) {
                    shouldShowMapCenterSearchButton = true
                }
            },
            modifier = Modifier
                .fillMaxSize(),
        )

        if (mapUiMode != MapUiMode.SpotDetail) {
            MapOverlayControls(
                keyword = uiState.searchKeyword,
                onKeywordChanged = onKeywordChanged,
                onSearchClick = {
                    onLocationTrackingModeChange(LocationTrackingMode.NoFollow)
                    shouldShowMapCenterSearchButton = false
                    mapUiMode = MapUiMode.ResultList
                    sheetLevel = MapSheetLevel.Peek
                    onSearchClick()
                },
            )
        }

        if (
            shouldShowMapCenterSearchButton &&
            mapUiMode != MapUiMode.SpotDetail &&
            !uiState.isLoading
        ) {
            MapCenterSearchButton(
                onClick = {
                    val coordinate = mapCenterCoordinate ?: return@MapCenterSearchButton
                    shouldShowMapCenterSearchButton = false
                    onLocationTrackingModeChange(LocationTrackingMode.NoFollow)
                    mapUiMode = MapUiMode.ResultList
                    sheetLevel = MapSheetLevel.Peek
                    onMapCenterSearchClick(coordinate)
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = MapCenterSearchButtonTopPadding),
            )
        }

        if (mapUiMode != MapUiMode.SpotDetail) {
            MyLocationButton(
                isTracking = mapLocationTrackingMode == LocationTrackingMode.Follow,
                onClick = {
                    if (isLocationPermissionGranted) {
                        onLocationTrackingModeChange(LocationTrackingMode.NoFollow)
                        shouldShowMapCenterSearchButton = false
                        mapUiMode = MapUiMode.ResultList
                        sheetLevel = MapSheetLevel.Peek
                        onCurrentLocationClick()
                    } else {
                        onCurrentLocationClick()
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

        if (isSpotSearchLoading) {
            MapSearchLoadingOverlay(
                description = stringResource(uiState.searchMode.toLoadingDescriptionResId()),
            )
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
                            isNearbyLoading = uiState.isFavoriteSpotNearbyLoading,
                            onFavoriteClick = onSpotFavoriteClick,
                            onRegionalGuideClick = onRegionalGuideClick,
                            onCloseClick = {
                                onSpotDetailDismiss()
                                mapUiMode = MapUiMode.ResultList
                                sheetLevel = MapSheetLevel.Peek
                            },
                            bottomContentPadding = navigationBarBottomPadding,
                        )
                    }

                    mapUiMode == MapUiMode.ResultList -> {
                        SpotBottomSheetContent(
                            spots = uiState.spots,
                            selectedSpot = selectedSpot,
                            isLoading = uiState.isLoading || uiState.isFavoriteSpotNearbyLoading,
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
                            bottomContentPadding = navigationBarBottomPadding,
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
        spots.isNotEmpty() ||
        selectedSpot != null

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
        MapSheetLevel.Half,
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
                onMapCenterSearchClick = {},
                onLocationNoticeActionClick = {},
                onTypeClick = {},
                onSpotClick = {},
                onSpotDetailDismiss = {},
                onSpotFavoriteClick = {},
                onBottomBarVisibilityChanged = {},
                onRegionalGuideClick = {},
            )
        }
    }

}

@StringRes
private fun MapSearchMode.toLoadingDescriptionResId(): Int {
    return when (this) {
        MapSearchMode.KEYWORD -> R.string.map_search_loading_keyword
        MapSearchMode.CURRENT_LOCATION -> R.string.map_search_loading_current_location
        MapSearchMode.MAP_CENTER -> R.string.map_search_loading_map_center
    }
}

private val MyLocationButtonHorizontalPadding = 16.dp
private val MyLocationButtonBottomPadding = 16.dp
private val MapCenterSearchButtonTopPadding = 112.dp

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

private fun MapLocationNotice?.shouldRetryCurrentLocationSearchOnResume(): Boolean {
    return this == MapLocationNotices.PermissionDenied ||
        this == MapLocationNotices.LocationServiceDisabled ||
        this == MapLocationNotices.CurrentLocationUnavailable
}
