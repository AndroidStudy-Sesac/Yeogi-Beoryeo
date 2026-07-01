package com.team.yeogibeoryeo.navigation

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapRouteInitialSpotTypeMapperTest {
    @Test
    fun `노출 대상 수거 장소 타입이면 초기 타입으로 변환한다`() {
        val route = MapRoute(initialSpotType = CollectionSpotRouteType.BATTERY_BIN)

        assertEquals(CollectionSpotType.BATTERY_BIN, route.toInitialCollectionSpotTypeOrNull())
    }

    @Test
    fun `새로 노출된 수거 장소 타입이면 초기 타입으로 변환한다`() {
        val route = MapRoute(initialSpotType = CollectionSpotRouteType.MEDICINE_DROP_BOX)

        assertEquals(CollectionSpotType.MEDICINE_DROP_BOX, route.toInitialCollectionSpotTypeOrNull())
    }

    @Test
    fun `초기 타입이 없으면 null을 반환한다`() {
        val route = MapRoute(initialSpotType = null)

        assertNull(route.toInitialCollectionSpotTypeOrNull())
    }
}
