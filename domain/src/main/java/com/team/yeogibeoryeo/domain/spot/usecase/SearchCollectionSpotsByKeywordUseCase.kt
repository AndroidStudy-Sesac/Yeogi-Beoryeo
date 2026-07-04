package com.team.yeogibeoryeo.domain.spot.usecase

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotRepository
import javax.inject.Inject

class SearchCollectionSpotsByKeywordUseCase @Inject constructor(
    private val repository: CollectionSpotRepository,
    private val normalizeKeywordUseCase: NormalizeCollectionSpotSearchKeywordUseCase,
) {
    suspend operator fun invoke(
        keyword: String,
        types: Set<CollectionSpotType> = emptySet()
    ): List<CollectionSpot> {
        val normalizedKeyword = normalizeKeywordUseCase(keyword)

        return repository.searchByKeyword(
            keyword = normalizedKeyword,
            types = types
        ).filterByExplicitRegionKeyword(normalizedKeyword)
    }

    private fun List<CollectionSpot>.filterByExplicitRegionKeyword(
        keyword: String,
    ): List<CollectionSpot> {
        if (!keyword.isEupMyeonDongCandidate()) return this

        return filter { spot ->
            val explicitRegions = spot.address.extractExplicitRegions()
            explicitRegions.isEmpty() ||
                explicitRegions.any { region -> region.matchesKeyword(keyword) }
        }
    }

    private fun String.extractExplicitRegions(): List<String> {
        val parenthesizedRegions = PARENTHESIZED_TEXT_REGEX
            .findAll(this)
            .flatMap { matchResult ->
                matchResult.groupValues
                    .getOrNull(1)
                    .orEmpty()
                    .split(REGION_TOKEN_DELIMITER_REGEX)
                    .asSequence()
            }

        val tokenRegions = split(REGION_TOKEN_DELIMITER_REGEX)
            .asSequence()

        return (parenthesizedRegions + tokenRegions)
            .map { token -> token.cleanToken() }
            .filter { token -> token.isEupMyeonDongCandidate() }
            .distinct()
            .toList()
    }

    private fun String.matchesKeyword(keyword: String): Boolean {
        return this == keyword ||
            (startsWith(keyword) && isLegalDongGaCandidate())
    }

    private fun String.cleanToken(): String =
        trim().trim('(', ')', '[', ']', ',', '.', ' ')

    private fun String.isEupMyeonDongCandidate(): Boolean =
        EUP_MYEON_DONG_REGEX.matches(this) ||
            isLegalDongGaCandidate()

    private fun String.isLegalDongGaCandidate(): Boolean =
        LEGAL_DONG_GA_REGEX.matches(this)

    private companion object {
        private val PARENTHESIZED_TEXT_REGEX = "\\(([^)]+)\\)".toRegex()
        private val REGION_TOKEN_DELIMITER_REGEX = "[,\\s]+".toRegex()
        private val EUP_MYEON_DONG_REGEX = """[가-힣]+\d*(동|읍|면)""".toRegex()
        private val LEGAL_DONG_GA_REGEX = """[가-힣]+\d+가""".toRegex()
    }
}
