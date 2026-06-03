package com.team.yeogibeoryeo.data.spot.cache

import kotlinx.serialization.Serializable

@Serializable
internal data class RecentCurrentLocationSpotCacheDto(
    val spots: List<CollectionSpotCacheDto>,
    val savedAtMillis: Long,
)

@Serializable
internal data class CollectionSpotCacheDto(
    val id: String,
    val name: String,
    val type: String,
    val address: String,
    val detailLocation: String?,
    val coordinate: CoordinateCacheDto?,
    val distanceMeter: Int?,
    val isBookmarked: Boolean,
)

@Serializable
internal data class CoordinateCacheDto(
    val latitude: Double,
    val longitude: Double,
)
