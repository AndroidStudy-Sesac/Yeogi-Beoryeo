package com.team.yeogibeoryeo.domain.spot.usecase

import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateDistanceMeterUseCaseTest {

    private val useCase = CalculateDistanceMeterUseCase()

    @Test
    fun `같은 좌표의 거리는 0m로 계산한다`() {
        val coordinate = Coordinate(
            latitude = 37.5666102,
            longitude = 126.9783881,
        )

        val distanceMeter = useCase(
            from = coordinate,
            to = coordinate,
        )

        assertEquals(0, distanceMeter)
    }

    @Test
    fun `서로 다른 좌표의 거리를 meter 단위로 계산한다`() {
        val seoulCityHall = Coordinate(
            latitude = 37.5666102,
            longitude = 126.9783881,
        )
        val nearbyCoordinate = Coordinate(
            latitude = 37.567508,
            longitude = 126.978960,
        )

        val distanceMeter = useCase(
            from = seoulCityHall,
            to = nearbyCoordinate,
        )

        assertEquals(112, distanceMeter)
    }
}
