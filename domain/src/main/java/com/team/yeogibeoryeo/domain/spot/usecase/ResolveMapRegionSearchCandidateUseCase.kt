package com.team.yeogibeoryeo.domain.spot.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotAddressSearchPolicy
import com.team.yeogibeoryeo.domain.spot.model.MapRegionSearchCandidate
import com.team.yeogibeoryeo.domain.spot.model.MapRegionSearchCandidateResult
import javax.inject.Inject

class ResolveMapRegionSearchCandidateUseCase @Inject constructor(
    private val regionOptionsRepository: RegionOptionsRepository,
    private val normalizeKeywordUseCase: NormalizeCollectionSpotSearchKeywordUseCase,
) {
    suspend operator fun invoke(keyword: String): MapRegionSearchCandidateResult {
        val trimmedKeyword = keyword.trim()
        val searchKeyword = normalizeKeywordUseCase(trimmedKeyword)

        if (
            trimmedKeyword.isBlank() ||
            !CollectionSpotAddressSearchPolicy.isEupMyeonDongCandidate(searchKeyword)
        ) {
            return MapRegionSearchCandidateResult.ReadyToSearch(searchKeyword)
        }

        val matchedRegions = regionOptionsRepository.findRegionsByEupmyeondongKeyword(searchKeyword)
            .filter { region -> region.eupmyeondong.matchesSearchKeyword(searchKeyword) }
            .filterByScopeTokens(
                tokens = CollectionSpotAddressSearchPolicy.tokenize(trimmedKeyword),
                searchKeyword = searchKeyword,
            )
        val fallbackRegion = CollectionSpotAddressSearchPolicy.tokenize(trimmedKeyword)
            .toScopedFallbackRegion(searchKeyword)
            .takeIf { matchedRegions.isEmpty() }
        val candidates = (matchedRegions + listOfNotNull(fallbackRegion))
            .toCandidates(searchKeyword)

        return when (candidates.size) {
            0 -> MapRegionSearchCandidateResult.ReadyToSearch(searchKeyword)
            1 -> MapRegionSearchCandidateResult.ReadyToSearch(
                searchKeyword = searchKeyword,
                selectedCandidate = candidates.first(),
            )

            else -> MapRegionSearchCandidateResult.NeedSelection(
                originalKeyword = trimmedKeyword,
                searchKeyword = searchKeyword,
                candidates = candidates,
            )
        }
    }

    private fun List<Region>.filterByScopeTokens(
        tokens: List<String>,
        searchKeyword: String,
    ): List<Region> {
        val scopeTokens = tokens
            .filterNot { token ->
                token == searchKeyword ||
                    CollectionSpotAddressSearchPolicy.matchesEupMyeonDongKeyword(token, searchKeyword)
            }
            .filter { token ->
                CollectionSpotAddressSearchPolicy.normalizedSidoName(token) != null ||
                    CollectionSpotAddressSearchPolicy.isSigunguLike(token)
            }

        if (scopeTokens.isEmpty()) return this

        return filter { region ->
            scopeTokens.all { token -> region.matchesScopeToken(token) }
        }
    }

    private fun Region.matchesScopeToken(token: String): Boolean {
        val sidoMatches = sido?.let { sido ->
            CollectionSpotAddressSearchPolicy.sidoMatches(token, sido)
        } ?: false
        val sigunguMatches = sigungu
            ?.split(REGION_SCOPE_SEPARATOR)
            ?.any { sigunguPart -> sigunguPart == token } == true ||
            sigungu == token

        return sidoMatches || sigunguMatches
    }

    private suspend fun List<Region>.toCandidates(searchKeyword: String): List<MapRegionSearchCandidate> {
        val candidates = mutableListOf<MapRegionSearchCandidate>()

        for (region in this) {
            candidates += region.toCandidate(searchKeyword)
        }

        return candidates
    }

    private suspend fun Region.toCandidate(searchKeyword: String): MapRegionSearchCandidate {
        val legalDongKeywords = regionOptionsRepository.findLegalDongKeywordsByRegion(
            region = this,
            keyword = searchKeyword,
        )
        val searchKeywords = (listOf(searchKeyword) + legalDongKeywords)
            .distinct()

        return MapRegionSearchCandidate(
            region = this,
            searchKeyword = searchKeyword,
            searchKeywords = searchKeywords,
        )
    }

    private fun List<String>.toScopedFallbackRegion(searchKeyword: String): Region? {
        val scopeTokens = filterNot { token ->
            token == searchKeyword ||
                CollectionSpotAddressSearchPolicy.matchesEupMyeonDongKeyword(token, searchKeyword)
        }
        if (scopeTokens.isEmpty()) return null

        val sido = scopeTokens
            .firstNotNullOfOrNull { token -> CollectionSpotAddressSearchPolicy.normalizedSidoName(token) }
        val sigungu = scopeTokens.toSigunguName()

        if (sido == null && sigungu == null) return null

        return Region(
            sido = sido,
            sigungu = sigungu,
            eupmyeondong = searchKeyword,
        )
    }

    private fun List<String>.toSigunguName(): String? {
        val sigunguTokens = filter { token ->
            CollectionSpotAddressSearchPolicy.normalizedSidoName(token) == null &&
                CollectionSpotAddressSearchPolicy.isSigunguLike(token)
        }
        if (sigunguTokens.isEmpty()) return null

        return sigunguTokens
            .zipWithNext()
            .firstOrNull { (current, next) ->
                current.endsWith(CITY_SUFFIX) && next.endsWith(DISTRICT_SUFFIX)
            }
            ?.let { (city, district) -> "$city $district" }
            ?: sigunguTokens.first()
    }

    private fun String?.matchesSearchKeyword(searchKeyword: String): Boolean {
        val regionName = this ?: return false
        return CollectionSpotAddressSearchPolicy.matchesEupMyeonDongKeyword(
            regionName = regionName,
            keyword = searchKeyword,
        )
    }

    private companion object {
        const val REGION_SCOPE_SEPARATOR = " "
        const val CITY_SUFFIX = "시"
        const val DISTRICT_SUFFIX = "구"
    }
}
