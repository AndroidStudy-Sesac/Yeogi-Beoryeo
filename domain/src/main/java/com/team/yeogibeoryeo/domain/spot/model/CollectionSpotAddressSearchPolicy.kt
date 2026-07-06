package com.team.yeogibeoryeo.domain.spot.model

object CollectionSpotAddressSearchPolicy {
    fun normalizeKeyword(keyword: String): String {
        val trimmedKeyword = keyword.trim()
        if (trimmedKeyword.isBlank()) return trimmedKeyword

        val tokens = tokenize(trimmedKeyword)
        if (tokens.any { token -> token.hasAddressNumber() || token.isRoadNameLike() }) {
            return trimmedKeyword
        }

        return tokens.lastOrNull { token -> token.hasEupMyeonDongShape() } ?: trimmedKeyword
    }

    fun tokenize(value: String): List<String> =
        value
            .trim()
            .split(REGION_TOKEN_DELIMITER_REGEX)
            .map { token -> token.cleanToken() }
            .filter { token -> token.isNotBlank() }

    fun extractExplicitRegions(address: String): List<String> {
        val parenthesizedRegions = PARENTHESIZED_TEXT_REGEX.findAll(address).flatMap { matchResult ->
            matchResult.groupValues.getOrNull(1).orEmpty()
                .split(REGION_TOKEN_DELIMITER_REGEX)
                .asSequence()
        }
        val tokenRegions = address.split(REGION_TOKEN_DELIMITER_REGEX).asSequence()

        return (parenthesizedRegions + tokenRegions)
            .map { token -> token.cleanToken() }
            .filter { token -> token.hasEupMyeonDongShape() }
            .distinct()
            .toList()
    }

    fun matchesEupMyeonDongKeyword(regionName: String, keyword: String): Boolean =
        regionName == keyword || (regionName.startsWith(keyword) && regionName.isLegalDongGaCandidate())

    fun isEupMyeonDongCandidate(token: String): Boolean =
        token.hasEupMyeonDongShape()

    fun normalizedSidoName(token: String): String? =
        when (token) {
            in SIDO_NAMES -> SIDO_ALIASES[token] ?: token
            in SIDO_ALIASES -> SIDO_ALIASES[token]
            else -> null
        }

    fun isSigunguLike(token: String): Boolean =
        token.endsWith(SIGUNGU_CITY_SUFFIX) ||
            token.endsWith(SIGUNGU_COUNTY_SUFFIX) ||
            token.endsWith(SIGUNGU_DISTRICT_SUFFIX)

    fun sidoMatches(token: String, sido: String): Boolean =
        normalizedSidoName(token) == sido || token == sido

    private fun String.cleanToken(): String =
        trim().trim('(', ')', '[', ']', ',', '.', ' ')

    private fun String.hasAddressNumber(): Boolean =
        ADDRESS_NUMBER_REGEX.matches(this)

    private fun String.isRoadNameLike(): Boolean {
        val nameWithoutNumbers = replace(DIGIT_REGEX, "")
        return nameWithoutNumbers.endsWith(ROAD_NAME_SUFFIX) ||
            nameWithoutNumbers.endsWith(ROAD_DETAIL_SUFFIX)
    }

    private fun String.hasEupMyeonDongShape(): Boolean =
        EUP_MYEON_DONG_REGEX.matches(this) || isLegalDongGaCandidate()

    private fun String.isLegalDongGaCandidate(): Boolean =
        LEGAL_DONG_GA_REGEX.matches(this)

    private const val ROAD_NAME_SUFFIX = "로"
    private const val ROAD_DETAIL_SUFFIX = "길"
    private const val EUP_SUFFIX = "읍"
    private const val MYEON_SUFFIX = "면"
    private const val DONG_SUFFIX = "동"
    private const val LEGAL_DONG_GA_SUFFIX = "가"
    private const val SIGUNGU_CITY_SUFFIX = "시"
    private const val SIGUNGU_COUNTY_SUFFIX = "군"
    private const val SIGUNGU_DISTRICT_SUFFIX = "구"

    private val REGION_TOKEN_DELIMITER_REGEX = "[,\\s]+".toRegex()
    private val PARENTHESIZED_TEXT_REGEX = "\\(([^)]+)\\)".toRegex()
    private val DIGIT_REGEX = "\\d+".toRegex()
    private val ADDRESS_NUMBER_REGEX = """\d+(-\d+)?""".toRegex()
    private val EUP_MYEON_DONG_REGEX =
        """[가-힣]+\d*[$EUP_SUFFIX$MYEON_SUFFIX$DONG_SUFFIX]""".toRegex()
    private val LEGAL_DONG_GA_REGEX = """[가-힣]+\d+$LEGAL_DONG_GA_SUFFIX""".toRegex()

    private val SIDO_NAMES = setOf(
        "서울특별시",
        "부산광역시",
        "대구광역시",
        "인천광역시",
        "광주광역시",
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
        "전라남도",
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
        "광주" to "광주광역시",
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
        "전남" to "전라남도",
        "경북" to "경상북도",
        "경남" to "경상남도",
        "제주" to "제주특별자치도",
        "제주도" to "제주특별자치도",
    )
}
