package com.team.yeogibeoryeo.domain.spot.model

data class CollectionSpotSearchResult(
    val spots: List<CollectionSpot>,
    val isPartial: Boolean = false,
)
