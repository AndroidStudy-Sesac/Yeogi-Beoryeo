package com.team.yeogibeoryeo.domain.region.model

/**
 * 지역 정보의 출처를 구분하는 Enum
 */
enum class RegionSource {

    /** 사용자가 직접 입력한 검색어 */
    USER_INPUT,

    /** GPS 좌표 기반 Reverse Geocoding 결과 */
    GPS_REVERSE_GEOCODING,

    /** 장소 정보 주소에서 추출한 지역 정보 */
    SPOT_ADDRESS,

    /** 사용자가 수동으로 선택한 지역 정보 */
    MANUAL_SELECTED
}