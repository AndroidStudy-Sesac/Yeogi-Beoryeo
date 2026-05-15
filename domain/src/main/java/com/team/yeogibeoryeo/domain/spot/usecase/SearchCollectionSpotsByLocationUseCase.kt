package com.team.yeogibeoryeo.domain.spot.usecase

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotRepository
import javax.inject.Inject

class SearchCollectionSpotsByLocationUseCase @Inject constructor(
    private val repository: CollectionSpotRepository
) {
    suspend operator fun invoke(
        coordinate: Coordinate,
        radiusMeter: Int,
        types: Set<CollectionSpotType> = emptySet()
    ): List<CollectionSpot> {
        return repository.searchByLocation(
            coordinate = coordinate,
            radiusMeter = radiusMeter,
            types = types
        )
    }
}