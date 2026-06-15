package com.team.yeogibeoryeo.domain.favorite.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RegionalGuideFavoriteKeyTest {
    @Test
    fun `encode and decode preserves region target region and management zone`() {
        val key =
            RegionalGuideFavoriteKey(
                sido = "인천광역시",
                sigungu = "중구",
                eupmyeondong = "신흥동",
                targetRegionName = "신흥동+율목동",
                managementZoneName = "중구 관리구역",
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
    fun `management zone separates same target region candidates`() {
        val baseRegion =
            RegionalGuideFavoriteKey(
                sido = "대전광역시",
                sigungu = "유성구",
                eupmyeondong = "반석동",
                targetRegionName = "반석동 일부지역",
                managementZoneName = "노은2동",
            )
        val otherManagementZone = baseRegion.copy(managementZoneName = "노은3동")

        assertNotEquals(baseRegion.encode(), otherManagementZone.encode())
    }

    @Test
    fun `legacy key without management zone can still be decoded`() {
        val legacyKey = "regional-guide-v1|5:대전광역시3:유성구3:반석동8:반석동 일부지역"

        val decodedKey = RegionalGuideFavoriteKey.decodeOrNull(legacyKey)

        assertEquals(
            RegionalGuideFavoriteKey(
                sido = "대전광역시",
                sigungu = "유성구",
                eupmyeondong = "반석동",
                targetRegionName = "반석동 일부지역",
                managementZoneName = null,
            ),
            decodedKey
        )
    }

    @Test
    fun `encodeLegacy omits management zone for compatible favorite lookup`() {
        val key =
            RegionalGuideFavoriteKey(
                sido = "Sido",
                sigungu = "Sigungu",
                eupmyeondong = "Dong",
                targetRegionName = "Target",
                managementZoneName = "Management",
            )

        assertEquals(
            RegionalGuideFavoriteKey(
                sido = "Sido",
                sigungu = "Sigungu",
                eupmyeondong = "Dong",
                targetRegionName = "Target",
                managementZoneName = null,
            ),
            RegionalGuideFavoriteKey.decodeOrNull(key.encodeLegacy())
        )
    }

    @Test
    fun `invalid key returns null instead of throwing`() {
        assertNull(RegionalGuideFavoriteKey.decodeOrNull("broken-target-id"))
        assertNull(RegionalGuideFavoriteKey.decodeOrNull("regional-guide-v1|4:Sido"))
    }
}
