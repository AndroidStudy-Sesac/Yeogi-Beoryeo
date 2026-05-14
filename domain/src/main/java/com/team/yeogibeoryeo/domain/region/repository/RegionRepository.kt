package com.team.yeogibeoryeo.domain.region.repository

import com.team.yeogibeoryeo.domain.region.model.Region

/**
 * 행정구역(Region) 데이터 변환 및 추출을 담당하는 Repository
 */
interface RegionRepository {

    /**
     * 주소 문자열에서 행정구역을 파싱합니다.
     * 로컬 파싱 로직 기반으로 즉시 결과를 반환합니다.
     *
     * @param address 전체 주소 문자열
     */
    fun extractRegionFromAddress(address: String): Region?

    /**
     * 검색어 기반 행정구역 분석을 수행합니다.
     * 향후 API 연동 가능성을 고려해 비동기로 처리합니다.
     *
     * @param keyword 검색어 (예: "문래동")
     */
    suspend fun resolveRegionFromKeyword(keyword: String): Region?

    /**
     * 좌표 기반 행정구역 변환(Reverse Geocoding)을 수행합니다.
     */
    suspend fun resolveRegionFromCoordinate(
        latitude: Double,
        longitude: Double
    ): Region?
}