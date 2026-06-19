package com.team.yeogibeoryeo.navigation

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapRouteInitialSpotTypeMapperTest {
    @Test
    fun `valid initial spot type is parsed`() {
        val route = MapRoute(initialSpotType = CollectionSpotType.BATTERY_BIN.name)

        assertEquals(CollectionSpotType.BATTERY_BIN, route.toInitialCollectionSpotTypeOrNull())
    }

    @Test
    fun `new visible initial spot type is parsed`() {
        val route = MapRoute(initialSpotType = CollectionSpotType.MEDICINE_DROP_BOX.name)

        assertEquals(CollectionSpotType.MEDICINE_DROP_BOX, route.toInitialCollectionSpotTypeOrNull())
    }

    @Test
    fun `invalid initial spot type is ignored`() {
        val route = MapRoute(initialSpotType = "NOT_A_SPOT_TYPE")

        assertNull(route.toInitialCollectionSpotTypeOrNull())
    }

    @Test
    fun `blank initial spot type is ignored`() {
        val route = MapRoute(initialSpotType = " ")

        assertNull(route.toInitialCollectionSpotTypeOrNull())
    }
}
