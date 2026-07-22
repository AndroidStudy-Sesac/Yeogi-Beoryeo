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
    fun `uses compose markers below clustering threshold`() {
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
    fun `uses cluster markers at clustering threshold`() {
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
    fun `excludes selected spot from cluster markers and keeps it as compose marker`() {
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
    fun `does not exclude matching cluster spot when selected spot has no coordinate`() {
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
    fun `excludes spots without coordinates before applying clustering threshold`() {
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
    fun `deduplicates compose markers by spot id`() {
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
    fun `cluster key uses spot id and coordinate`() {
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
