package com.team.yeogibeoryeo.domain.region.usecase

import javax.inject.Inject

class ClassifyRegionSearchInputUseCase @Inject constructor() {

    operator fun invoke(input: String): RegionSearchInputType {
        val tokens = input
            .trim()
            .split(WHITESPACE_REGEX)
            .map { token -> token.cleanToken() }
            .filter { token -> token.isNotBlank() }

        if (tokens.isEmpty()) return RegionSearchInputType.REGION_KEYWORD

        val hasSido = tokens.any { token -> token.isSidoLike() }
        val sigunguCount = tokens.count { token -> token.isSigunguLike() }
        val hasCompoundSigungu = tokens
            .zipWithNext()
            .any { (current, next) -> current.endsWith("시") && next.endsWith("구") }
        val hasAddressNumber = tokens.any { token -> token.hasAddressNumber() }
        val hasRoadName = tokens.any { token -> token.isRoadNameLike() }
        val hasRegionScope = (hasSido && sigunguCount > 0) || hasCompoundSigungu

        return if (hasRegionScope && (hasAddressNumber || hasRoadName)) {
            RegionSearchInputType.ADDRESS
        } else {
            RegionSearchInputType.REGION_KEYWORD
        }
    }

    private fun String.cleanToken(): String =
        trim().trim('(', ')', '[', ']', ',', '.', ' ')

    private fun String.isSidoLike(): Boolean =
        this in SIDO_NAMES || this in SIDO_ALIASES

    private fun String.isSigunguLike(): Boolean =
        endsWith("시") || endsWith("군") || endsWith("구")

    private fun String.isRoadNameLike(): Boolean {
        val nameWithoutNumbers = replace(DIGIT_REGEX, "")
        return nameWithoutNumbers.endsWith("로") || nameWithoutNumbers.endsWith("길")
    }

    private fun String.hasAddressNumber(): Boolean =
        ADDRESS_NUMBER_REGEX.matches(this)

    private companion object {
        private val WHITESPACE_REGEX = "\\s+".toRegex()
        private val DIGIT_REGEX = "\\d+".toRegex()
        private val ADDRESS_NUMBER_REGEX = """\d+(-\d+)?""".toRegex()

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
            "제주도"
        )

        private val SIDO_ALIASES = setOf(
            "서울",
            "서울시",
            "부산",
            "부산시",
            "대구",
            "대구시",
            "인천",
            "인천시",
            "광주",
            "대전",
            "대전시",
            "울산",
            "울산시",
            "세종",
            "세종시",
            "경기",
            "강원",
            "충북",
            "충남",
            "전북",
            "전남",
            "경북",
            "경남",
            "제주"
        )
    }
}

enum class RegionSearchInputType {
    ADDRESS,
    REGION_KEYWORD
}
