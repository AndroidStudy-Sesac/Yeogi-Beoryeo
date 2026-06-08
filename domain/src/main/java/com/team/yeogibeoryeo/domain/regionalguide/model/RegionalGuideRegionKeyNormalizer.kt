package com.team.yeogibeoryeo.domain.regionalguide.model

object RegionalGuideRegionKeyNormalizer {

    fun normalizeSigungu(sigungu: String): String {
        val trimmedSigungu = sigungu.trim()
        val firstToken = trimmedSigungu.split(Regex("\\s+"))
            .firstOrNull()
            ?.takeIf { token -> token.endsWith(CITY_SUFFIX) }

        if (firstToken != null) return firstToken

        val cityIndex = trimmedSigungu.indexOf(CITY_SUFFIX)
        val districtIndex = trimmedSigungu.indexOf(DISTRICT_SUFFIX)

        return if (cityIndex > 0 && districtIndex > cityIndex) {
            trimmedSigungu.substring(startIndex = 0, endIndex = cityIndex + CITY_SUFFIX.length)
        } else {
            trimmedSigungu
        }
    }

    private const val CITY_SUFFIX = "시"
    private const val DISTRICT_SUFFIX = "구"
}
