package com.team.yeogibeoryeo.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionalGuideRouteNavigationPolicyTest {
    @Test
    fun `regional guide route without favorite target is guide tab route`() {
        val route = RegionalGuideRoute()

        assertFalse(route.isFavoriteReentryRoute())
        assertFalse(route.isMapReentryRoute())
    }

    @Test
    fun `regional guide route with favorite target is favorites reentry route`() {
        val route = RegionalGuideRoute(initialFavoriteTargetId = "regional-guide-v1|4:Sido")

        assertTrue(route.isFavoriteReentryRoute())
        assertFalse(route.isMapReentryRoute())
    }

    @Test
    fun `blank favorite target is not favorites reentry route`() {
        assertFalse(RegionalGuideRoute(initialFavoriteTargetId = " ").isFavoriteReentryRoute())
    }

    @Test
    fun `spot address creates regional guide address route`() {
        val address = "  서울특별시 중구 남대문로 63 (남대문로2가, 한진빌딩)  "

        val route = address.toRegionalGuideAddressRouteOrNull()

        assertEquals("서울특별시 중구 남대문로 63 (남대문로2가, 한진빌딩)", route?.initialAddress)
        assertFalse(route!!.isFavoriteReentryRoute())
        assertTrue(route.isMapReentryRoute())
    }

    @Test
    fun `blank spot address does not create regional guide address route`() {
        assertNull(" ".toRegionalGuideAddressRouteOrNull())
    }

    @Test
    fun `regional guide route with address and favorite target remains favorites reentry route`() {
        val route = RegionalGuideRoute(
            initialAddress = "서울특별시 중구 남대문로 63 (남대문로2가, 한진빌딩)",
            initialFavoriteTargetId = "regional-guide-v1|4:Sido",
        )

        assertTrue(route.isFavoriteReentryRoute())
        assertFalse(route.isMapReentryRoute())
    }
}
