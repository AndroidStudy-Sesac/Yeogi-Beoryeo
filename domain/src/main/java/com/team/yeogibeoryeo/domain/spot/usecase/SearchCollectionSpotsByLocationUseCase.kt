package com.team.yeogibeoryeo.domain.spot.usecase

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.log.MapSearchTimingLogger
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotGeocodingRepository
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotRepository
import javax.inject.Inject

class SearchCollectionSpotsByLocationUseCase @Inject constructor(
    private val repository: CollectionSpotRepository,
    private val geocodingRepository: CollectionSpotGeocodingRepository,
    private val mapSearchTimingLogger: MapSearchTimingLogger = MapSearchTimingLogger.NoOp,
) {
    suspend operator fun invoke(
        coordinate: Coordinate,
        radiusMeter: Int,
        types: Set<CollectionSpotType> = emptySet()
    ): List<CollectionSpot> {
        val searchStartedAtNanos = System.nanoTime()
        val spots = repository.searchRawByLocation(
            coordinate = coordinate,
            radiusMeter = radiusMeter,
            types = types
        )
        return geocodingRepository.geocodeSpots(spots)
            .also { geocodedSpots ->
                mapSearchTimingLogger.log(
                    "repository search finished finalCount=${geocodedSpots.size} " +
                        "elapsedMs=${searchStartedAtNanos.elapsedMs()}",
                )
            }
    }
}

private fun Long.elapsedMs(): Long =
    (System.nanoTime() - this) / NANOS_PER_MILLISECOND

private const val NANOS_PER_MILLISECOND = 1_000_000L
