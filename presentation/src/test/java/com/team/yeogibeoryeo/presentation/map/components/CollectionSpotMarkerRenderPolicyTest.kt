package com.team.yeogibeoryeo.presentation.map.components

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionSpotMarkerRenderPolicyTest {
    @Test
    fun `클러스터링 기준 미만이면 Compose 마커를 사용한다`() {
        val spots = spots(count = 99)

        val state = buildCollectionSpotMarkerRenderState(
            spots = spots,
            selectedSpot = null,
        )

        assertFalse(state.useClustering)
        assertEquals(99, state.composeMarkerSpots.size)
        assertTrue(state.clusterMarkerSpots.isEmpty())
    }

    @Test
    fun `클러스터링 기준 이상이면 클러스터 마커를 사용한다`() {
        val spots = spots(count = MARKER_CLUSTERING_THRESHOLD)

        val state = buildCollectionSpotMarkerRenderState(
            spots = spots,
            selectedSpot = null,
        )

        assertTrue(state.useClustering)
        assertTrue(state.composeMarkerSpots.isEmpty())
        assertEquals(MARKER_CLUSTERING_THRESHOLD, state.clusterMarkerSpots.size)
    }

    @Test
    fun `선택한 스팟은 클러스터에서 제외하고 Compose 마커로 유지한다`() {
        val selectedSpot = spot(id = "spot-42")
        val spots = spots(count = MARKER_CLUSTERING_THRESHOLD)
            .map { spot ->
                if (spot.id == selectedSpot.id) selectedSpot else spot
            }

        val state = buildCollectionSpotMarkerRenderState(
            spots = spots,
            selectedSpot = selectedSpot,
        )

        assertTrue(state.useClustering)
        assertEquals(listOf(selectedSpot), state.composeMarkerSpots)
        assertEquals(MARKER_CLUSTERING_THRESHOLD - 1, state.clusterMarkerSpots.size)
        assertFalse(state.clusterMarkerSpots.any { spot -> spot.id == selectedSpot.id })
    }

    @Test
    fun `선택한 스팟에 좌표가 없으면 같은 ID의 클러스터 스팟을 제외하지 않는다`() {
        val selectedSpot = spot(id = "spot-42", coordinate = null)
        val spots = spots(count = MARKER_CLUSTERING_THRESHOLD)

        val state = buildCollectionSpotMarkerRenderState(
            spots = spots,
            selectedSpot = selectedSpot,
        )

        assertTrue(state.useClustering)
        assertTrue(state.composeMarkerSpots.isEmpty())
        assertTrue(state.clusterMarkerSpots.any { spot -> spot.id == selectedSpot.id })
    }

    @Test
    fun `클러스터링 기준을 적용하기 전에 좌표 없는 스팟을 제외한다`() {
        val spots = spots(count = MARKER_CLUSTERING_THRESHOLD - 1) +
            spot(id = "no-coordinate", coordinate = null)

        val state = buildCollectionSpotMarkerRenderState(
            spots = spots,
            selectedSpot = null,
        )

        assertFalse(state.useClustering)
        assertEquals(MARKER_CLUSTERING_THRESHOLD - 1, state.composeMarkerSpots.size)
        assertTrue(state.composeMarkerSpots.none { spot -> spot.coordinate == null })
        assertTrue(state.clusterMarkerSpots.isEmpty())
    }

    @Test
    fun `Compose 마커는 스팟 ID를 기준으로 중복을 제거한다`() {
        val duplicate = spot(id = "duplicate")
        val spots = listOf(duplicate, duplicate.copy(name = "updated"))

        val state = buildCollectionSpotMarkerRenderState(
            spots = spots,
            selectedSpot = duplicate,
        )

        assertFalse(state.useClustering)
        assertEquals(listOf(duplicate), state.composeMarkerSpots)
    }

    @Test
    fun `클러스터 키는 스팟 ID와 좌표를 사용한다`() {
        val spot = spot(
            id = "spot-key",
            coordinate = Coordinate(
                latitude = 37.5,
                longitude = 126.9,
            ),
        )

        val key = spot.toClusterKey()

        assertEquals("spot-key", key.id)
        assertEquals(37.5, key.latitude, 0.0)
        assertEquals(126.9, key.longitude, 0.0)
    }

    @Test
    fun `클러스터 마커도 스팟 ID를 기준으로 중복을 제거한다`() {
        val originalSpots = spots(count = MARKER_CLUSTERING_THRESHOLD)
        val duplicate = originalSpots.first().copy(
            coordinate = Coordinate(
                latitude = 35.0,
                longitude = 129.0,
            ),
        )

        val state = buildCollectionSpotMarkerRenderState(
            spots = originalSpots + duplicate,
            selectedSpot = null,
        )

        assertTrue(state.useClustering)
        assertEquals(MARKER_CLUSTERING_THRESHOLD, state.clusterMarkerSpots.size)
        assertEquals(originalSpots.first(), state.clusterMarkerSpots.first())
    }

    private fun spots(count: Int): List<CollectionSpot> =
        (0 until count).map { index ->
            spot(id = "spot-$index")
        }

    private fun spot(
        id: String,
        coordinate: Coordinate? = Coordinate(
            latitude = 37.0 + id.hashCode().mod(1000) / 10_000.0,
            longitude = 126.0 + id.hashCode().mod(1000) / 10_000.0,
        ),
    ): CollectionSpot =
        CollectionSpot(
            id = id,
            name = "Spot $id",
            type = CollectionSpotType.BATTERY_BIN,
            address = "Seoul",
            detailLocation = null,
            coordinate = coordinate,
        )
}
