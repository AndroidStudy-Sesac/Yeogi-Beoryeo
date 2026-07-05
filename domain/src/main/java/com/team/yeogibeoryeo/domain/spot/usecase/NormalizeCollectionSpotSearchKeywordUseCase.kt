package com.team.yeogibeoryeo.domain.spot.usecase

import javax.inject.Inject

class NormalizeCollectionSpotSearchKeywordUseCase @Inject constructor() {

    operator fun invoke(keyword: String): String {
        val trimmedKeyword = keyword.trim()
        if (trimmedKeyword.isBlank()) return trimmedKeyword

        val tokens = trimmedKeyword
            .split(WHITESPACE_REGEX)
            .map { token -> token.cleanToken() }
            .filter { token -> token.isNotBlank() }

        if (tokens.any { token -> token.hasAddressNumber() || token.isRoadNameLike() }) {
            return trimmedKeyword
        }

        return tokens
            .lastOrNull { token -> token.isEupMyeonDongCandidate() }
            ?: trimmedKeyword
    }

    private fun String.cleanToken(): String =
        trim().trim('(', ')', '[', ']', ',', '.', ' ')

    private fun String.hasAddressNumber(): Boolean =
        ADDRESS_NUMBER_REGEX.matches(this)

    private fun String.isRoadNameLike(): Boolean {
        val nameWithoutNumbers = replace(DIGIT_REGEX, "")
        return nameWithoutNumbers.endsWith("로") || nameWithoutNumbers.endsWith("길")
    }

    private fun String.isEupMyeonDongCandidate(): Boolean =
        EUP_MYEON_DONG_REGEX.matches(this) ||
            LEGAL_DONG_GA_REGEX.matches(this)

    private companion object {
        private val WHITESPACE_REGEX = "\\s+".toRegex()
        private val DIGIT_REGEX = "\\d+".toRegex()
        private val ADDRESS_NUMBER_REGEX = """\d+(-\d+)?""".toRegex()
        private val EUP_MYEON_DONG_REGEX = """[가-힣]+\d*[동읍면]""".toRegex()
        private val LEGAL_DONG_GA_REGEX = """[가-힣]+\d+가""".toRegex()
    }
}
