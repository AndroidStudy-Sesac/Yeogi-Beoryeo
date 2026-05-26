package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.Marker
import com.naver.maps.map.compose.MarkerState
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.rememberCameraPositionState
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun CollectionSpotNaverMap(
    spots: List<CollectionSpot>,
    selectedSpot: CollectionSpot?,
    onSpotClick: (CollectionSpot) -> Unit,
    modifier: Modifier = Modifier,
) {
    val markerSpots = spots.filter { spot ->
        spot.coordinate != null
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(
            DEFAULT_LOCATION,
            DEFAULT_ZOOM,
        )
    }

    LaunchedEffect(markerSpots) {
        val firstSpot = markerSpots.firstOrNull()
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
                    SELECTED_MARKER_COLOR
                } else {
                    DEFAULT_MARKER_COLOR
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

private val DEFAULT_MARKER_COLOR = Color(0xFF00C853)
private val SELECTED_MARKER_COLOR = Color(0xFFFF7043)

private const val DEFAULT_ZOOM = 12.0
private const val SEARCH_RESULT_ZOOM = 15.0
private const val SELECTED_SPOT_ZOOM = 16.0

private const val DEFAULT_MARKER_Z_INDEX = 0
private const val SELECTED_MARKER_Z_INDEX = 10