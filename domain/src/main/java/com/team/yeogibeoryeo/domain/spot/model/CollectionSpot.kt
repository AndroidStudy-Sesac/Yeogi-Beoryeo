package com.team.yeogibeoryeo.domain.spot.model

data class CollectionSpot(
    val id: String,
    val name: String,
    val type: CollectionSpotType,
    val address: String,
    val detailLocation: String?,
    val coordinate: Coordinate?,
    val distanceMeter: Int? = null,
    val isBookmarked: Boolean = false
)