package com.team.yeogibeoryeo.domain.spot.usecase

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheEntry
import com.team.yeogibeoryeo.domain.spot.repository.RecentCurrentLocationSpotCacheRepository
import com.team.yeogibeoryeo.domain.time.TimeProvider
import javax.inject.Inject

class SaveRecentCurrentLocationSpotsUseCase @Inject constructor(
    private val repository: RecentCurrentLocationSpotCacheRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(
        spots: List<CollectionSpot>,
        searchCoordinate: Coordinate,
    ) {
        repository.saveRecentCurrentLocationSpots(
            RecentCurrentLocationSpotCacheEntry(
                spots = spots,
                searchCoordinate = searchCoordinate,
                savedAtMillis = timeProvider.currentTimeMillis(),
            ),
        )
    }
}
