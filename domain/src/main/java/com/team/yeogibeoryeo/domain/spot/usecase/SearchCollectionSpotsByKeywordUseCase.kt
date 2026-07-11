package com.team.yeogibeoryeo.domain.spot.usecase

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotAddressSearchPolicy
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotSearchResult
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.MapRegionSearchCandidate
import com.team.yeogibeoryeo.domain.spot.log.MapSearchTimingLogger
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotGeocodingRepository
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotRepository
import javax.inject.Inject

class SearchCollectionSpotsByKeywordUseCase @Inject constructor(
    private val repository: CollectionSpotRepository,
    private val geocodingRepository: CollectionSpotGeocodingRepository,
    private val normalizeKeywordUseCase: NormalizeCollectionSpotSearchKeywordUseCase,
    private val mapSearchTimingLogger: MapSearchTimingLogger = MapSearchTimingLogger.NoOp,
) {
    suspend operator fun invoke(
        keyword: String,
        types: Set<CollectionSpotType> = emptySet(),
        selectedRegionCandidate: MapRegionSearchCandidate? = null,
    ): List<CollectionSpot> {
        return searchWithResult(
            keyword = keyword,
            types = types,
            selectedRegionCandidate = selectedRegionCandidate,
        ).spots
    }

    suspend fun searchWithResult(
        keyword: String,
        types: Set<CollectionSpotType> = emptySet(),
        selectedRegionCandidate: MapRegionSearchCandidate? = null,
    ): CollectionSpotSearchResult {
        val normalizedKeyword = normalizeKeywordUseCase(keyword)
        val result = repository.searchRawByKeyword(
            keyword = normalizedKeyword,
            types = types,
        )
        val explicitRegionFilterStartedAtNanos = System.nanoTime()
        val explicitRegionFilteredSpots = result.spots
            .filterByExplicitRegionKeyword(normalizedKeyword)
        if (CollectionSpotAddressSearchPolicy.isEupMyeonDongCandidate(normalizedKeyword)) {
            mapSearchTimingLogger.log(
                "explicit region filter finished before=${result.spots.size} " +
                    "after=${explicitRegionFilteredSpots.size} " +
                    "elapsedMs=${explicitRegionFilterStartedAtNanos.elapsedMs()}",
            )
        }

        val selectedRegionFilterStartedAtNanos = System.nanoTime()
        val selectedRegionFilteredSpots = explicitRegionFilteredSpots
            .filterBySelectedRegionCandidate(selectedRegionCandidate)

        if (selectedRegionCandidate != null) {
            mapSearchTimingLogger.log(
                "selected region filter finished before=${explicitRegionFilteredSpots.size} " +
                    "after=${selectedRegionFilteredSpots.size} " +
                    "elapsedMs=${selectedRegionFilterStartedAtNanos.elapsedMs()}",
            )
        }
        val geocodedSpots = geocodingRepository.geocodeSpots(selectedRegionFilteredSpots)

        return CollectionSpotSearchResult(
            spots = geocodedSpots,
            isPartial = result.isPartial,
        )
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
        val addressSigungu = tokens.toSigunguName()
        val selectedSido = candidate.region.sido
        val selectedSigungu = candidate.region.sigungu

        val hasAnySido = tokens.any { token ->
            CollectionSpotAddressSearchPolicy.normalizedSidoName(
                token = token,
                sigungu = addressSigungu,
            ) != null
        }
        val hasSelectedSido = selectedSido == null ||
            tokens.any { token ->
                CollectionSpotAddressSearchPolicy.sidoMatches(
                    token = token,
                    sido = selectedSido,
                    requestedSigungu = selectedSigungu,
                    candidateSigungu = addressSigungu,
                )
            }
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

    private companion object {
        const val REGION_SCOPE_SEPARATOR = " "
        const val CITY_SUFFIX = "시"
        const val DISTRICT_SUFFIX = "구"
    }
}

private fun Long.elapsedMs(): Long =
    (System.nanoTime() - this) / NANOS_PER_MILLISECOND

private const val NANOS_PER_MILLISECOND = 1_000_000L
