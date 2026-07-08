package com.team.yeogibeoryeo.domain.spot.repository

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotSearchResult
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate

interface CollectionSpotRepository {

    suspend fun searchRawByKeyword(
        keyword: String,
        types: Set<CollectionSpotType> = emptySet()
    ): CollectionSpotSearchResult

    suspend fun searchRawByLocation(
        coordinate: Coordinate,
        radiusMeter: Int,
        types: Set<CollectionSpotType> = emptySet()
    ): List<CollectionSpot>
}
