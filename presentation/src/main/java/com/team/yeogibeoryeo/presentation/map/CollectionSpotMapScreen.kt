package com.team.yeogibeoryeo.presentation.map

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
import com.team.yeogibeoryeo.domain.spot.model.MapRegionSearchCandidate
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.components.MessageSnackbar
import com.team.yeogibeoryeo.presentation.map.components.CollectionSpotNaverMap
import com.team.yeogibeoryeo.presentation.map.components.MapCenterSearchButton
import com.team.yeogibeoryeo.presentation.map.components.MapCurrentLocationGuideOverlay
import com.team.yeogibeoryeo.presentation.map.components.MapOverlayControls
import com.team.yeogibeoryeo.presentation.map.components.MapResultBottomSheetPeekHeight
import com.team.yeogibeoryeo.presentation.map.components.MapSearchLoadingOverlay
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
    mapLocationGuideViewModel: MapLocationGuideViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mapLocationGuideUiState by mapLocationGuideViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val favoriteUpdateFailedMessage = stringResource(R.string.favorite_update_failed_message)
    val currentFavoriteUpdateFailedMessage by rememberUpdatedState(favoriteUpdateFailedMessage)
    val hasFineLocationPermission = rememberFineLocationPermissionGranted()
    var hasGrantedLocationPermissionInSession by rememberSaveable {
        mutableStateOf(false)
    }
    var previousFineLocationPermission by rememberSaveable {
        mutableStateOf(hasFineLocationPermission)
    }
    var isLocationPermissionRequestBlocked by rememberSaveable {
        mutableStateOf(false)
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
            isLocationPermissionRequestBlocked = false
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
        onDenied = {
            isLocationPermissionRequestBlocked = false
            viewModel.onLocationPermissionDenied()
        },
        onBlocked = {
            isLocationPermissionRequestBlocked = true
            viewModel.onLocationPermissionDenied()
        },
    )
    LaunchedEffect(favoriteSpotMoveRequest, initialSpotType) {
        favoriteSpotMoveRequest?.let { request ->
            locationTrackingMode = LocationTrackingMode.NoFollow
            viewModel.showFavoriteSpot(request)
        } ?: run {
            viewModel.searchByCurrentLocationOnMapEntryIfPermitted(initialSpotType)
        }
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                CollectionSpotMapEvent.FavoriteUpdateFailed -> {
                    snackbarHostState.showSnackbar(currentFavoriteUpdateFailedMessage)
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        CollectionSpotMapContent(
            uiState = uiState,
            isLocationPermissionGranted = isLocationPermissionGranted,
            isLocationPermissionRequestBlocked = isLocationPermissionRequestBlocked,
            showCurrentLocationGuide = mapLocationGuideUiState.isVisible,
            locationTrackingMode = locationTrackingMode,
            onLocationTrackingModeChange = { mode ->
                locationTrackingMode = mode
            },
            onKeywordChanged = viewModel::onSearchKeywordChanged,
            onSearchClick = viewModel::searchByKeyword,
            onRegionCandidateClick = viewModel::onRegionSearchCandidateClick,
            onRegionDetailAllClick = viewModel::onRegionDetailSearchAllClick,
            onRegionDetailKeywordClick = viewModel::onRegionDetailSearchKeywordClick,
            onRegionDetailBackClick = viewModel::onRegionDetailSearchBack,
            onCurrentLocationClick = requestCurrentLocationSearch,
            onMapCenterSearchClick = viewModel::searchByMapCenter,
            onLocationNoticeActionClick = { action ->
                when (action) {
                    MapLocationNoticeAction.RequestLocationPermission -> requestCurrentLocationSearch()
                    MapLocationNoticeAction.OpenAppSettings,
                    MapLocationNoticeAction.OpenLocationSettings,
                    -> context.startActivity(action.toIntent(context.packageName))
                }
            },
            onCurrentLocationGuideDismiss = mapLocationGuideViewModel::dismissGuide,
            onTypeClick = viewModel::onSpotTypeClick,
            onClearTypeFiltersClick = viewModel::clearSpotTypeFilters,
            onSpotClick = viewModel::onSpotClick,
            onSpotDetailDismiss = viewModel::clearSelectedSpot,
            onSpotFavoriteClick = viewModel::onSpotFavoriteClick,
            onBottomBarVisibilityChanged = onBottomBarVisibilityChanged,
            onRegionalGuideClick = onRegionalGuideClick,
            modifier = Modifier.fillMaxSize(),
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) { snackbarData ->
            MessageSnackbar(
                message = snackbarData.visuals.message,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(FavoriteSnackbarIconSize),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                },
            )
        }
    }
}

