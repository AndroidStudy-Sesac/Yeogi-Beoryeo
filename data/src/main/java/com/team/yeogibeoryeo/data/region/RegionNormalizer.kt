package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.model.RegionSidoAliasPolicy

/**
 * 행정구역 명칭을 배출 가이드 API(/info) 표준 포맷으로 보정하는 유틸리티
 * 축약어(예: "서울")를 공식 명칭("서울특별시")으로 변환하고 특정 지역의 예외 케이스를 처리합니다.
 */
object RegionNormalizer {

    internal fun isSidoName(name: String): Boolean =
        RegionSidoAliasPolicy.isSidoName(name)

    fun normalize(region: Region): Region {
        val normalizedSido = RegionSidoAliasPolicy.normalizeSidoName(region.sido)

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
