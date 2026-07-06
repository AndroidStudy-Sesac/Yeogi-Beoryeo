package com.team.yeogibeoryeo.domain.spot.repository

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotSearchResult
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate

interface CollectionSpotRepository {

    suspend fun searchByKeyword(
        keyword: String,
        types: Set<CollectionSpotType> = emptySet()
    ): List<CollectionSpot>

    suspend fun searchByKeywordResult(
        keyword: String,
        types: Set<CollectionSpotType> = emptySet()
    ): CollectionSpotSearchResult {
        return CollectionSpotSearchResult(
            spots = searchByKeyword(
                keyword = keyword,
                types = types,
            ),
        )
    }

    suspend fun searchByLocation(
        coordinate: Coordinate,
        radiusMeter: Int,
        types: Set<CollectionSpotType> = emptySet()
    ): List<CollectionSpot>

    suspend fun geocodeSpot(
        spot: CollectionSpot
    ): CollectionSpot
}
