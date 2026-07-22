package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.clustering.ClusterMarkerInfo
import com.naver.maps.map.clustering.ClusterMarkerUpdater
import com.naver.maps.map.clustering.Clusterer
import com.naver.maps.map.clustering.DefaultClusterMarkerUpdater
import com.naver.maps.map.clustering.DefaultLeafMarkerUpdater
import com.naver.maps.map.clustering.LeafMarkerInfo
import com.naver.maps.map.clustering.LeafMarkerUpdater
import com.naver.maps.map.compose.DisposableMapEffect
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot

@OptIn(ExperimentalNaverMapApi::class)
@Composable
internal fun CollectionSpotClusterOverlay(
    spots: List<CollectionSpot>,
    markerColor: Int,
    onSpotClick: (CollectionSpot) -> Unit,
    onClusterClick: (LatLng, Int, Double) -> Unit,
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
                    onClusterClick = { clusterPosition, clusterMaxZoom ->
                        onClusterClick(
                            clusterPosition,
                            clusterMaxZoom,
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

    override fun updateLeafMarker(info: LeafMarkerInfo, marker: Marker) {
        defaultUpdater.updateLeafMarker(info, marker)

        val spot = info.tag as? CollectionSpot
        marker.iconTintColor = markerColor
        marker.zIndex = DEFAULT_MARKER_Z_INDEX
        marker.captionText = spot?.name.orEmpty()
        marker.onClickListener = Overlay.OnClickListener {
            if (spot != null) {
                onSpotClick(spot)
            }
            true
        }
    }
}

private class CollectionSpotClusterMarkerUpdater(
    private val onClusterClick: (LatLng, Int) -> Unit,
) : ClusterMarkerUpdater {
    private val defaultUpdater = DefaultClusterMarkerUpdater()

    override fun updateClusterMarker(info: ClusterMarkerInfo, marker: Marker) {
        defaultUpdater.updateClusterMarker(info, marker)
        marker.zIndex = CLUSTER_MARKER_Z_INDEX
        marker.onClickListener = Overlay.OnClickListener {
            onClusterClick(info.position, info.maxZoom)
            true
        }
    }
}

private const val DEFAULT_MARKER_Z_INDEX = 0
private const val CLUSTER_MARKER_Z_INDEX = 1
