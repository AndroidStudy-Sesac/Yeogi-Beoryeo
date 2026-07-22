package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.clustering.ClusterMarkerInfo
import com.naver.maps.map.clustering.ClusterMarkerUpdater
import com.naver.maps.map.clustering.Clusterer
import com.naver.maps.map.clustering.DefaultClusterMarkerUpdater
import com.naver.maps.map.clustering.DefaultLeafMarkerUpdater
import com.naver.maps.map.clustering.LeafMarkerInfo
import com.naver.maps.map.clustering.LeafMarkerUpdater
import com.naver.maps.map.compose.DisposableMapEffect
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.LocationTrackingMode
import com.naver.maps.map.compose.MapProperties
import com.naver.maps.map.compose.MapUiSettings
import com.naver.maps.map.compose.Marker
import com.naver.maps.map.compose.MarkerState
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.rememberCameraPositionState
import com.naver.maps.map.overlay.Overlay
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.presentation.map.location.rememberMapLocationSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun CollectionSpotNaverMap(
    spots: List<CollectionSpot>,
    selectedSpot: CollectionSpot?,
    searchFocusCoordinate: Coordinate?,
    isLocationPermissionGranted: Boolean,
    locationTrackingMode: LocationTrackingMode,
    onSpotClick: (CollectionSpot) -> Unit,
    onMapClick: () -> Unit,
    onCameraCenterChanged: (Coordinate) -> Unit,
    onUserCameraMove: () -> Unit,
    modifier: Modifier = Modifier,
    naverLogoBottomPadding: Dp = NaverLogoDefaultBottomPadding,
) {
    val defaultMarkerColor = MaterialTheme.colorScheme.primary
    val selectedMarkerColor = MaterialTheme.colorScheme.tertiary
    val defaultMarkerColorArgb = defaultMarkerColor.toArgb()
    val markerRenderState = remember(spots, selectedSpot) {
        buildCollectionSpotMarkerRenderState(
            spots = spots,
            selectedSpot = selectedSpot,
        )
    }
    val currentOnSpotClick by rememberUpdatedState(onSpotClick)
    val coroutineScope = rememberCoroutineScope()
    val locationSource = rememberMapLocationSource()
    val mapProperties = MapProperties(
        locationTrackingMode = if (isLocationPermissionGranted) {
            locationTrackingMode
        } else {
            LocationTrackingMode.None
        },
    )
    val mapUiSettings = MapUiSettings(
        isLogoClickEnabled = false,
        logoMargin = PaddingValues(
            start = NaverLogoHorizontalPadding,
            top = NaverLogoTopPadding,
            end = NaverLogoHorizontalPadding,
            bottom = naverLogoBottomPadding,
        ),
    )

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(
            DEFAULT_LOCATION,
            DEFAULT_ZOOM,
        )
    }
    var isProgrammaticCameraMove by remember { mutableStateOf(false) }
    suspend fun moveCamera(update: CameraUpdate) {
        isProgrammaticCameraMove = true
        cameraPositionState.move(update)
        repeat(PROGRAMMATIC_CAMERA_MOVE_GUARD_FRAMES) {
            withFrameNanos { }
        }
        isProgrammaticCameraMove = false
    }

    LaunchedEffect(Unit) {
        snapshotFlow { cameraPositionState.position.toCameraSnapshot() }
            .drop(1)
            .collect { cameraSnapshot ->
                onCameraCenterChanged(cameraSnapshot.toCoordinate())
                if (!isProgrammaticCameraMove) {
                    onUserCameraMove()
                }
            }
    }

    LaunchedEffect(spots, searchFocusCoordinate) {
        if (selectedSpot != null) return@LaunchedEffect

        val coordinates = spots.mapNotNull { spot -> spot.coordinate }

        when (coordinates.size) {
            0 -> {
                val coordinate = searchFocusCoordinate ?: return@LaunchedEffect
                moveCamera(
                    CameraUpdate.scrollAndZoomTo(
                        LatLng(
                            coordinate.latitude,
                            coordinate.longitude,
                        ),
                        SEARCH_RESULT_ZOOM,
                    ),
                )
            }

            1 -> {
                val coordinate = coordinates.single()
                moveCamera(
                    CameraUpdate.scrollAndZoomTo(
                        LatLng(
                            coordinate.latitude,
                            coordinate.longitude,
                        ),
                        SEARCH_RESULT_ZOOM,
                    ),
                )
            }

            else -> {
                moveCamera(
                    CameraUpdate.fitBounds(
                        coordinates.toLatLngBounds(),
                        SEARCH_RESULT_BOUNDS_PADDING,
                    ),
                )
            }
        }
    }

    LaunchedEffect(selectedSpot?.id) {
        val coordinate = selectedSpot?.coordinate

        if (coordinate != null) {
            moveCamera(
                CameraUpdate.scrollAndZoomTo(
                    LatLng(
                        coordinate.latitude,
                        coordinate.longitude,
                    ),
                    SELECTED_SPOT_ZOOM,
                ),
            )
        }
    }

    NaverMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = mapUiSettings,
        locationSource = locationSource.takeIf {
            isLocationPermissionGranted
        },
        onMapClick = { _, _ ->
            onMapClick()
        },
    ) {
        if (markerRenderState.useClustering) {
            CollectionSpotClusterOverlay(
                spots = markerRenderState.clusterMarkerSpots,
                markerColor = defaultMarkerColorArgb,
                onSpotClick = { spot ->
                    currentOnSpotClick(spot)
                },
                onClusterClick = { clusterPosition, currentZoom, maxZoom ->
                    coroutineScope.launch {
                        moveCamera(
                            CameraUpdate.scrollAndZoomTo(
                                clusterPosition,
                                (currentZoom + CLUSTER_CLICK_ZOOM_INCREMENT)
                                    .coerceAtMost(maxZoom),
                            ),
                        )
                    }
                },
            )
        }

        markerRenderState.composeMarkerSpots.forEach { spot ->
            val coordinate = spot.coordinate ?: return@forEach
            val isSelected = selectedSpot?.id == spot.id

            Marker(
                state = MarkerState(
                    position = LatLng(
                        coordinate.latitude,
                        coordinate.longitude,
                    ),
                ),
                captionText = spot.name,
                iconTintColor = if (isSelected) {
                    selectedMarkerColor
                } else {
                    defaultMarkerColor
                },
                zIndex = if (isSelected) {
                    SELECTED_MARKER_Z_INDEX
                } else {
                    DEFAULT_MARKER_Z_INDEX
                },
                onClick = {
                    onSpotClick(spot)
                    true
                },
            )
        }
    }
}

