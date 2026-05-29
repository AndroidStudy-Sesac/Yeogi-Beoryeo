package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.parser.RegionAddressParser
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionRepository
import javax.inject.Inject

class RegionRepositoryImpl @Inject constructor(
    private val addressParser: RegionAddressParser
) : RegionRepository {

    override fun extractRegionFromAddress(address: String): Region? {
        if (address.isBlank()) return null

        return addressParser.parse(address)
            .takeIf { region -> region.hasAnyRegionComponent() }
    }

    override suspend fun resolveRegionFromKeyword(keyword: String): Region? {
        if (keyword.isBlank()) return null

        return addressParser.parse(keyword)
            .takeIf { region -> region.hasAnyRegionComponent() }
    }

    override suspend fun resolveRegionFromCoordinate(latitude: Double, longitude: Double): Region? {
        // TODO: 향후 외부 API 기반 Reverse Geocoding 연동 시 구현
        return null
    }

    private fun Region.hasAnyRegionComponent(): Boolean {
        return !sido.isNullOrBlank() ||
            !sigungu.isNullOrBlank() ||
            !eupmyeondong.isNullOrBlank()
    }
}
