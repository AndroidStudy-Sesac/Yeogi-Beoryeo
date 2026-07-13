package com.team.yeogibeoryeo.domain.region.model

object RegionSidoAliasPolicy {

    fun normalizeInputRegion(region: Region): Region {
        val normalizedSido = normalizeSidoForInput(
            sido = region.sido,
            sigungu = region.sigungu,
        )

        return region.copy(sido = normalizedSido ?: region.sido)
    }

    fun isSidoName(name: String): Boolean {
        val normalizedName = name.normalizeRegionName() ?: return false

        return normalizedName in OFFICIAL_SIDO_NAMES || normalizedName in SIDO_ALIASES
    }

    fun normalizeSidoName(sido: String?): String? {
        val normalizedSido = sido.normalizeRegionName() ?: return null

        return SIDO_ALIASES[normalizedSido] ?: normalizedSido
    }

    fun normalizeSidoForInput(
        sido: String?,
        sigungu: String?,
    ): String? {
        val normalizedSido = normalizeSidoName(sido) ?: return null
        val normalizedSigungu = sigungu.normalizeRegionName()

        return when {
            normalizedSido != GWANGJU_JEONNAM_INTEGRATED_SIDO -> normalizedSido
            isGwangjuSigungu(normalizedSigungu) -> GWANGJU_SIDO
            normalizedSigungu != null -> JEONNAM_SIDO
            else -> normalizedSido
        }
    }

    fun resolveIntegratedSido(
        sigungu: String?,
    ): String =
        if (isGwangjuSigungu(sigungu)) {
            GWANGJU_SIDO
        } else {
            JEONNAM_SIDO
        }

    fun isSameSido(
        requestedSido: String?,
        requestedSigungu: String?,
        candidateSido: String?,
        candidateSigungu: String?,
    ): Boolean {
        val requested = normalizeSidoForInput(
            sido = requestedSido,
            sigungu = requestedSigungu,
        ) ?: return true
        val candidate = normalizeSidoForInput(
            sido = candidateSido,
            sigungu = candidateSigungu,
        ) ?: return false

        return requested == candidate
    }

    private fun String?.normalizeRegionName(): String? =
        this
            ?.trim()
            ?.takeIf { value -> value.isNotBlank() }

    private fun isGwangjuSigungu(sigungu: String?): Boolean =
        sigungu.normalizeRegionName() in GWANGJU_SIGUNGU_NAMES

    const val GWANGJU_SIDO = "광주광역시"
    const val JEONNAM_SIDO = "전라남도"
    const val GWANGJU_JEONNAM_INTEGRATED_SIDO = "전남광주통합특별시"

    private val OFFICIAL_SIDO_NAMES = setOf(
        "서울특별시",
        "부산광역시",
        "대구광역시",
        "인천광역시",
        GWANGJU_SIDO,
        "대전광역시",
        "울산광역시",
        "세종특별자치시",
        "경기도",
        "강원특별자치도",
        "강원도",
        "충청북도",
        "충청남도",
        "전북특별자치도",
        "전라북도",
        JEONNAM_SIDO,
        GWANGJU_JEONNAM_INTEGRATED_SIDO,
        "경상북도",
        "경상남도",
        "제주특별자치도",
        "제주도",
    )

    private val SIDO_ALIASES = mapOf(
        "서울" to "서울특별시",
        "서울시" to "서울특별시",
        "부산" to "부산광역시",
        "부산시" to "부산광역시",
        "대구" to "대구광역시",
        "대구시" to "대구광역시",
        "인천" to "인천광역시",
        "인천시" to "인천광역시",
        "광주" to GWANGJU_SIDO,
        "대전" to "대전광역시",
        "대전시" to "대전광역시",
        "울산" to "울산광역시",
        "울산시" to "울산광역시",
        "세종" to "세종특별자치시",
        "세종시" to "세종특별자치시",
        "경기" to "경기도",
        "강원" to "강원특별자치도",
        "강원도" to "강원특별자치도",
        "충북" to "충청북도",
        "충남" to "충청남도",
        "전북" to "전북특별자치도",
        "전라북도" to "전북특별자치도",
        "전남" to JEONNAM_SIDO,
        "경북" to "경상북도",
        "경남" to "경상남도",
        "제주" to "제주특별자치도",
        "제주도" to "제주특별자치도",
    )

    private val GWANGJU_SIGUNGU_NAMES = setOf(
        "동구",
        "서구",
        "남구",
        "북구",
        "광산구",
    )
}
