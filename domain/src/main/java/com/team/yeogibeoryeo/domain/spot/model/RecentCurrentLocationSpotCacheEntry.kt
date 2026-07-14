package com.team.yeogibeoryeo.domain.spot.model

data class RecentCurrentLocationSpotCacheEntry(
    val spots: List<CollectionSpot>,
    val searchCoordinate: Coordinate,
    val savedAtMillis: Long,
) {
    fun isFresh(
        nowMillis: Long,
        ttlMillis: Long = RECENT_CURRENT_LOCATION_SPOT_CACHE_TTL_MILLIS,
    ): Boolean {
        val elapsedMillis = nowMillis - savedAtMillis

        return elapsedMillis in 0 until ttlMillis
    }
}

const val RECENT_CURRENT_LOCATION_SPOT_CACHE_TTL_MILLIS = 10 * 60 * 1_000L
