package com.team.yeogibeoryeo.data.favorite.mapper

import com.team.yeogibeoryeo.data.favorite.local.RegionalGuideFavoriteSnapshotEntity
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteKey
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.region.model.Region
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegionalGuideFavoriteSnapshotMapperTest {
    @Test
    fun `snapshot maps to entity and back`() {
        val snapshot =
            RegionalGuideFavoriteSnapshot(
                targetId =
                    RegionalGuideFavoriteKey(
                        sido = "인천광역시",
                        sigungu = "중구",
                        eupmyeondong = "신흥동",
                        targetRegionName = "신흥동+율목동",
                    ).encode(),
                region = Region(
                    sido = "인천광역시",
                    sigungu = "중구",
                    eupmyeondong = "신흥동",
                ),
                targetRegionName = "신흥동+율목동",
                managementZoneName = "중구 관리구역",
            )

        assertEquals(snapshot, snapshot.toEntity().toDomain())
    }

    @Test
    fun `invalid target id maps to null`() {
        val entity =
            RegionalGuideFavoriteSnapshotEntity(
                targetId = "broken-target-id",
                sido = "인천광역시",
                sigungu = "중구",
                eupmyeondong = null,
                targetRegionName = "신흥동+율목동",
                managementZoneName = "중구 관리구역",
            )

        assertNull(entity.toDomain())
    }
}