@OptIn(ExperimentalNaverMapApi::class)
@Composable
private fun CollectionSpotClusterOverlay(
    spots: List<CollectionSpot>,
    markerColor: Int,
    onSpotClick: (CollectionSpot) -> Unit,
    onClusterClick: (LatLng, Double, Double) -> Unit,
) {
    val clusterItems = remember(spots) {
        spots.associateBy { spot -> spot.toClusterKey() }
    }

    DisposableMapEffect(clusterItems, markerColor) { naverMap ->
        val clusterer = Clusterer.Builder<CollectionSpotClusterKey>()
            .leafMarkerUpdater(
                CollectionSpotLeafMarkerUpdater(
                    markerColor = markerColor,
                    onSpotClick = onSpotClick,
                ),
            )
            .clusterMarkerUpdater(
                CollectionSpotClusterMarkerUpdater(
                    onClusterClick = { clusterPosition ->
                        onClusterClick(
                            clusterPosition,
                            naverMap.cameraPosition.zoom,
                            naverMap.maxZoom,
                        )
                    },
                ),
            )
            .build()

        clusterer.map = naverMap
        clusterer.addAll(clusterItems)

        onDispose {
            clusterer.clear()
            clusterer.map = null
        }
    }
}

private class CollectionSpotLeafMarkerUpdater(
    private val markerColor: Int,
    private val onSpotClick: (CollectionSpot) -> Unit,
) : LeafMarkerUpdater {
    private val defaultUpdater = DefaultLeafMarkerUpdater()

    override fun updateLeafMarker(info: LeafMarkerInfo, marker: com.naver.maps.map.overlay.Marker) {
        defaultUpdater.updateLeafMarker(info, marker)

        val spot = info.tag as? CollectionSpot
        marker.iconTintColor = markerColor
        marker.zIndex = DEFAULT_MARKER_Z_INDEX
        marker.captionText = spot?.name.orEmpty()
        marker.onClickListener = Overlay.OnClickListener { _ ->
            if (spot != null) {
                onSpotClick(spot)
            }
            true
        }
    }
}

private class CollectionSpotClusterMarkerUpdater(
    private val onClusterClick: (LatLng) -> Unit,
) : ClusterMarkerUpdater {
    private val defaultUpdater = DefaultClusterMarkerUpdater()

    override fun updateClusterMarker(
        info: ClusterMarkerInfo,
        marker: com.naver.maps.map.overlay.Marker,
    ) {
        defaultUpdater.updateClusterMarker(info, marker)
        marker.zIndex = CLUSTER_MARKER_Z_INDEX
        marker.onClickListener = Overlay.OnClickListener { _ ->
            onClusterClick(info.position)
            true
        }
    }
}

private val DEFAULT_LOCATION = LatLng(
    37.5666102,
    126.9783881,
)

private const val DEFAULT_ZOOM = 12.0
private const val SEARCH_RESULT_ZOOM = 15.0
private const val SELECTED_SPOT_ZOOM = 16.0
private const val SEARCH_RESULT_BOUNDS_PADDING = 120
private const val PROGRAMMATIC_CAMERA_MOVE_GUARD_FRAMES = 3
private const val CLUSTER_CLICK_ZOOM_INCREMENT = 1.0

private const val DEFAULT_MARKER_Z_INDEX = 0
private const val SELECTED_MARKER_Z_INDEX = 10
private const val CLUSTER_MARKER_Z_INDEX = 1
private val NaverLogoHorizontalPadding = 12.dp
private val NaverLogoTopPadding = 16.dp
private val NaverLogoDefaultBottomPadding = 16.dp

private data class CameraSnapshot(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double,
)

private fun CameraPosition.toCameraSnapshot(): CameraSnapshot =
    CameraSnapshot(
        latitude = target.latitude,
        longitude = target.longitude,
        zoom = zoom,
    )

private fun CameraSnapshot.toCoordinate(): Coordinate =
    Coordinate(
        latitude = latitude,
        longitude = longitude,
    )

private fun List<Coordinate>.toLatLngBounds(): LatLngBounds {
    val builder = LatLngBounds.Builder()

    forEach { coordinate ->
        builder.include(
            LatLng(
                coordinate.latitude,
                coordinate.longitude,
            ),
        )
    }

    return builder.build()
}
