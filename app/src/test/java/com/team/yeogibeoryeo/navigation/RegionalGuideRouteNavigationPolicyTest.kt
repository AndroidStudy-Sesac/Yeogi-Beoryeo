package com.team.yeogibeoryeo.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionalGuideRouteNavigationPolicyTest {
    @Test
    fun `regional guide route without favorite target is guide tab route`() {
        assertFalse(RegionalGuideRoute().isFavoriteReentryRoute())
    }

    @Test
    fun `regional guide route with favorite target is favorites reentry route`() {
        assertTrue(
            RegionalGuideRoute(initialFavoriteTargetId = "regional-guide-v1|4:Sido")
                .isFavoriteReentryRoute(),
        )
    }

    @Test
    fun `blank favorite target is not favorites reentry route`() {
        assertFalse(RegionalGuideRoute(initialFavoriteTargetId = " ").isFavoriteReentryRoute())
    }
}
