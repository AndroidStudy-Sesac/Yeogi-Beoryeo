package com.team.yeogibeoryeo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.Marker
import com.naver.maps.map.compose.MarkerState
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.rememberCameraPositionState

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun EwasteMapScreen() {
    val context = LocalContext.current

    val stores: List<EwasteStore> = remember {
        loadEwasteStores(context)
    }

    val validStores = stores.filter { store ->
        store.latitude != null && store.longitude != null
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(
            LatLng(37.5665, 126.9780),
            11.0
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        NaverMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            validStores.forEach { store ->
                val lat = store.latitude
                val lng = store.longitude

                if (lat != null && lng != null) {
                    Marker(
                        state = MarkerState(
                            position = LatLng(lat, lng)
                        ),
                        captionText = store.storeName,
                        subCaptionText = store.category
                    )
                }
            }
        }

        if (validStores.isEmpty()) {
            Text("표시할 폐가전 수거처 좌표가 없습니다.")
        }
    }
}