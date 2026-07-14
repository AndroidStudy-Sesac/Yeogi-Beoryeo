package com.team.yeogibeoryeo.data.spot.cache

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecentCurrentLocationSpotCacheMapperTest {

    @Test
    fun `domain cache entry를 dto로 변환할 때 검색 결과와 저장 시각을 유지한다`() {
        val spot = sampleSpot(
            id = "spot-1",
            coordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881),
        )
        val entry = RecentCurrentLocationSpotCacheEntry(
            spots = listOf(spot),
            searchCoordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881),
            savedAtMillis = 1_200_000L,
        )

        val dto = entry.toDto()

        assertEquals(1_200_000L, dto.savedAtMillis)
        assertEquals(37.5666102, dto.searchCoordinate?.latitude)
        assertEquals(126.9783881, dto.searchCoordinate?.longitude)
        assertEquals(1, dto.spots.size)
        assertEquals("spot-1", dto.spots.first().id)
        assertEquals(CollectionSpotType.STANDARD_BAG_STORE.name, dto.spots.first().type)
        assertEquals(37.5666102, dto.spots.first().coordinate?.latitude)
        assertEquals(126.9783881, dto.spots.first().coordinate?.longitude)
    }

    @Test
    fun `dto를 domain cache entry로 변환할 때 수거 장소 좌표와 부가 정보를 복원한다`() {
        val dto = RecentCurrentLocationSpotCacheDto(
            spots = listOf(
                CollectionSpotCacheDto(
                    id = "spot-1",
                    name = "수거 장소",
                    type = CollectionSpotType.RECYCLING_CENTER.name,
                    address = "서울특별시 영등포구",
                    detailLocation = "1층 입구",
                    coordinate = CoordinateCacheDto(latitude = 37.1, longitude = 127.1),
                    distanceMeter = 120,
                    isBookmarked = true,
                ),
            ),
            searchCoordinate = CoordinateCacheDto(latitude = 37.5, longitude = 126.9),
            savedAtMillis = 1_200_000L,
        )

        val entry = dto.toDomain()

        assertEquals(1_200_000L, entry.savedAtMillis)
        assertEquals(Coordinate(latitude = 37.5, longitude = 126.9), entry.searchCoordinate)
        assertEquals(CollectionSpotType.RECYCLING_CENTER, entry.spots.first().type)
        assertEquals(Coordinate(latitude = 37.1, longitude = 127.1), entry.spots.first().coordinate)
        assertEquals("1층 입구", entry.spots.first().detailLocation)
        assertEquals(120, entry.spots.first().distanceMeter)
        assertEquals(true, entry.spots.first().isBookmarked)
    }

    @Test
    fun `coordinate가 없는 장소도 cache dto로 변환할 수 있다`() {
        val entry = RecentCurrentLocationSpotCacheEntry(
            spots = listOf(sampleSpot(id = "spot-without-coordinate", coordinate = null)),
            searchCoordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881),
            savedAtMillis = 1_200_000L,
        )

        val dto = entry.toDto()
        val restoredEntry = dto.toDomain()

        assertNull(dto.spots.first().coordinate)
        assertNull(restoredEntry.spots.first().coordinate)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `기준 좌표가 없는 dto는 domain cache entry로 복원하지 않는다`() {
        val dto = RecentCurrentLocationSpotCacheDto(
            spots = emptyList(),
            searchCoordinate = null,
            savedAtMillis = 1_200_000L,
        )

        dto.toDomain()
    }

    private fun sampleSpot(
        id: String,
        coordinate: Coordinate?,
    ): CollectionSpot {
        return CollectionSpot(
            id = id,
            name = "수거 장소 $id",
            type = CollectionSpotType.STANDARD_BAG_STORE,
            address = "서울특별시 영등포구",
            detailLocation = null,
            coordinate = coordinate,
            distanceMeter = null,
            isBookmarked = false,
        )
    }
}