@Composable
private fun CollectionSpotMapContent(
    uiState: CollectionSpotMapUiState,
    isLocationPermissionGranted: Boolean,
    isLocationPermissionRequestBlocked: Boolean,
    showCurrentLocationGuide: Boolean,
    locationTrackingMode: LocationTrackingMode,
    onLocationTrackingModeChange: (LocationTrackingMode) -> Unit,
    onKeywordChanged: (String) -> Unit,
    onSearchClick: () -> Unit,
    onRegionCandidateClick: (MapRegionSearchCandidate) -> Unit,
    onRegionDetailAllClick: () -> Unit,
    onRegionDetailKeywordClick: (String) -> Unit,
    onRegionDetailBackClick: () -> Unit,
    onCurrentLocationClick: () -> Unit,
    onMapCenterSearchClick: (Coordinate) -> Unit,
    onLocationNoticeActionClick: (MapLocationNoticeAction) -> Unit,
    onCurrentLocationGuideDismiss: () -> Unit,
    onTypeClick: (CollectionSpotType) -> Unit,
    onClearTypeFiltersClick: () -> Unit,
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
    var mapUiMode by remember { mutableStateOf(MapUiMode.Browsing) }
    var sheetLevel by remember { mutableStateOf(MapSheetLevel.Hidden) }
    var sheetRevealRequest by remember { mutableIntStateOf(0) }
    var mapCenterCoordinate by remember { mutableStateOf<Coordinate?>(null) }
    var shouldShowMapCenterSearchButton by remember { mutableStateOf(false) }
    var currentLocationButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var visibleSheetHeight by remember { mutableStateOf(0.dp) }
    val shouldDeferBottomSheetForGuide = showCurrentLocationGuide &&
        mapUiMode != MapUiMode.SpotDetail
    val shouldShowBottomSheet = uiState.shouldShowBottomSheet &&
        !isSpotSearchLoading &&
        !shouldDeferBottomSheetForGuide
    val shouldRenderBottomSheet =
        shouldShowBottomSheet && sheetLevel != MapSheetLevel.Hidden
    val selectedSpot = uiState.selectedSpot
    val selectedSpotMoveRequestSequence = uiState.favoriteSpotMoveRequestSequence
    val hasLocationNotice = uiState.locationNotice != null
    val hasNoticeOrError = hasLocationNotice ||
        uiState.errorMessageResId != null
    val hasRegionCandidates = uiState.regionSearchCandidates.isNotEmpty()
    val hasRegionDetailSelection = uiState.regionDetailSearchCandidate != null
    val hasRegionSelection = hasRegionCandidates || hasRegionDetailSelection
    val hasEmptyResult = uiState.hasSearched &&
        uiState.spots.isEmpty() &&
        !hasRegionSelection &&
        !hasNoticeOrError &&
        !uiState.isLoading
    val hasStateMessageContent = hasNoticeOrError ||
        hasEmptyResult ||
        (uiState.isLoading && !isSpotSearchLoading)
    val isNoticeOrErrorOnly = hasNoticeOrError &&
        uiState.spots.isEmpty() &&
        selectedSpot == null
    val mapLocationTrackingMode = when {
        !isLocationPermissionGranted -> LocationTrackingMode.None
        locationTrackingMode == LocationTrackingMode.None -> LocationTrackingMode.NoFollow
        else -> locationTrackingMode
    }
    val hasResultListToReturn = uiState.hasSearched ||
        uiState.spots.isNotEmpty() ||
        hasRegionSelection

    BackHandler(enabled = hasRegionDetailSelection) {
        onRegionDetailBackClick()
        if (!hasRegionCandidates) {
            mapUiMode = MapUiMode.Browsing
            sheetLevel = MapSheetLevel.Hidden
        }
    }

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

    BackHandler(
        enabled = mapUiMode == MapUiMode.ResultList &&
            sheetLevel != MapSheetLevel.Hidden &&
            shouldShowBottomSheet &&
            !hasRegionDetailSelection,
    ) {
        mapUiMode = MapUiMode.Browsing
        sheetLevel = MapSheetLevel.Hidden
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
        uiState.spots,
        uiState.errorMessageResId,
        uiState.locationNotice,
        uiState.regionSearchCandidates,
        uiState.regionDetailSearchCandidate,
        showCurrentLocationGuide,
    ) {
        if (shouldDeferBottomSheetForGuide) return@LaunchedEffect

        when {
            hasRegionSelection -> {
                mapUiMode = MapUiMode.ResultList
                sheetLevel = MapSheetLevel.Expanded
            }

            isSpotSearchLoading -> {
                mapUiMode = MapUiMode.Browsing
                sheetLevel = MapSheetLevel.Hidden
            }

            uiState.isLoading || hasNoticeOrError -> {
                mapUiMode = MapUiMode.ResultList
                sheetLevel = when {
                    hasLocationNotice -> MapSheetLevel.Medium
                    hasNoticeOrError -> MapSheetLevel.Expanded
                    else -> MapSheetLevel.Peek
                }
            }

            hasEmptyResult && mapUiMode != MapUiMode.SpotDetail -> {
                mapUiMode = MapUiMode.ResultList
                sheetLevel = MapSheetLevel.Expanded
            }

            uiState.hasSearched && mapUiMode == MapUiMode.Browsing -> {
                mapUiMode = MapUiMode.ResultList
                sheetLevel = MapSheetLevel.Half
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

    LaunchedEffect(shouldRenderBottomSheet, sheetLevel, mapUiMode, isNoticeOrErrorOnly) {
        val shouldHideBottomBar =
            shouldRenderBottomSheet &&
                sheetLevel != MapSheetLevel.Hidden &&
                mapUiMode != MapUiMode.Browsing &&
                !isNoticeOrErrorOnly

        onBottomBarVisibilityChanged(!shouldHideBottomBar)
    }

    LaunchedEffect(shouldRenderBottomSheet) {
        if (!shouldRenderBottomSheet) {
            visibleSheetHeight = 0.dp
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val density = LocalDensity.current
        val navigationBarBottomPadding = with(density) {
            WindowInsets.navigationBars.getBottom(density).toDp()
        }
        val safeDrawingBottomPadding = with(density) {
            WindowInsets.safeDrawing.getBottom(density).toDp()
        }
        val bottomContentPadding = maxOf(
            navigationBarBottomPadding,
            safeDrawingBottomPadding,
        )
        val bottomSheetMaxExpandedHeight = bottomSheetMaxExpandedHeight(
            mapUiMode = mapUiMode,
            hasRegionSelection = hasRegionSelection,
            hasStateMessageContent = hasStateMessageContent,
            maxHeight = maxHeight,
            bottomContentPadding = bottomContentPadding,
            regionCandidateCount = uiState.regionSearchCandidates.size,
            regionDetailCandidate = uiState.regionDetailSearchCandidate,
            canNavigateBackToRegionCandidates = hasRegionCandidates,
        )
        val searchBarTopPadding = with(density) {
            WindowInsets.statusBars.getTop(density).toDp()
        } + MapOverlayControlsTopPadding
        val naverLogoBottomPadding = naverLogoBottomPadding(
            shouldShowBottomSheet = shouldRenderBottomSheet,
            visibleSheetHeight = visibleSheetHeight,
        )
        val shouldShowMapOverlayControls = shouldShowMapOverlayControls(
            mapUiMode = mapUiMode,
            hasRegionSelection = hasRegionSelection,
            maxHeight = maxHeight,
            searchBarTopPadding = searchBarTopPadding,
            naverLogoBottomPadding = naverLogoBottomPadding,
        )

        CollectionSpotNaverMap(
            spots = uiState.spots,
            selectedSpot = uiState.selectedSpot,
            searchFocusCoordinate = uiState.searchFocusCoordinate,
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
                when (mapUiMode) {
                    MapUiMode.Browsing -> {
                        mapUiMode = MapUiMode.ResultList.takeIf { shouldShowBottomSheet } ?: MapUiMode.Browsing
                        sheetLevel = MapSheetLevel.Peek.takeIf { shouldShowBottomSheet } ?: MapSheetLevel.Hidden
                    }

                    MapUiMode.ResultList,
                    MapUiMode.SpotDetail,
                    -> {
                        mapUiMode = MapUiMode.Browsing
                        sheetLevel = MapSheetLevel.Hidden
                        onBottomBarVisibilityChanged(true)
                    }
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
            naverLogoBottomPadding = naverLogoBottomPadding,
            modifier = Modifier
                .fillMaxSize(),
        )

        if (shouldShowMapOverlayControls) {
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
                topPadding = searchBarTopPadding,
            )
        }

        if (
            shouldShowMapCenterSearchButton &&
            mapUiMode != MapUiMode.SpotDetail &&
            !hasRegionSelection &&
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
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = MyLocationButtonHorizontalPadding,
                        bottom = myLocationButtonBottomPadding(
                            sheetLevel = sheetLevel,
                            shouldShowBottomSheet = shouldRenderBottomSheet,
                            visibleSheetHeight = visibleSheetHeight,
                        ),
                    ),
            ) {
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
                        .onGloballyPositioned { coordinates ->
                            currentLocationButtonBounds = coordinates.boundsInRoot()
                        },
                )
            }
        }

        if (isSpotSearchLoading) {
            MapSearchLoadingOverlay(
                description = stringResource(uiState.searchMode.toLoadingDescriptionResId()),
            )
        }

        if (shouldRenderBottomSheet) {
            ThreeStepMapBottomSheet(
                sheetLevel = sheetLevel,
                revealKey = "$mapUiMode-${selectedSpot?.id}-$sheetRevealRequest",
                onSheetLevelChanged = { level ->
                    sheetLevel = level
                },
                modifier = Modifier.align(Alignment.BottomCenter),
                maxExpandedVisibleHeight = bottomSheetMaxExpandedHeight,
                onVisibleHeightChanged = { height ->
                    visibleSheetHeight = height
                },
            ) {
                when (mapUiMode) {
                    MapUiMode.SpotDetail -> {
                        if (selectedSpot != null) {
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
                                bottomContentPadding = bottomContentPadding,
                            )
                        }
                    }

                    MapUiMode.ResultList -> {
                        SpotBottomSheetContent(
                            spots = uiState.spots,
                            selectedSpot = selectedSpot,
                            isLoading = uiState.isLoading || uiState.isFavoriteSpotNearbyLoading,
                            hasSearched = uiState.hasSearched,
                            selectedTypes = uiState.selectedTypes,
                            isFilterResultEmpty = uiState.isFilterResultEmpty,
                            searchMode = uiState.searchMode,
                            regionSearchCandidates = uiState.regionSearchCandidates,
                            regionDetailSearchCandidate = uiState.regionDetailSearchCandidate,
                            locationNotice = uiState.locationNotice.withLocationPermissionActionFallback(
                                isLocationPermissionRequestBlocked = isLocationPermissionRequestBlocked,
                            ),
                            errorMessageResId = uiState.errorMessageResId,
                            partialWarningMessageResId = uiState.partialWarningMessageResId,
                            onTypeClick = onTypeClick,
                            onClearTypeFiltersClick = onClearTypeFiltersClick,
                            onRegionCandidateClick = onRegionCandidateClick,
                            onRegionDetailAllClick = onRegionDetailAllClick,
                            onRegionDetailKeywordClick = onRegionDetailKeywordClick,
                            onRegionDetailBackClick = onRegionDetailBackClick,
                            onLocationNoticeActionClick = onLocationNoticeActionClick,
                            onSpotFavoriteClick = onSpotFavoriteClick,
                            onSpotClick = { spot ->
                                onLocationTrackingModeChange(LocationTrackingMode.NoFollow)
                                mapUiMode = MapUiMode.SpotDetail
                                sheetLevel = MapSheetLevel.Medium
                                sheetRevealRequest += 1
                                onSpotClick(spot)
                            },
                            bottomContentPadding = bottomContentPadding,
                        )
                    }

                    MapUiMode.Browsing -> Unit
                }
            }
        }

        val guideTargetBounds = currentLocationButtonBounds
        if (
            showCurrentLocationGuide &&
            guideTargetBounds != null &&
            mapUiMode != MapUiMode.SpotDetail
        ) {
            MapCurrentLocationGuideOverlay(
                targetBounds = guideTargetBounds,
                onDismiss = onCurrentLocationGuideDismiss,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private val CollectionSpotMapUiState.shouldShowBottomSheet: Boolean
    get() = isLoading ||
        locationNotice != null ||
        errorMessageResId != null ||
        regionSearchCandidates.isNotEmpty() ||
        regionDetailSearchCandidate != null ||
        hasSearched ||
        spots.isNotEmpty() ||
        selectedSpot != null

private enum class MapUiMode {
    Browsing,
    ResultList,
    SpotDetail,
}

private fun bottomSheetMaxExpandedHeight(
    mapUiMode: MapUiMode,
    hasRegionSelection: Boolean,
    hasStateMessageContent: Boolean,
    maxHeight: Dp,
    bottomContentPadding: Dp,
    regionCandidateCount: Int,
    regionDetailCandidate: MapRegionSearchCandidate?,
    canNavigateBackToRegionCandidates: Boolean,
): Dp? {
    return when {
        mapUiMode == MapUiMode.SpotDetail -> null
        hasRegionSelection -> regionSelectionContentFitHeight(
            maxHeight = maxHeight,
            bottomContentPadding = bottomContentPadding,
            candidateCount = regionCandidateCount,
            detailCandidate = regionDetailCandidate,
            canNavigateBackToRegionCandidates = canNavigateBackToRegionCandidates,
        )
        hasStateMessageContent -> MapStateMessageBottomSheetMaxExpandedHeight
        else -> null
    }
}

private fun regionSelectionContentFitHeight(
    maxHeight: Dp,
    bottomContentPadding: Dp,
    candidateCount: Int,
    detailCandidate: MapRegionSearchCandidate?,
    canNavigateBackToRegionCandidates: Boolean,
): Dp {
    val contentHeight = if (detailCandidate == null) {
        MapBottomSheetHeaderEstimatedHeight +
            MapRegionSelectionDescriptionEstimatedHeight +
            MapRegionSelectionRowEstimatedHeight * candidateCount.toFloat() +
            bottomContentPadding +
            MapRegionSelectionBottomExtraPadding
    } else {
        val detailKeywordCount = detailCandidate.searchKeywords
            .filterNot { keyword -> keyword == detailCandidate.searchKeyword }
            .distinct()
            .size
        val backButtonHeight = if (canNavigateBackToRegionCandidates) {
            MapRegionDetailBackButtonEstimatedHeight
        } else {
            0.dp
        }

        MapBottomSheetHeaderEstimatedHeight +
            backButtonHeight +
            MapRegionDetailDescriptionEstimatedHeight +
            MapRegionSelectionRowEstimatedHeight * detailKeywordCount.toFloat() +
            MapRegionDetailAllRowEstimatedHeight +
            bottomContentPadding +
            MapRegionSelectionBottomExtraPadding
    }
    val maxContentFitHeight = maxHeight * MapRegionSelectionMaxExpandedRatio

    return contentHeight
        .coerceAtLeast(MapResultBottomSheetPeekHeight)
        .coerceAtMost(maxContentFitHeight)
}

private fun myLocationButtonBottomPadding(
    sheetLevel: MapSheetLevel,
    shouldShowBottomSheet: Boolean,
    visibleSheetHeight: Dp,
) = if (!shouldShowBottomSheet) {
    MyLocationButtonBottomPadding
} else {
    when (sheetLevel) {
        MapSheetLevel.Hidden -> MyLocationButtonBottomPadding
        MapSheetLevel.Peek -> maxOf(
            visibleSheetHeight,
            MapResultBottomSheetPeekHeight,
        ) + MyLocationButtonBottomPadding
        MapSheetLevel.Medium -> maxOf(
            visibleSheetHeight,
            MapSpotDetailBottomSheetPeekHeight,
        ) + MyLocationButtonBottomPadding
        MapSheetLevel.Half,
        MapSheetLevel.Expanded -> MyLocationButtonBottomPadding
    }
}

private fun naverLogoBottomPadding(
    shouldShowBottomSheet: Boolean,
    visibleSheetHeight: Dp,
) = if (!shouldShowBottomSheet) {
    NaverLogoBottomPadding
} else {
    visibleSheetHeight + NaverLogoBottomPadding
}

private fun shouldShowMapOverlayControls(
    mapUiMode: MapUiMode,
    hasRegionSelection: Boolean,
    maxHeight: Dp,
    searchBarTopPadding: Dp,
    naverLogoBottomPadding: Dp,
): Boolean {
    if (mapUiMode == MapUiMode.SpotDetail) return false
    if (hasRegionSelection) return false

    val naverLogoTop = maxHeight - naverLogoBottomPadding - NaverLogoEstimatedHeight
    val searchOverlayBottom =
        searchBarTopPadding + MapSearchBarMinHeight + MapSearchOverlayLogoGap

    return naverLogoTop > searchOverlayBottom
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
                isLocationPermissionRequestBlocked = false,
                showCurrentLocationGuide = false,
                locationTrackingMode = LocationTrackingMode.NoFollow,
                onLocationTrackingModeChange = {},
                onKeywordChanged = {},
                onSearchClick = {},
                onRegionCandidateClick = {},
                onRegionDetailAllClick = {},
                onRegionDetailKeywordClick = {},
                onRegionDetailBackClick = {},
                onCurrentLocationClick = {},
                onMapCenterSearchClick = {},
                onLocationNoticeActionClick = {},
                onCurrentLocationGuideDismiss = {},
                onTypeClick = {},
                onClearTypeFiltersClick = {},
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
private val NaverLogoBottomPadding = 16.dp
private val NaverLogoEstimatedHeight = 24.dp
private val MapSearchBarMinHeight = 56.dp
private val MapSearchOverlayLogoGap = 8.dp
private val MapOverlayControlsTopPadding = 2.dp
private val MapCenterSearchButtonTopPadding = 112.dp
private val FavoriteSnackbarIconSize = 20.dp
private const val MapRegionSelectionMaxExpandedRatio = 0.88f
private val MapBottomSheetHeaderEstimatedHeight = 57.dp
private val MapRegionSelectionDescriptionEstimatedHeight = 92.dp
private val MapRegionDetailDescriptionEstimatedHeight = 150.dp
private val MapRegionDetailBackButtonEstimatedHeight = 60.dp
private val MapRegionSelectionRowEstimatedHeight = 68.dp
private val MapRegionDetailAllRowEstimatedHeight = 92.dp
private val MapRegionSelectionBottomExtraPadding = 24.dp
private val MapStateMessageBottomSheetMaxExpandedHeight = 360.dp

private fun MapLocationNoticeAction.toIntent(packageName: String): Intent {
    return when (this) {
        MapLocationNoticeAction.RequestLocationPermission -> error(
            "RequestLocationPermission must be handled before creating an intent.",
        )
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

private fun MapLocationNotice?.withLocationPermissionActionFallback(
    isLocationPermissionRequestBlocked: Boolean,
): MapLocationNotice? {
    if (this != MapLocationNotices.PermissionDenied) return this

    return copy(
        action = if (isLocationPermissionRequestBlocked) {
            MapLocationNoticeAction.OpenAppSettings
        } else {
            MapLocationNoticeAction.RequestLocationPermission
        },
    )
}

private fun MapLocationNotice?.shouldRetryCurrentLocationSearchOnResume(): Boolean {
    return this == MapLocationNotices.PermissionDenied ||
        this == MapLocationNotices.LocationServiceDisabled ||
        this == MapLocationNotices.CurrentLocationUnavailable
}
