package com.team.yeogibeoryeo.domain.spot.log

interface MapSearchTimingLogger {
    fun log(message: String)

    companion object {
        val NoOp: MapSearchTimingLogger = object : MapSearchTimingLogger {
            override fun log(message: String) = Unit
        }
    }
}
