package com.team.yeogibeoryeo.domain.spot.usecase

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotAddressSearchPolicy
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.MapRegionSearchCandidate
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotRepository
import javax.inject.Inject

class SearchCollectionSpotsByKeywordUseCase @Inject constructor(
    private val repository: CollectionSpotRepository,
    private val normalizeKeywordUseCase: NormalizeCollectionSpotSearchKeywordUseCase,
) {
    suspend operator fun invoke(
        keyword: String,
        types: Set<CollectionSpotType> = emptySet(),
        selectedRegionCandidate: MapRegionSearchCandidate? = null,
    ): List<CollectionSpot> {
        val normalizedKeyword = normalizeKeywordUseCase(keyword)

        return repository.searchByKeyword(
            keyword = normalizedKeyword,
            types = types,
        )
            .filterByExplicitRegionKeyword(normalizedKeyword)
            .filterBySelectedRegionCandidate(selectedRegionCandidate)
    }

    private fun List<CollectionSpot>.filterByExplicitRegionKeyword(
        keyword: String,
    ): List<CollectionSpot> {
        if (!CollectionSpotAddressSearchPolicy.isEupMyeonDongCandidate(keyword)) return this

        return filter { spot ->
            val explicitRegions = CollectionSpotAddressSearchPolicy.extractExplicitRegions(spot.address)

            explicitRegions.isEmpty() ||
                explicitRegions.any { region ->
                    CollectionSpotAddressSearchPolicy.matchesEupMyeonDongKeyword(
                        regionName = region,
                        keyword = keyword,
                    )
                }
        }
    }

    private fun List<CollectionSpot>.filterBySelectedRegionCandidate(
        selectedRegionCandidate: MapRegionSearchCandidate?,
    ): List<CollectionSpot> {
        val candidate = selectedRegionCandidate ?: return this

        return filter { spot ->
            spot.address.matchesSelectedRegionCandidate(candidate)
        }
    }

    private fun String.matchesSelectedRegionCandidate(
        candidate: MapRegionSearchCandidate,
    ): Boolean {
        val tokens = CollectionSpotAddressSearchPolicy.tokenize(this)
        val selectedSido = candidate.region.sido
        val selectedSigungu = candidate.region.sigungu

        val hasAnySido = tokens.any { token ->
            CollectionSpotAddressSearchPolicy.normalizedSidoName(token) != null
        }
        val hasSelectedSido = selectedSido == null ||
            tokens.any { token -> CollectionSpotAddressSearchPolicy.sidoMatches(token, selectedSido) }
        if (hasAnySido && !hasSelectedSido) return false

        val selectedSigunguParts = selectedSigungu?.split(REGION_SCOPE_SEPARATOR).orEmpty()
        val hasAnySigungu = tokens.any { token ->
            CollectionSpotAddressSearchPolicy.normalizedSidoName(token) == null &&
                CollectionSpotAddressSearchPolicy.isSigunguLike(token)
        }
        val hasSelectedSigungu = selectedSigungu == null ||
            selectedSigungu in tokens ||
            selectedSigunguParts.all { sigunguPart -> sigunguPart in tokens }

        return !hasAnySigungu || hasSelectedSigungu
    }

    private companion object {
        const val REGION_SCOPE_SEPARATOR = " "
    }
}
