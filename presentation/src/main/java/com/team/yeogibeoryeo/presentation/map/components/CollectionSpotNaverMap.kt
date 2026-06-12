package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.LocationTrackingMode
import com.naver.maps.map.compose.MapProperties
import com.naver.maps.map.compose.Marker
import com.naver.maps.map.compose.MarkerState
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.rememberCameraPositionState
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.presentation.map.location.rememberMapLocationSource

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun CollectionSpotNaverMap(
    spots: List<CollectionSpot>,
    selectedSpot: CollectionSpot?,
    isLocationPermissionGranted: Boolean,
    locationTrackingMode: LocationTrackingMode,
    onSpotClick: (CollectionSpot) -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val defaultMarkerColor = MaterialTheme.colorScheme.primary
    val selectedMarkerColor = MaterialTheme.colorScheme.tertiary
    val markerSpots =
        (spots + listOfNotNull(selectedSpot))
            .distinctBy { spot -> spot.id }
            .filter { spot -> spot.coordinate != null }
    val locationSource = rememberMapLocationSource()
    val mapProperties = MapProperties(
        locationTrackingMode = if (isLocationPermissionGranted) {
            locationTrackingMode
        } else {
            LocationTrackingMode.None
        },
    )

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(
            DEFAULT_LOCATION,
            DEFAULT_ZOOM,
        )
    }

    LaunchedEffect(spots) {
        if (selectedSpot != null) return@LaunchedEffect

        val firstSpot = spots.firstOrNull { spot -> spot.coordinate != null }
        val coordinate = firstSpot?.coordinate

        if (coordinate != null) {
            cameraPositionState.move(
                CameraUpdate.scrollAndZoomTo(
                    LatLng(
                        coordinate.latitude,
                        coordinate.longitude,
                    ),
                    SEARCH_RESULT_ZOOM,
                ),
            )
        }
    }

    LaunchedEffect(selectedSpot?.id) {
        val coordinate = selectedSpot?.coordinate

        if (coordinate != null) {
            cameraPositionState.move(
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
        locationSource = locationSource.takeIf {
            isLocationPermissionGranted
        },
        onMapClick = { _, _ ->
            onMapClick()
        },
    ) {
        markerSpots.forEach { spot ->
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

private val DEFAULT_LOCATION = LatLng(
    37.5666102,
    126.9783881,
)

private const val DEFAULT_ZOOM = 12.0
private const val SEARCH_RESULT_ZOOM = 15.0
private const val SELECTED_SPOT_ZOOM = 16.0

private const val DEFAULT_MARKER_Z_INDEX = 0
private const val SELECTED_MARKER_Z_INDEX = 10
