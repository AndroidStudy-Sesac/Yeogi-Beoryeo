package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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
import com.naver.maps.map.overlay.OverlayImage
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot

@OptIn(ExperimentalNaverMapApi::class)
@Composable
internal fun CollectionSpotClusterOverlay(
    spots: List<CollectionSpot>,
    markerStyle: CollectionSpotMarkerStyle,
    onSpotClick: (CollectionSpot) -> Unit,
    onClusterClick: (LatLng, Int, Double) -> Unit,
) {
    val density = LocalDensity.current
    val clusterItems = remember(spots) {
        spots.associateBy { spot -> spot.toClusterKey() }
    }
    val markerRenderStyle = remember(markerStyle, density) {
        markerStyle.toClusterLeafMarkerRenderStyle(density)
    }

    DisposableMapEffect(clusterItems, markerRenderStyle) { naverMap ->
        val clusterer = Clusterer.Builder<CollectionSpotClusterKey>()
            .leafMarkerUpdater(
                CollectionSpotLeafMarkerUpdater(
                    markerRenderStyle = markerRenderStyle,
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
    private val markerRenderStyle: ClusterLeafMarkerRenderStyle,
    private val onSpotClick: (CollectionSpot) -> Unit,
) : LeafMarkerUpdater {
    private val defaultUpdater = DefaultLeafMarkerUpdater()

    override fun updateLeafMarker(info: LeafMarkerInfo, marker: Marker) {
        defaultUpdater.updateLeafMarker(info, marker)

        val spot = info.tag as? CollectionSpot
        marker.icon = markerRenderStyle.icon
        marker.iconTintColor = markerRenderStyle.color
        marker.width = markerRenderStyle.width
        marker.height = markerRenderStyle.height
        marker.zIndex = markerRenderStyle.zIndex
        marker.isForceShowIcon = markerRenderStyle.isForceShowIcon
        marker.captionText = spot?.name.orEmpty()
        marker.onClickListener = Overlay.OnClickListener {
            if (spot != null) {
                onSpotClick(spot)
            }
            true
        }
    }
}

internal data class ClusterLeafMarkerRenderStyle(
    val icon: OverlayImage,
    val color: Int,
    val width: Int,
    val height: Int,
    val zIndex: Int,
    val isForceShowIcon: Boolean,
)

internal fun CollectionSpotMarkerStyle.toClusterLeafMarkerRenderStyle(
    density: Density,
): ClusterLeafMarkerRenderStyle {
    val markerSize = toClusterLeafMarkerSize(
        density = density,
        sizeAuto = Marker.SIZE_AUTO,
    )

    return ClusterLeafMarkerRenderStyle(
        icon = icon.toOverlayImage(),
        color = color.toArgb(),
        width = markerSize.width,
        height = markerSize.height,
        zIndex = zIndex,
        isForceShowIcon = isForceShowIcon,
    )
}

internal data class ClusterLeafMarkerSize(
    val width: Int,
    val height: Int,
)

internal fun CollectionSpotMarkerStyle.toClusterLeafMarkerSize(
    density: Density,
    sizeAuto: Int,
): ClusterLeafMarkerSize =
    ClusterLeafMarkerSize(
        width = width?.let { with(density) { it.roundToPx() } } ?: sizeAuto,
        height = height?.let { with(density) { it.roundToPx() } } ?: sizeAuto,
    )

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

private const val CLUSTER_MARKER_Z_INDEX = 1
