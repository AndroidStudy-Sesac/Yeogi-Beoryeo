package com.team.yeogibeoryeo.domain.spot.usecase

import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class CalculateDistanceMeterUseCase @Inject constructor() {

    operator fun invoke(
        from: Coordinate,
        to: Coordinate,
    ): Int {
        val latitudeDistance = Math.toRadians(to.latitude - from.latitude)
        val longitudeDistance = Math.toRadians(to.longitude - from.longitude)
        val fromLatitude = Math.toRadians(from.latitude)
        val toLatitude = Math.toRadians(to.latitude)

        val a = sin(latitudeDistance / 2) * sin(latitudeDistance / 2) +
            cos(fromLatitude) * cos(toLatitude) *
            sin(longitudeDistance / 2) * sin(longitudeDistance / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (EARTH_RADIUS_METER * c).roundToInt()
    }

    private companion object {
        const val EARTH_RADIUS_METER = 6_371_000
    }
}
