package com.team.yeogibeoryeo.domain.favorite.model

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionSpotFavoriteSnapshotTest {
    @Test
    fun `CollectionSpot id를 수거 장소 즐겨찾기 targetId로 사용한다`() {
        val spot =
            CollectionSpot(
                id = "폐건전지 수거함_서울특별시 영등포구 문래동_주민센터 앞",
                name = "폐건전지 수거함",
                type = CollectionSpotType.BATTERY_BIN,
                address = "서울특별시 영등포구 문래동",
                detailLocation = "주민센터 앞",
                coordinate = Coordinate(latitude = 37.5, longitude = 126.9),
                distanceMeter = 120,
                isBookmarked = true,
            )

        val snapshot = spot.toFavoriteSnapshot()

        assertEquals(spot.id, snapshot.targetId)
        assertEquals(spot.name, snapshot.name)
        assertEquals(spot.type, snapshot.type)
        assertEquals(spot.address, snapshot.address)
        assertEquals(spot.detailLocation, snapshot.detailLocation)
        assertEquals(spot.coordinate, snapshot.coordinate)
    }
}
