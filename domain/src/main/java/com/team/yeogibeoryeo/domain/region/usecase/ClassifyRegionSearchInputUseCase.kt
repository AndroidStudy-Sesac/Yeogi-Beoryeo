package com.team.yeogibeoryeo.domain.region.usecase

import com.team.yeogibeoryeo.domain.region.model.RegionSidoAliasPolicy
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
        RegionSidoAliasPolicy.isSidoName(this)

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

    }
}

enum class RegionSearchInputType {
    ADDRESS,
    REGION_KEYWORD
}
