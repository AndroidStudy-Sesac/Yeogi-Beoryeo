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
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import com.team.yeogibeoryeo.presentation.map.components.MyLocationButton
import com.team.yeogibeoryeo.presentation.map.components.SpotBottomSheetContent
import com.team.yeogibeoryeo.presentation.map.components.SpotDetailBottomSheetContent
import com.team.yeogibeoryeo.presentation.map.components.ThreeStepMapBottomSheet
import com.team.yeogibeoryeo.presentation.map.location.rememberFineLocationPermissionGranted
import com.team.yeogibeoryeo.presentation.map.location.rememberCurrentLocationSearchRequester
import com.team.yeogibeoryeo.presentation.map.model.FavoriteSpotMapMoveRequest
import com.team.yeogibeoryeo.presentation.operationnotice.MapOperationNoticeViewModel
import com.team.yeogibeoryeo.presentation.operationnotice.OperationNoticeUiModel

@Composable
fun CollectionSpotMapScreen(
    modifier: Modifier = Modifier,
    initialSpotType: CollectionSpotType? = null,
    favoriteSpotMoveRequest: FavoriteSpotMapMoveRequest? = null,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
    onBottomBarInputEnabledChanged: (Boolean) -> Unit = {},
    onRegionalGuideClick: (String) -> Unit = {},
    viewModel: CollectionSpotMapViewModel = hiltViewModel(),
    mapLocationGuideViewModel: MapLocationGuideViewModel = hiltViewModel(),
    operationNoticeViewModel: MapOperationNoticeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mapLocationGuideUiState by mapLocationGuideViewModel.uiState.collectAsStateWithLifecycle()
    val operationNotice by operationNoticeViewModel.notice.collectAsStateWithLifecycle()
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
    val isLocationPermissionGranted =
        hasFineLocationPermission || hasGrantedLocationPermissionInSession
    val currentLocationNotice by rememberUpdatedState(uiState.locationNotice)
    val currentHasFineLocationPermission by rememberUpdatedState(hasFineLocationPermission)
    var locationTrackingMode by remember {
        mutableStateOf(LocationTrackingMode.None)
    }
    val showCurrentLocationGuide = mapLocationGuideUiState.isReady &&
        mapLocationGuideUiState.isVisible

    LaunchedEffect(hasFineLocationPermission) {
        if (!hasFineLocationPermission) {
            hasGrantedLocationPermissionInSession = false
            if (previousFineLocationPermission) {
                viewModel.onLocationPermissionRevoked()
            }
        } else if (!previousFineLocationPermission) {
            mapLocationGuideViewModel.clearLocationPermissionBlocked()
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
        hasRequestedFineLocationPermission = mapLocationGuideUiState.hasRequestedLocationPermission,
        onRequestLaunched = mapLocationGuideViewModel::markLocationPermissionRequested,
        onGranted = {
            hasGrantedLocationPermissionInSession = true
            previousFineLocationPermission = true
            mapLocationGuideViewModel.clearLocationPermissionBlocked()
            locationTrackingMode = LocationTrackingMode.NoFollow
            viewModel.searchByCurrentLocation()
        },
        onDenied = {
            viewModel.onLocationPermissionDenied()
        },
        onBlocked = {
            mapLocationGuideViewModel.markLocationPermissionBlocked()
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
            isLocationPermissionRequestBlocked =
                mapLocationGuideUiState.isLocationPermissionRequestBlocked,
            isCurrentLocationGuideReady = mapLocationGuideUiState.isReady,
            showCurrentLocationGuide = showCurrentLocationGuide,
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
            onRegionSearchBackClick = viewModel::onRegionSearchBack,
            onRegionBackCurrentLocationSheetHiddenClear =
                viewModel::clearRegionBackCurrentLocationSheetHidden,
            onCurrentLocationClick = requestCurrentLocationSearch,
            onBlockedCurrentLocationClick = viewModel::onLocationPermissionDenied,
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
            operationNotice = operationNotice,
            onOperationNoticeDismiss = operationNoticeViewModel::dismissNotice,
            onBottomBarVisibilityChanged = onBottomBarVisibilityChanged,
            onBottomBarInputEnabledChanged = onBottomBarInputEnabledChanged,
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
    isCurrentLocationGuideReady: Boolean,
    showCurrentLocationGuide: Boolean,
    locationTrackingMode: LocationTrackingMode,
    onLocationTrackingModeChange: (LocationTrackingMode) -> Unit,
    onKeywordChanged: (String) -> Unit,
    onSearchClick: () -> Unit,
    onRegionCandidateClick: (MapRegionSearchCandidate) -> Unit,
    onRegionDetailAllClick: () -> Unit,
    onRegionDetailKeywordClick: (String) -> Unit,
    onRegionDetailBackClick: () -> Unit,
    onRegionSearchBackClick: () -> Unit,
    onRegionBackCurrentLocationSheetHiddenClear: () -> Unit,
    onCurrentLocationClick: () -> Unit,
    onBlockedCurrentLocationClick: () -> Unit,
    onMapCenterSearchClick: (Coordinate) -> Unit,
    onLocationNoticeActionClick: (MapLocationNoticeAction) -> Unit,
    onCurrentLocationGuideDismiss: () -> Unit,
    onTypeClick: (CollectionSpotType) -> Unit,
    onClearTypeFiltersClick: () -> Unit,
    onSpotClick: (CollectionSpot) -> Unit,
    onSpotDetailDismiss: () -> Unit,
    onSpotFavoriteClick: (CollectionSpot) -> Unit,
    operationNotice: OperationNoticeUiModel? = null,
    onOperationNoticeDismiss: (String) -> Unit = {},
    onBottomBarVisibilityChanged: (Boolean) -> Unit,
    onBottomBarInputEnabledChanged: (Boolean) -> Unit,
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
    val currentLocationGuideTargetBounds = if (
        showCurrentLocationGuide &&
        mapUiMode != MapUiMode.SpotDetail
    ) {
        currentLocationButtonBounds
    } else {
        null
    }
    val shouldShowCurrentLocationGuideOverlay = currentLocationGuideTargetBounds != null
    val shouldDeferBottomSheetForGuide = (!isCurrentLocationGuideReady || showCurrentLocationGuide) &&
        mapUiMode != MapUiMode.SpotDetail
    val hasOperationNotice = operationNotice != null
    val shouldShowBottomSheet = (uiState.shouldShowBottomSheet || hasOperationNotice) &&
        !isSpotSearchLoading &&
        !shouldDeferBottomSheetForGuide
    val shouldRenderBottomSheet =
        shouldShowBottomSheet && sheetLevel != MapSheetLevel.Hidden
    val selectedSpot = uiState.selectedSpot
    val selectedSpotMoveRequestSequence = uiState.favoriteSpotMoveRequestSequence
    val hasLocationNotice = uiState.locationNotice != null
    val hasNoticeOrError = hasLocationNotice ||
        hasOperationNotice ||
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

    BackHandler(enabled = hasRegionCandidates && !hasRegionDetailSelection) {
        onRegionSearchBackClick()
        mapUiMode = MapUiMode.Browsing
        sheetLevel = MapSheetLevel.Hidden
    }

    BackHandler(enabled = mapUiMode == MapUiMode.SpotDetail && selectedSpot != null) {
        onSpotDetailDismiss()
        val returnState =
            mapDetailCloseReturnState(
                hasResultListToReturn = hasResultListToReturn,
                hasOperationNotice = hasOperationNotice,
            )
        mapUiMode = returnState.mapUiMode
        sheetLevel = returnState.sheetLevel
    }

    BackHandler(
        enabled = mapUiMode == MapUiMode.ResultList &&
            sheetLevel != MapSheetLevel.Hidden &&
            shouldShowBottomSheet &&
            !hasRegionSelection,
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
        operationNotice?.id,
        uiState.regionSearchCandidates,
        uiState.regionDetailSearchCandidate,
        isCurrentLocationGuideReady,
        showCurrentLocationGuide,
        uiState.shouldKeepCurrentLocationSheetHiddenAfterRegionBack,
    ) {
        if (shouldDeferBottomSheetForGuide) return@LaunchedEffect
        if (
            shouldKeepSpotDetailOnOperationNotice(
                mapUiMode = mapUiMode,
                hasOperationNotice = hasOperationNotice,
                hasLocationNotice = hasLocationNotice,
                hasError = uiState.errorMessageResId != null,
                isLoading = uiState.isLoading,
            )
        ) {
            return@LaunchedEffect
        }

        when {
            hasRegionSelection -> {
                mapUiMode = MapUiMode.ResultList
                sheetLevel = MapSheetLevel.Expanded
            }

            shouldKeepCurrentLocationSheetHiddenAfterRegionBack(
                shouldKeepCurrentLocationSheetHiddenAfterRegionBack =
                    uiState.shouldKeepCurrentLocationSheetHiddenAfterRegionBack,
                mapUiMode = mapUiMode,
                searchMode = uiState.searchMode,
                hasNoticeOrError = hasNoticeOrError,
                hasRegionSelection = hasRegionSelection,
            ) -> {
                mapUiMode = MapUiMode.Browsing
                sheetLevel = MapSheetLevel.Hidden
            }

            isSpotSearchLoading -> {
                mapUiMode = MapUiMode.Browsing
                sheetLevel = MapSheetLevel.Hidden
            }

            uiState.isLoading || hasNoticeOrError -> {
                mapUiMode = MapUiMode.ResultList
                sheetLevel = when {
                    hasLocationNotice || hasOperationNotice -> MapSheetLevel.Medium
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
            val returnState =
                mapDetailCloseReturnState(
                    hasResultListToReturn = uiState.hasSearched,
                    hasOperationNotice = hasOperationNotice,
                )
            mapUiMode = returnState.mapUiMode
            sheetLevel = returnState.sheetLevel
        }
    }

    LaunchedEffect(selectedSpotMoveRequestSequence) {
        if (selectedSpotMoveRequestSequence > 0 && selectedSpot != null) {
            mapUiMode = MapUiMode.SpotDetail
            sheetLevel = MapSheetLevel.Medium
            sheetRevealRequest += 1
        }
    }

    LaunchedEffect(shouldRenderBottomSheet, sheetLevel, mapUiMode) {
        val shouldHideBottomBar =
            shouldRenderBottomSheet &&
                sheetLevel != MapSheetLevel.Hidden &&
                mapUiMode != MapUiMode.Browsing

        onBottomBarVisibilityChanged(!shouldHideBottomBar)
    }

    LaunchedEffect(shouldShowCurrentLocationGuideOverlay) {
        if (shouldShowCurrentLocationGuideOverlay) {
            onBottomBarVisibilityChanged(true)
        }
        onBottomBarInputEnabledChanged(!shouldShowCurrentLocationGuideOverlay)
    }

    DisposableEffect(Unit) {
        onDispose {
            onBottomBarInputEnabledChanged(true)
        }
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
        val bottomSheetMaxExpandedHeight = MapBottomSheetHeightPolicy.maxExpandedHeight(
            mapUiMode = mapUiMode,
            hasRegionSelection = hasRegionSelection,
            hasStateMessageContent = hasStateMessageContent,
            maxHeight = maxHeight,
            bottomContentPadding = bottomContentPadding,
            regionCandidateCount = uiState.regionSearchCandidates.size,
            regionDetailCandidate = uiState.regionDetailSearchCandidate,
            canNavigateBackToRegionCandidates = hasRegionCandidates,
            fontScale = density.fontScale,
        )
        val bottomSheetMediumVisibleHeight = MapBottomSheetHeightPolicy.mediumVisibleHeight(
            hasStateMessageContent = hasStateMessageContent,
            maxHeight = maxHeight,
            bottomContentPadding = bottomContentPadding,
            fontScale = density.fontScale,
        )
        val searchBarTopPadding = with(density) {
            WindowInsets.statusBars.getTop(density).toDp()
        } + MapOverlayControlsTopPadding
        val naverLogoBottomPadding = naverLogoBottomPadding(
            shouldShowBottomSheet = shouldRenderBottomSheet,
            visibleSheetHeight = visibleSheetHeight,
            bottomContentPadding = bottomContentPadding,
        )
        val shouldShowMapOverlayControls = shouldShowMapOverlayControls(
            mapUiMode = mapUiMode,
            hasRegionSelection = hasRegionSelection,
            maxHeight = maxHeight,
            searchBarTopPadding = searchBarTopPadding,
            naverLogoBottomPadding = naverLogoBottomPadding,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (shouldShowCurrentLocationGuideOverlay) {
                        Modifier.clearAndSetSemantics { }
                    } else {
                        Modifier
                    },
                ),
        ) {
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
                    onRegionBackCurrentLocationSheetHiddenClear()
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
                        onRegionBackCurrentLocationSheetHiddenClear()
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
                        onRegionBackCurrentLocationSheetHiddenClear()
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
                                mediumVisibleHeight = bottomSheetMediumVisibleHeight,
                                bottomContentPadding = bottomContentPadding,
                            ),
                        ),
                ) {
                    MyLocationButton(
                        isTracking = mapLocationTrackingMode == LocationTrackingMode.Follow,
                        onClick = {
                            onRegionBackCurrentLocationSheetHiddenClear()
                            if (isLocationPermissionGranted) {
                                onLocationTrackingModeChange(LocationTrackingMode.NoFollow)
                                shouldShowMapCenterSearchButton = false
                                mapUiMode = MapUiMode.ResultList
                                sheetLevel = MapSheetLevel.Peek
                                onCurrentLocationClick()
                            } else {
                                if (isLocationPermissionRequestBlocked) {
                                    mapUiMode = MapUiMode.ResultList
                                    sheetLevel = MapSheetLevel.Medium
                                    sheetRevealRequest += 1
                                    onBlockedCurrentLocationClick()
                                } else {
                                    onCurrentLocationClick()
                                }
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
                    mediumVisibleHeight = bottomSheetMediumVisibleHeight,
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
                                        val returnState =
                                            mapDetailCloseReturnState(
                                                hasResultListToReturn = true,
                                                hasOperationNotice = hasOperationNotice,
                                            )
                                        mapUiMode = returnState.mapUiMode
                                        sheetLevel = returnState.sheetLevel
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
                                operationNotice = operationNotice,
                                errorMessageResId = uiState.errorMessageResId,
                                partialWarningMessageResId = uiState.partialWarningMessageResId,
                                onTypeClick = onTypeClick,
                                onClearTypeFiltersClick = onClearTypeFiltersClick,
                                onRegionCandidateClick = onRegionCandidateClick,
                                onRegionDetailAllClick = onRegionDetailAllClick,
                                onRegionDetailKeywordClick = onRegionDetailKeywordClick,
                                onRegionDetailBackClick = onRegionDetailBackClick,
                                onLocationNoticeActionClick = onLocationNoticeActionClick,
                                onOperationNoticeDismiss = onOperationNoticeDismiss,
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
        }

        currentLocationGuideTargetBounds?.let { guideTargetBounds ->
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

internal enum class MapUiMode {
    Browsing,
    ResultList,
    SpotDetail,
}

internal fun shouldKeepSpotDetailOnOperationNotice(
    mapUiMode: MapUiMode,
    hasOperationNotice: Boolean,
    hasLocationNotice: Boolean,
    hasError: Boolean,
    isLoading: Boolean,
): Boolean =
    mapUiMode == MapUiMode.SpotDetail &&
        hasOperationNotice &&
        !hasLocationNotice &&
        !hasError &&
        !isLoading

internal fun shouldKeepCurrentLocationSheetHiddenAfterRegionBack(
    shouldKeepCurrentLocationSheetHiddenAfterRegionBack: Boolean,
    mapUiMode: MapUiMode,
    searchMode: MapSearchMode,
    hasNoticeOrError: Boolean,
    hasRegionSelection: Boolean,
): Boolean =
    shouldKeepCurrentLocationSheetHiddenAfterRegionBack &&
        mapUiMode != MapUiMode.SpotDetail &&
        searchMode == MapSearchMode.CURRENT_LOCATION &&
        !hasNoticeOrError &&
        !hasRegionSelection

internal data class MapDetailReturnState(
    val mapUiMode: MapUiMode,
    val sheetLevel: MapSheetLevel,
)

internal fun mapDetailCloseReturnState(
    hasResultListToReturn: Boolean,
    hasOperationNotice: Boolean,
): MapDetailReturnState =
    when {
        hasResultListToReturn || hasOperationNotice ->
            MapDetailReturnState(
                mapUiMode = MapUiMode.ResultList,
                sheetLevel = if (hasOperationNotice) {
                    MapSheetLevel.Medium
                } else {
                    MapSheetLevel.Peek
                },
            )

        else ->
            MapDetailReturnState(
                mapUiMode = MapUiMode.Browsing,
                sheetLevel = MapSheetLevel.Hidden,
            )
    }

private fun myLocationButtonBottomPadding(
    sheetLevel: MapSheetLevel,
    shouldShowBottomSheet: Boolean,
    visibleSheetHeight: Dp,
    mediumVisibleHeight: Dp,
    bottomContentPadding: Dp,
) = if (!shouldShowBottomSheet) {
    MyLocationButtonBottomPadding
} else {
    val sheetBottomPadding = bottomContentPadding + MyLocationButtonBottomPadding

    when (sheetLevel) {
        MapSheetLevel.Hidden -> MyLocationButtonBottomPadding
        MapSheetLevel.Peek -> maxOf(
            visibleSheetHeight,
            MapResultBottomSheetPeekHeight,
        ) + sheetBottomPadding
        MapSheetLevel.Medium -> maxOf(
            visibleSheetHeight,
            mediumVisibleHeight,
        ) + sheetBottomPadding
        MapSheetLevel.Half,
        MapSheetLevel.Expanded -> MyLocationButtonBottomPadding
    }
}

private fun naverLogoBottomPadding(
    shouldShowBottomSheet: Boolean,
    visibleSheetHeight: Dp,
    bottomContentPadding: Dp,
) = if (!shouldShowBottomSheet) {
    NaverLogoBottomPadding
} else {
    visibleSheetHeight + bottomContentPadding + NaverLogoBottomPadding
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
                isCurrentLocationGuideReady = true,
                showCurrentLocationGuide = false,
                locationTrackingMode = LocationTrackingMode.NoFollow,
                onLocationTrackingModeChange = {},
                onKeywordChanged = {},
                onSearchClick = {},
                onRegionCandidateClick = {},
                onRegionDetailAllClick = {},
                onRegionDetailKeywordClick = {},
                onRegionDetailBackClick = {},
                onRegionSearchBackClick = {},
                onRegionBackCurrentLocationSheetHiddenClear = {},
                onCurrentLocationClick = {},
                onBlockedCurrentLocationClick = {},
                onMapCenterSearchClick = {},
                onLocationNoticeActionClick = {},
                onCurrentLocationGuideDismiss = {},
                onTypeClick = {},
                onClearTypeFiltersClick = {},
                onSpotClick = {},
                onSpotDetailDismiss = {},
                onSpotFavoriteClick = {},
                onBottomBarVisibilityChanged = {},
                onBottomBarInputEnabledChanged = {},
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
