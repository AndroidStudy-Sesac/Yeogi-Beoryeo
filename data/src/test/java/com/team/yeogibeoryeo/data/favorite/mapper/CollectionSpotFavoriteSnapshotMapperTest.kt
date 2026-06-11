package com.team.yeogibeoryeo.data.favorite.mapper

import com.team.yeogibeoryeo.data.favorite.local.CollectionSpotFavoriteSnapshotEntity
import com.team.yeogibeoryeo.domain.favorite.model.CollectionSpotFavoriteSnapshot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CollectionSpotFavoriteSnapshotMapperTest {
    @Test
    fun `수거 장소 즐겨찾기 스냅샷을 Entity로 변환한다`() {
        val snapshot =
            CollectionSpotFavoriteSnapshot(
                targetId = "spot-1",
                name = "폐건전지 수거함",
                type = CollectionSpotType.BATTERY_BIN,
                address = "서울특별시 영등포구 문래동",
                detailLocation = "주민센터 앞",
                coordinate = Coordinate(latitude = 37.5, longitude = 126.9),
            )

        val entity = snapshot.toEntity()

        assertEquals("spot-1", entity.targetId)
        assertEquals("폐건전지 수거함", entity.name)
        assertEquals("BATTERY_BIN", entity.spotType)
        assertEquals("서울특별시 영등포구 문래동", entity.address)
        assertEquals("주민센터 앞", entity.detailLocation)
        assertEquals(37.5, entity.latitude)
        assertEquals(126.9, entity.longitude)
    }

    @Test
    fun `Entity를 수거 장소 즐겨찾기 스냅샷으로 변환한다`() {
        val entity =
            CollectionSpotFavoriteSnapshotEntity(
                targetId = "spot-1",
                name = "폐건전지 수거함",
                spotType = "BATTERY_BIN",
                address = "서울특별시 영등포구 문래동",
                detailLocation = "주민센터 앞",
                latitude = 37.5,
                longitude = 126.9,
            )

        val snapshot = entity.toDomain()

        assertEquals(
            CollectionSpotFavoriteSnapshot(
                targetId = "spot-1",
                name = "폐건전지 수거함",
                type = CollectionSpotType.BATTERY_BIN,
                address = "서울특별시 영등포구 문래동",
                detailLocation = "주민센터 앞",
                coordinate = Coordinate(latitude = 37.5, longitude = 126.9),
            ),
            snapshot,
        )
    }

    @Test
    fun `알 수 없는 수거 장소 타입은 null로 변환한다`() {
        val entity =
            CollectionSpotFavoriteSnapshotEntity(
                targetId = "spot-1",
                name = "수거함",
                spotType = "UNKNOWN_TYPE",
                address = "주소",
                detailLocation = null,
                latitude = null,
                longitude = null,
            )

        assertNull(entity.toDomain())
    }
}
