package com.team.yeogibeoryeo.domain.favorite.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RegionalGuideFavoriteKeyTest {
    @Test
    fun `encode and decode preserves region and target region`() {
        val key =
            RegionalGuideFavoriteKey(
                sido = "인천광역시",
                sigungu = "중구",
                eupmyeondong = "신흥동",
                targetRegionName = "신흥동+율목동",
            )

        assertEquals(key, RegionalGuideFavoriteKey.decodeOrNull(key.encode()))
    }

    @Test
    fun `target region separates multiple guide zones in same region`() {
        val baseRegion =
            RegionalGuideFavoriteKey(
                sido = "인천광역시",
                sigungu = "중구",
                eupmyeondong = null,
                targetRegionName = "신흥동+율목동",
            )
        val otherZone = baseRegion.copy(targetRegionName = "신포동+연안동")

        assertNotEquals(baseRegion.encode(), otherZone.encode())
    }

    @Test
    fun `invalid key returns null instead of throwing`() {
        assertNull(RegionalGuideFavoriteKey.decodeOrNull("broken-target-id"))
        assertNull(RegionalGuideFavoriteKey.decodeOrNull("regional-guide-v1|4:Sido"))
    }
}
