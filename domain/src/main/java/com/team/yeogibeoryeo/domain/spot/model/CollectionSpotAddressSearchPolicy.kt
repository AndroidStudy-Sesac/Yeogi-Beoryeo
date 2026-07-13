package com.team.yeogibeoryeo.domain.spot.model

import com.team.yeogibeoryeo.domain.region.model.RegionSidoAliasPolicy

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

    fun normalizedSidoName(
        token: String,
        sigungu: String? = null,
    ): String? {
        val sido = token.toNormalizedSidoName() ?: return null

        return RegionSidoAliasPolicy.normalizeSidoForInput(
            sido = sido,
            sigungu = sigungu,
        ) ?: sido
    }

    fun isSigunguLike(token: String): Boolean =
        token.endsWith(SIGUNGU_CITY_SUFFIX) ||
            token.endsWith(SIGUNGU_COUNTY_SUFFIX) ||
            token.endsWith(SIGUNGU_DISTRICT_SUFFIX)

    fun sidoMatches(
        token: String,
        sido: String,
        requestedSigungu: String? = null,
        candidateSigungu: String? = requestedSigungu,
    ): Boolean {
        val tokenSido = token.toNormalizedSidoName() ?: return token == sido
        val requestedSido = RegionSidoAliasPolicy.normalizeSidoForInput(
            sido = sido,
            sigungu = requestedSigungu,
        ) ?: sido

        return RegionSidoAliasPolicy.isSameSido(
            requestedSido = requestedSido,
            candidateSido = tokenSido,
            candidateSigungu = candidateSigungu,
        )
    }

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

    private fun String.toNormalizedSidoName(): String? =
        RegionSidoAliasPolicy
            .normalizeSidoName(this)
            ?.takeIf { RegionSidoAliasPolicy.isSidoName(this) }

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

}
