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
    fun `regional guide route with favorite target and no source is guide tab route`() {
        val route = RegionalGuideRoute(initialFavoriteTargetId = "regional-guide-v1|4:Sido")

        assertFalse(route.isFavoriteReentryRoute())
        assertFalse(route.isMapReentryRoute())
    }

    @Test
    fun `blank favorite target is not favorites reentry route`() {
        assertFalse(RegionalGuideRoute(initialFavoriteTargetId = " ").isFavoriteReentryRoute())
    }

    @Test
    fun `regional guide route from home summary remains guide tab route`() {
        val route = RegionalGuideRoute(initialFavoriteTargetId = "regional-guide-v2|4:Sido")

        assertFalse(route.isFavoriteReentryRoute())
        assertFalse(route.isMapReentryRoute())
    }

    @Test
    fun `regional guide route from favorites remains favorites reentry route`() {
        val route = RegionalGuideRoute(
            initialFavoriteTargetId = "regional-guide-v2|4:Sido",
            entrySource = RegionalGuideEntrySource.FAVORITES,
        )

        assertTrue(route.isFavoriteReentryRoute())
        assertFalse(route.isMapReentryRoute())
    }

    @Test
    fun `spot address creates regional guide address route`() {
        val address = "  서울특별시 중구 퇴계로 63 (남창동, 삼익패션타운)  "

        val route = address.toRegionalGuideAddressRouteOrNull()

        assertEquals("서울특별시 중구 퇴계로 63 (남창동, 삼익패션타운)", route?.initialAddress)
        assertFalse(route!!.isFavoriteReentryRoute())
        assertTrue(route.isMapReentryRoute())
    }

    @Test
    fun `blank spot address does not create regional guide address route`() {
        assertNull(" ".toRegionalGuideAddressRouteOrNull())
    }

    @Test
    fun `regional guide route with address and favorite target requires favorites source for favorites reentry`() {
        val route = RegionalGuideRoute(
            initialAddress = "서울특별시 중구 퇴계로 63 (남창동, 삼익패션타운)",
            initialFavoriteTargetId = "regional-guide-v1|4:Sido",
        )

        assertFalse(route.isFavoriteReentryRoute())
        assertFalse(route.isMapReentryRoute())
    }
}
