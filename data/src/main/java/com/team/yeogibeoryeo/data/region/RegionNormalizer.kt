package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.domain.region.model.Region

/**
 * 행정구역 명칭을 배출 가이드 API(/info) 표준 포맷으로 보정하는 유틸리티
 * 축약어(예: "서울")를 공식 명칭("서울특별시")으로 변환하고 특정 지역의 예외 케이스를 처리합니다.
 */
object RegionNormalizer {

    private val sidoMap = mapOf(
        "서울" to "서울특별시", "서울시" to "서울특별시",
        "부산" to "부산광역시", "부산시" to "부산광역시",
        "대구" to "대구광역시", "대구시" to "대구광역시",
        "인천" to "인천광역시", "인천시" to "인천광역시",
        "광주" to "광주광역시", "광주시" to "광주광역시",
        "대전" to "대전광역시", "대전시" to "대전광역시",
        "울산" to "울산광역시", "울산시" to "울산광역시",
        "세종" to "세종특별자치시", "세종시" to "세종특별자치시",
        "경기" to "경기도",
        "강원" to "강원특별자치도",
        "충북" to "충청북도",
        "충남" to "충청남도",
        "전북" to "전북특별자치도",
        "전남" to "전라남도",
        "경북" to "경상북도",
        "경남" to "경상남도",
        "제주" to "제주특별자치도"
    )

    fun normalize(region: Region): Region {
        val normalizedSido = region.sido?.let { sidoMap[it] ?: it }

        // [도메인 지식]: 세종특별자치시는 하위 구가 없으므로 API 오류 방지를 위해 sigungu 제거
        val normalizedSigungu = if (normalizedSido == "세종특별자치시") {
            null
        } else {
            region.sigungu
        }

        return region.copy(
            sido = normalizedSido,
            sigungu = normalizedSigungu
        )
    }
}