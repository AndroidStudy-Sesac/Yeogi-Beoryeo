package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.LocationTrackingMode
import com.naver.maps.map.compose.MapProperties
import com.naver.maps.map.compose.MapUiSettings
import com.naver.maps.map.compose.Marker
import com.naver.maps.map.compose.MarkerDefaults
import com.naver.maps.map.compose.MarkerState
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.rememberCameraPositionState
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.util.MarkerIcons
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.presentation.map.location.rememberMapLocationSource
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

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
    val isDarkTheme = isSystemInDarkTheme()
    val lightDefaultMarkerColor = MaterialTheme.colorScheme.primary
    val lightSelectedMarkerColor = MaterialTheme.colorScheme.tertiary
    val defaultMarkerStyle = CollectionSpotMarkerStylePolicy.defaultStyle(
        isDarkTheme = isDarkTheme,
        lightColor = lightDefaultMarkerColor,
    )
    val defaultMarkerColorArgb = defaultMarkerStyle.color.toArgb()
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
                markerIcon = defaultMarkerStyle.icon.toOverlayImage(),
                markerColor = defaultMarkerColorArgb,
                onSpotClick = { spot ->
                    currentOnSpotClick(spot)
                },
                onClusterClick = { clusterPosition, clusterMaxZoom, mapMaxZoom ->
                    coroutineScope.launch {
                        moveCamera(
                            CameraUpdate.scrollAndZoomTo(
                                clusterPosition,
                                (clusterMaxZoom + 1).toDouble()
                                    .coerceAtMost(mapMaxZoom),
                            ),
                        )
                    }
                },
            )
        }

        markerRenderState.composeMarkerSpots.forEach { spot ->
            val coordinate = spot.coordinate ?: return@forEach
            val isSelected = selectedSpot?.id == spot.id
            val markerStyle = CollectionSpotMarkerStylePolicy.style(
                isSelected = isSelected,
                isDarkTheme = isDarkTheme,
                lightDefaultColor = lightDefaultMarkerColor,
                lightSelectedColor = lightSelectedMarkerColor,
            )

            Marker(
                state = MarkerState(
                    position = LatLng(
                        coordinate.latitude,
                        coordinate.longitude,
                    ),
                ),
                icon = markerStyle.icon.toOverlayImage(),
                captionText = spot.name,
                iconTintColor = markerStyle.color,
                width = markerStyle.width ?: MarkerDefaults.SizeAuto,
                height = markerStyle.height ?: MarkerDefaults.SizeAuto,
                isForceShowIcon = markerStyle.isForceShowIcon,
                zIndex = markerStyle.zIndex,
                onClick = {
                    onSpotClick(spot)
                    true
                },
            )
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

private fun CollectionSpotMarkerIcon.toOverlayImage(): OverlayImage =
    when (this) {
        CollectionSpotMarkerIcon.Default -> MarkerDefaults.Icon
        CollectionSpotMarkerIcon.Black -> MarkerIcons.BLACK
    }

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
