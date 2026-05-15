package com.team.yeogibeoryeo.data.region.parser

import com.team.yeogibeoryeo.data.region.RegionNormalizer
import com.team.yeogibeoryeo.domain.region.model.Region
import javax.inject.Inject

/**
 * 주소 문자열을 분석하여 [Region] 객체로 매핑하는 파서 (Parser)
 * 띄어쓰기 및 접미사 단위로 행정구역을 추출한 뒤, [RegionNormalizer]를 거쳐 최종 반환합니다.
 */
class RegionAddressParser @Inject constructor() {

    fun parse(address: String): Region {
        val parts = address.trim().split("\\s+".toRegex())

        var sido: String? = null
        var sigungu: String? = null
        var eupmyeondong: String? = null

        parts.forEach { part ->
            when {
                (part.endsWith("도") || part.endsWith("시") || part in SIDO_ABBR) && sido == null -> {
                    sido = part
                }
                (part.endsWith("시") || part.endsWith("군") || part.endsWith("구")) && part != sido -> {
                    if (sigungu == null) sigungu = part
                }
                part.endsWith("읍") || part.endsWith("면") || part.endsWith("동") -> {
                    if (eupmyeondong == null) eupmyeondong = part
                }
            }
        }

        // 1차 파싱된 데이터를 정규화 유틸리티를 거쳐 최종 포맷으로 변환
        val parsedRegion = Region(sido = sido, sigungu = sigungu, eupmyeondong = eupmyeondong)
        return RegionNormalizer.normalize(parsedRegion)
    }

    companion object {
        private val SIDO_ABBR = setOf(
            "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
            "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주"
        )
    }
}