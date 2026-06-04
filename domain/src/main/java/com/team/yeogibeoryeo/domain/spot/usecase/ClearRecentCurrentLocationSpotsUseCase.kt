package com.team.yeogibeoryeo.domain.spot.usecase

import com.team.yeogibeoryeo.domain.spot.repository.RecentCurrentLocationSpotCacheRepository
import javax.inject.Inject

class ClearRecentCurrentLocationSpotsUseCase @Inject constructor(
    private val repository: RecentCurrentLocationSpotCacheRepository,
) {
    suspend operator fun invoke() {
        repository.clearRecentCurrentLocationSpots()
    }
}
