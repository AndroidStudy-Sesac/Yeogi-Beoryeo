package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.parser.RegionAddressParser
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionRepository
import javax.inject.Inject

/**
 * [RegionRepository]의 실질적인 동작을 정의하는 Data 계층의 구현체
 * 초기 1단계에서는 로컬 주소 파서만 활용하며, 향후 Geocoder API 등 외부 데이터 소스를 추가하여 확장합니다.
 */
class RegionRepositoryImpl @Inject constructor(
    private val addressParser: RegionAddressParser
) : RegionRepository {

    override fun extractRegionFromAddress(address: String): Region? {
        if (address.isBlank()) return null
        return addressParser.parse(address)
    }

    override suspend fun resolveRegionFromKeyword(keyword: String): Region? {
        if (keyword.isBlank()) return null
        return addressParser.parse(keyword)
    }

    override suspend fun resolveRegionFromCoordinate(latitude: Double, longitude: Double): Region? {
        // TODO: 향후 외부 API 기반 Reverse Geocoding 연동 시 구현
        return null
    }
}