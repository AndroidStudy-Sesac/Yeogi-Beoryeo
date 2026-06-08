package com.team.yeogibeoryeo.domain.regionalguide.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RegionalGuideRegionKeyNormalizerTest {

    @Test
    fun `administrative district sigungu is normalized to info sigungu key`() {
        assertEquals(
            "수원시",
            RegionalGuideRegionKeyNormalizer.normalizeSigungu("수원시 장안구")
        )
        assertEquals(
            "성남시",
            RegionalGuideRegionKeyNormalizer.normalizeSigungu("성남시 중원구")
        )
    }

    @Test
    fun `normal sigungu is kept as info sigungu key`() {
        assertEquals(
            "중구",
            RegionalGuideRegionKeyNormalizer.normalizeSigungu("중구")
        )
        assertEquals(
            "울주군",
            RegionalGuideRegionKeyNormalizer.normalizeSigungu("울주군")
        )
    }
}
