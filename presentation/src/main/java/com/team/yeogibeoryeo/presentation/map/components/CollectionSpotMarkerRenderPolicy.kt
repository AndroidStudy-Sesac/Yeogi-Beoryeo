package com.team.yeogibeoryeo.presentation.map.components

import com.naver.maps.geometry.LatLng
import com.naver.maps.map.clustering.ClusteringKey
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot

internal const val MARKER_CLUSTERING_THRESHOLD = 100

internal data class CollectionSpotMarkerRenderState(
    val composeMarkerSpots: List<CollectionSpot>,
    val clusterMarkerSpots: List<CollectionSpot>,
    val useClustering: Boolean,
)

internal data class CollectionSpotClusterKey(
    val id: String,
    val latitude: Double,
    val longitude: Double,
) : ClusteringKey {
    override fun getPosition(): LatLng =
        LatLng(latitude, longitude)
}

internal fun buildCollectionSpotMarkerRenderState(
    spots: List<CollectionSpot>,
    selectedSpot: CollectionSpot?,
    clusteringThreshold: Int = MARKER_CLUSTERING_THRESHOLD,
): CollectionSpotMarkerRenderState {
    val markerSpots = (spots + listOfNotNull(selectedSpot))
        .distinctBy { spot -> spot.id }
        .filter { spot -> spot.coordinate != null }
    val useClustering = markerSpots.size >= clusteringThreshold

    if (!useClustering) {
        return CollectionSpotMarkerRenderState(
            composeMarkerSpots = markerSpots,
            clusterMarkerSpots = emptyList(),
            useClustering = false,
        )
    }

    val selectedClusterExclusionId = selectedSpot
        ?.id
        ?.takeIf { selectedSpot.coordinate != null }

    return CollectionSpotMarkerRenderState(
        composeMarkerSpots = listOfNotNull(selectedSpot)
            .filter { spot -> spot.coordinate != null },
        clusterMarkerSpots = markerSpots
            .filter { spot -> spot.id != selectedClusterExclusionId },
        useClustering = true,
    )
}

internal fun CollectionSpot.toClusterKey(): CollectionSpotClusterKey {
    val coordinate = requireNotNull(coordinate) {
        "클러스터 키 생성에는 좌표가 필요합니다."
    }

    return CollectionSpotClusterKey(
        id = id,
        latitude = coordinate.latitude,
        longitude = coordinate.longitude,
    )
}
