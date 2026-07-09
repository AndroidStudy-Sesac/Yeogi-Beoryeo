package com.team.yeogibeoryeo.domain.region.model

object RegionSidoAliasPolicy {

    fun normalizeInputRegion(region: Region): Region {
        val normalizedSido = normalizeSidoForInput(
            sido = region.sido,
            sigungu = region.sigungu,
        )

        return region.copy(sido = normalizedSido ?: region.sido)
    }

    fun normalizeSidoForInput(
        sido: String?,
        sigungu: String?,
    ): String? {
        val normalizedSido = sido.normalizeRegionName() ?: return null
        val normalizedSigungu = sigungu.normalizeRegionName()

        return when {
            normalizedSido != GWANGJU_JEONNAM_INTEGRATED_SIDO -> normalizedSido
            normalizedSigungu in GWANGJU_SIGUNGU_NAMES -> GWANGJU_SIDO
            normalizedSigungu != null -> JEONNAM_SIDO
            else -> normalizedSido
        }
    }

    fun isSameSido(
        requestedSido: String?,
        candidateSido: String?,
        candidateSigungu: String?,
    ): Boolean {
        val requested = requestedSido.normalizeRegionName() ?: return true
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

    private const val GWANGJU_SIDO = "광주광역시"
    private const val JEONNAM_SIDO = "전라남도"
    const val GWANGJU_JEONNAM_INTEGRATED_SIDO = "전남광주통합특별시"

    private val GWANGJU_SIGUNGU_NAMES = setOf(
        "동구",
        "서구",
        "남구",
        "북구",
        "광산구",
    )
}
