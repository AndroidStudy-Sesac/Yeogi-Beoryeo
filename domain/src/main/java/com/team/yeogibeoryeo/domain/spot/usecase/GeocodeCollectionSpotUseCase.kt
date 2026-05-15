package com.team.yeogibeoryeo.domain.spot.usecase

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotRepository
import javax.inject.Inject

class GeocodeCollectionSpotUseCase @Inject constructor(
    private val repository: CollectionSpotRepository
) {
    suspend operator fun invoke(
        spot: CollectionSpot
    ): CollectionSpot {
        return repository.geocodeSpot(spot)
    }
}