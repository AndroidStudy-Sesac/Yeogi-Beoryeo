package com.team.yeogibeoryeo.presentation.map

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.presentation.map.components.CollectionSpotNaverMap
import com.team.yeogibeoryeo.presentation.map.components.CurrentLocationSearchLoadingOverlay
import com.team.yeogibeoryeo.presentation.map.components.MapOverlayControls
import com.team.yeogibeoryeo.presentation.map.components.SpotBottomSheetContent
import com.team.yeogibeoryeo.presentation.map.components.SpotDetailBottomSheetContent
import com.team.yeogibeoryeo.presentation.map.location.rememberCurrentLocationSearchRequester
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun CollectionSpotMapScreen(
    modifier: Modifier = Modifier,
    viewModel: CollectionSpotMapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val requestCurrentLocationSearch = rememberCurrentLocationSearchRequester(
        onGranted = viewModel::searchByCurrentLocation,
        onDenied = viewModel::onLocationPermissionDenied,
    )

    CollectionSpotMapContent(
        uiState = uiState,
        onKeywordChanged = viewModel::onSearchKeywordChanged,
        onSearchClick = viewModel::searchByKeyword,
        onCurrentLocationClick = {
            requestCurrentLocationSearch()
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
    val isCurrentLocationSearching = uiState.isLoading &&
        uiState.searchMode == MapSearchMode.CURRENT_LOCATION
    val shouldShowBottomSheet = uiState.shouldShowBottomSheet && !isCurrentLocationSearching
    var mapUiMode by remember { mutableStateOf(MapUiMode.Browsing) }
    var sheetLevel by remember { mutableStateOf(MapSheetLevel.Hidden) }
    var sheetRevealRequest by remember { mutableStateOf(0) }
    val selectedSpot = uiState.selectedSpot

    LaunchedEffect(
        uiState.hasSearched,
        uiState.isLoading,
        uiState.searchMode,
        uiState.errorMessage,
        uiState.locationNoticeMessage,
    ) {
        when {
            isCurrentLocationSearching -> {
                mapUiMode = MapUiMode.Browsing
                sheetLevel = MapSheetLevel.Hidden
            }

            uiState.isLoading || uiState.errorMessage != null || uiState.locationNoticeMessage != null -> {
                mapUiMode = MapUiMode.ResultList
                sheetLevel = MapSheetLevel.Peek
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
            onSpotClick = { spot ->
                mapUiMode = MapUiMode.SpotDetail
                sheetLevel = MapSheetLevel.Medium
                sheetRevealRequest += 1
                onSpotClick(spot)
            },
            onMapClick = {
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
                    mapUiMode = MapUiMode.ResultList
                    sheetLevel = MapSheetLevel.Peek
                    onCurrentLocationClick()
                },
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
                            locationNoticeMessage = uiState.locationNoticeMessage,
                            errorMessage = uiState.errorMessage,
                            onTypeClick = onTypeClick,
                            onSpotClick = { spot ->
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

@Composable
private fun ThreeStepMapBottomSheet(
    sheetLevel: MapSheetLevel,
    revealKey: Any?,
    onSheetLevelChanged: (MapSheetLevel) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()
        val sheetHeight = maxHeight - MapSheetTopMargin
        val sheetHeightPx = with(density) {
            sheetHeight.toPx().coerceAtLeast(1f)
        }
        val hiddenOffset = with(density) {
            (sheetHeight - Dp.Hairline).toPx().coerceIn(0f, sheetHeightPx)
        }
        val peekOffset = with(density) {
            (sheetHeight - MapResultBottomSheetPeekHeight).toPx().coerceIn(0f, hiddenOffset)
        }
        val mediumOffset = with(density) {
            (sheetHeight - MapSpotDetailBottomSheetPeekHeight).toPx().coerceIn(0f, hiddenOffset)
        }
        val expandedOffset = 0f
        fun offsetFor(level: MapSheetLevel): Float =
            when (level) {
                MapSheetLevel.Hidden -> hiddenOffset
                MapSheetLevel.Peek -> peekOffset
                MapSheetLevel.Medium -> mediumOffset
                MapSheetLevel.Expanded -> expandedOffset
            }

        val targetOffset = offsetFor(sheetLevel)
        val sheetOffset = remember(sheetHeightPx) {
            Animatable(targetOffset)
        }

        LaunchedEffect(targetOffset, revealKey) {
            sheetOffset.animateTo(targetOffset)
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(sheetHeight)
                .offset {
                    IntOffset(x = 0, y = sheetOffset.value.roundToInt())
                }
                .pointerInput(sheetHeightPx, hiddenOffset, peekOffset, mediumOffset) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                sheetOffset.snapTo(
                                    (sheetOffset.value + dragAmount).coerceIn(
                                        expandedOffset,
                                        hiddenOffset,
                                    ),
                                )
                            }
                        },
                        onDragEnd = {
                            val nearestLevel = MapSheetLevel.entries.minBy { level ->
                                kotlin.math.abs(offsetFor(level) - sheetOffset.value)
                            }

                            onSheetLevelChanged(nearestLevel)
                            coroutineScope.launch {
                                sheetOffset.animateTo(offsetFor(nearestLevel))
                            }
                        },
                    )
                },
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

private val CollectionSpotMapUiState.shouldShowBottomSheet: Boolean
    get() = isLoading ||
        locationNoticeMessage != null ||
        errorMessage != null ||
        hasSearched ||
        spots.isNotEmpty()

private enum class MapUiMode {
    Browsing,
    ResultList,
    SpotDetail,
}

private enum class MapSheetLevel {
    Hidden,
    Peek,
    Medium,
    Expanded,
}

private val MapSheetTopMargin = 72.dp
private val MapStatusBottomSheetPeekHeight = 132.dp
private val MapResultBottomSheetPeekHeight = 144.dp
private val MapSpotDetailBottomSheetPeekHeight = 220.dp

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
