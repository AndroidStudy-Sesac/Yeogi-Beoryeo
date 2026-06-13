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
        val address = " Seoul Yeongdeungpo-gu Mullae-dong (Mullae) "

        val route = address.toRegionalGuideAddressRouteOrNull()

        assertEquals("Seoul Yeongdeungpo-gu Mullae-dong (Mullae)", route?.initialAddress)
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
            initialAddress = "Seoul Yeongdeungpo-gu Mullae-dong",
            initialFavoriteTargetId = "regional-guide-v1|4:Sido",
        )

        assertTrue(route.isFavoriteReentryRoute())
        assertFalse(route.isMapReentryRoute())
    }
}
