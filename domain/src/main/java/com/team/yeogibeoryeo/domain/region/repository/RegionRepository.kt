package com.team.yeogibeoryeo.domain.region.repository

import com.team.yeogibeoryeo.domain.region.model.Region

interface RegionRepository {

    fun extractRegionFromAddress(address: String): Region?

    suspend fun resolveRegionFromKeyword(keyword: String): Region?

    suspend fun resolveRegionFromCoordinate(
        latitude: Double,
        longitude: Double
    ): Region?
}
