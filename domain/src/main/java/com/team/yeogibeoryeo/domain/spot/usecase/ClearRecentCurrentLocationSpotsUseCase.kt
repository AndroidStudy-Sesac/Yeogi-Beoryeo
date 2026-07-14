package com.team.yeogibeoryeo.domain.spot.usecase

import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheClearResult
import com.team.yeogibeoryeo.domain.spot.repository.RecentCurrentLocationSpotCacheRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class ClearRecentCurrentLocationSpotsUseCase @Inject constructor(
    private val repository: RecentCurrentLocationSpotCacheRepository,
) {
    suspend operator fun invoke(): RecentCurrentLocationSpotCacheClearResult {
        return try {
            repository.clearRecentCurrentLocationSpots()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            RecentCurrentLocationSpotCacheClearResult.Failed
        }
    }
}
