package com.team.yeogibeoryeo.domain.spot.repository

import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheEntry

interface RecentCurrentLocationSpotCacheRepository {
    suspend fun getRecentCurrentLocationSpots(): RecentCurrentLocationSpotCacheEntry?

    suspend fun saveRecentCurrentLocationSpots(entry: RecentCurrentLocationSpotCacheEntry)

    suspend fun clearRecentCurrentLocationSpots()
}
