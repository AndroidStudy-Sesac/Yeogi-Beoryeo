package com.team.yeogibeoryeo.domain.spot.repository

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot

interface CollectionSpotGeocodingRepository {

    suspend fun geocodeSpots(
        spots: List<CollectionSpot>
    ): List<CollectionSpot> {
        return spots.map { spot -> geocodeSpot(spot) }
    }

    suspend fun geocodeSpot(
        spot: CollectionSpot
    ): CollectionSpot
}
