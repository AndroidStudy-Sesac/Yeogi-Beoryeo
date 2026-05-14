package com.team.yeogibeoryeo.domain.region.model

/**
 * 시/도, 시군구, 읍면동 정보를 담는 공통 행정구역 모델
 *
 * @property sido 시/도 명칭
 * @property sigungu 시/군/구 명칭
 * @property eupmyeondong 읍/면/동 명칭
 */
data class Region(
    val sido: String? = null,
    val sigungu: String? = null,
    val eupmyeondong: String? = null
)