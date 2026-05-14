package com.team.yeogibeoryeo.domain.region.model

/**
 * 사용자의 검색어와 추출된 지역 정보를 연결하는 모델
 *
 * @property originalKeyword 원본 검색어
 * @property detectedRegion 변환된 행정구역 정보
 * @property source 지역 정보 출처
 */
data class RegionSearchContext(
    val originalKeyword: String,
    val detectedRegion: Region?,
    val source: RegionSource
)