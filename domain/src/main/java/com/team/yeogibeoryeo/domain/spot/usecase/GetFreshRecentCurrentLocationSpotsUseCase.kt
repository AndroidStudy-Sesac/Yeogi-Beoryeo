package com.team.yeogibeoryeo.domain.spot.usecase

import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheEntry
import com.team.yeogibeoryeo.domain.spot.repository.RecentCurrentLocationSpotCacheRepository
import com.team.yeogibeoryeo.domain.time.TimeProvider
import javax.inject.Inject

class GetFreshRecentCurrentLocationSpotsUseCase @Inject constructor(
    private val repository: RecentCurrentLocationSpotCacheRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(): RecentCurrentLocationSpotCacheEntry? {
        val entry = repository.getRecentCurrentLocationSpots() ?: return null

        return entry.takeIf {
            it.isFresh(nowMillis = timeProvider.currentTimeMillis())
        }
    }
}
