package com.team.yeogibeoryeo.presentation.map.formatter

import java.util.Locale

object DistanceFormatter {
    fun format(distanceMeter: Int?): String? {
        val distance = distanceMeter?.takeIf { it >= 0 } ?: return null

        return if (distance < METER_PER_KILOMETER) {
            "${distance}m"
        } else {
            val kilometerText = String
                .format(Locale.US, "%.1f", distance / METER_PER_KILOMETER.toDouble())
                .removeSuffix(".0")
            "${kilometerText}km"
        }
    }

    private const val METER_PER_KILOMETER = 1_000
}
