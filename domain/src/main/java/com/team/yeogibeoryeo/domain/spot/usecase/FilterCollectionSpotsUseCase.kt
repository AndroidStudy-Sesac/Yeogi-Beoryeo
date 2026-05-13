package com.team.yeogibeoryeo.domain.spot.usecase

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import javax.inject.Inject

class FilterCollectionSpotsUseCase @Inject constructor() {

    operator fun invoke(
        spots: List<CollectionSpot>,
        selectedTypes: Set<CollectionSpotType>
    ): List<CollectionSpot> {
        if (selectedTypes.isEmpty()) return spots

        return spots.filter { spot ->
            spot.type in selectedTypes
        }
    }
}