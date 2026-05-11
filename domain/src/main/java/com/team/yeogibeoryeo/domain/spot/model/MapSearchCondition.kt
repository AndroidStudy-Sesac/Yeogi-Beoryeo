package com.team.yeogibeoryeo.domain.spot.model

data class MapSearchCondition(
    val keyword: String? = null,
    val currentCoordinate: Coordinate? = null,
    val radiusMeter: Int? = null,
    val selectedTypes: Set<CollectionSpotType> = emptySet()
)