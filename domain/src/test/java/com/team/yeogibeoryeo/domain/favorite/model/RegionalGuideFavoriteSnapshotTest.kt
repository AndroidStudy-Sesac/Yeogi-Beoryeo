package com.team.yeogibeoryeo.domain.favorite.model

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegionalGuideFavoriteSnapshotTest {

    @Test
    fun `가이드 즐겨찾기 스냅샷은 지역명 공백을 정규화한다`() {
        val guide =
            RegionalDisposalGuide(
                region = Region(sido = "경기도", sigungu = "수원시"),
                targetRegionName = "  ",
                managementZoneName = " 장안동 ",
                schedules = emptyList(),
            )

        val snapshot = guide.toFavoriteSnapshot()

        assertNull(snapshot.targetRegionName)
        assertEquals("장안동", snapshot.managementZoneName)
    }
}
