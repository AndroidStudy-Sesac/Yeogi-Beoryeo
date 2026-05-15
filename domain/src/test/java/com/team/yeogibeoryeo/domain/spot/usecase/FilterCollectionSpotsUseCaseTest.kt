package com.team.yeogibeoryeo.domain.spot.usecase

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterCollectionSpotsUseCaseTest {

    private val filterCollectionSpotsUseCase = FilterCollectionSpotsUseCase()

    @Test
    fun `선택된 타입이 없으면 전체 장소 목록을 반환한다`() {
        val spots = createCollectionSpots()

        val result = filterCollectionSpotsUseCase(
            spots = spots,
            selectedTypes = emptySet(),
        )

        assertEquals(spots, result)
    }

    @Test
    fun `선택된 타입에 해당하는 장소만 반환한다`() {
        val spots = createCollectionSpots()

        val result = filterCollectionSpotsUseCase(
            spots = spots,
            selectedTypes = setOf(CollectionSpotType.BATTERY_BIN),
        )

        assertEquals(1, result.size)
        assertEquals(CollectionSpotType.BATTERY_BIN, result.first().type)
    }

    @Test
    fun `여러 타입이 선택되면 해당 타입들의 장소를 모두 반환한다`() {
        val spots = createCollectionSpots()

        val result = filterCollectionSpotsUseCase(
            spots = spots,
            selectedTypes = setOf(
                CollectionSpotType.BATTERY_BIN,
                CollectionSpotType.RECYCLING_CENTER,
            ),
        )

        assertEquals(2, result.size)
        assertEquals(
            listOf(
                CollectionSpotType.BATTERY_BIN,
                CollectionSpotType.RECYCLING_CENTER,
            ),
            result.map { it.type },
        )
    }

    @Test
    fun `선택된 타입에 해당하는 장소가 없으면 빈 리스트를 반환한다`() {
        val spots = createCollectionSpots()

        val result = filterCollectionSpotsUseCase(
            spots = spots,
            selectedTypes = setOf(CollectionSpotType.PHONE_DROP_OFF),
        )

        assertEquals(emptyList<CollectionSpot>(), result)
    }

    private fun createCollectionSpots(): List<CollectionSpot> {
        return listOf(
            createCollectionSpot(
                id = "spot-1",
                name = "폐건전지 수거함",
                type = CollectionSpotType.BATTERY_BIN,
            ),
            createCollectionSpot(
                id = "spot-2",
                name = "재활용센터",
                type = CollectionSpotType.RECYCLING_CENTER,
            ),
            createCollectionSpot(
                id = "spot-3",
                name = "종량제봉투 판매소",
                type = CollectionSpotType.STANDARD_BAG_STORE,
            ),
        )
    }

    private fun createCollectionSpot(
        id: String,
        name: String,
        type: CollectionSpotType,
    ): CollectionSpot {
        return CollectionSpot(
            id = id,
            name = name,
            type = type,
            address = "서울특별시 영등포구 문래동",
            detailLocation = null,
            coordinate = null,
            distanceMeter = null,
            isBookmarked = false,
        )
    }
}