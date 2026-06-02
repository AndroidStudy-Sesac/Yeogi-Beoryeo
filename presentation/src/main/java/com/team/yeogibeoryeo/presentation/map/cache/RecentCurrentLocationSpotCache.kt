package com.team.yeogibeoryeo.presentation.map.cache

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot

interface RecentCurrentLocationSpotCache {
    suspend fun getRecentCurrentLocationSpots(): RecentCurrentLocationSpotCacheEntry?

    suspend fun saveRecentCurrentLocationSpots(entry: RecentCurrentLocationSpotCacheEntry)

    suspend fun clearRecentCurrentLocationSpots()
}

data class RecentCurrentLocationSpotCacheEntry(
    val spots: List<CollectionSpot>,
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
