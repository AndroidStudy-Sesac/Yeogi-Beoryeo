package com.team.yeogibeoryeo.data.spot.repository

import com.team.yeogibeoryeo.data.core.key.AppKeyProvider
import com.team.yeogibeoryeo.data.spot.geocoder.SpotGeocoder
import com.team.yeogibeoryeo.data.spot.mapper.SpotMapper
import com.team.yeogibeoryeo.data.spot.remote.datasource.SpotRemoteDataSource
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotSearchResult
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.log.MapSearchTimingLogger
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.supervisorScope

class CollectionSpotRepositoryImpl @Inject constructor(
    private val remoteDataSource: SpotRemoteDataSource,
    private val spotMapper: SpotMapper,
    private val spotGeocoder: SpotGeocoder,
    private val publicDataKeyProvider: AppKeyProvider,
    private val mapSearchTimingLogger: MapSearchTimingLogger = MapSearchTimingLogger.NoOp,
) : CollectionSpotRepository {

    override suspend fun searchByKeyword(
        keyword: String,
        types: Set<CollectionSpotType>,
    ): List<CollectionSpot> {
        return searchByKeywordResult(
            keyword = keyword,
            types = types,
        ).spots
    }

    override suspend fun searchByKeywordResult(
        keyword: String,
        types: Set<CollectionSpotType>,
    ): CollectionSpotSearchResult {
        val repositorySearchStartedAtNanos = System.nanoTime()
        val result = remoteDataSource.searchByKeywordResult(
            serviceKey = publicDataKeyProvider.publicDataServiceKey,
            keyword = keyword,
        )

        val mappedSpots = spotMapper.mapToDomainList(result.items)
        val typeFilterStartedAtNanos = System.nanoTime()
        val typeFilteredSpots = mappedSpots.filterByTypes(types)
        mapSearchTimingLogger.log(
            "type filter finished before=${mappedSpots.size} after=${typeFilteredSpots.size} " +
                "elapsedMs=${typeFilterStartedAtNanos.elapsedMs()}",
        )

        val spots = typeFilteredSpots
            .geocodeAll()

        mapSearchTimingLogger.log(
            "repository search finished finalCount=${spots.size} " +
                "elapsedMs=${repositorySearchStartedAtNanos.elapsedMs()}",
        )

        return CollectionSpotSearchResult(
            spots = spots,
            isPartial = result.isPartial,
        )
    }

    override suspend fun searchByLocation(
        coordinate: Coordinate,
        radiusMeter: Int,
        types: Set<CollectionSpotType>,
    ): List<CollectionSpot> {
        val repositorySearchStartedAtNanos = System.nanoTime()
        val remoteSearchStartedAtNanos = System.nanoTime()
        val spots = remoteDataSource.searchByLocation(
            serviceKey = publicDataKeyProvider.publicDataServiceKey,
            latitude = coordinate.latitude,
            longitude = coordinate.longitude,
            radiusMeter = radiusMeter,
        ).let { dtoList ->
            mapSearchTimingLogger.log(
                "getSpot location finished count=${dtoList.size} " +
                    "elapsedMs=${remoteSearchStartedAtNanos.elapsedMs()}",
            )
            spotMapper.mapToDomainList(dtoList)
        }

        val typeFilterStartedAtNanos = System.nanoTime()
        val typeFilteredSpots = spots.filterByTypes(types)
        mapSearchTimingLogger.log(
            "type filter finished before=${spots.size} after=${typeFilteredSpots.size} " +
                "elapsedMs=${typeFilterStartedAtNanos.elapsedMs()}",
        )

        return typeFilteredSpots
            .geocodeAll()
            .also { geocodedSpots ->
                mapSearchTimingLogger.log(
                    "repository search finished finalCount=${geocodedSpots.size} " +
                        "elapsedMs=${repositorySearchStartedAtNanos.elapsedMs()}",
                )
            }
    }

    override suspend fun geocodeSpot(
        spot: CollectionSpot,
    ): CollectionSpot {
        val coordinate = spot.address.toGeocodeKey()?.let { address ->
            spotGeocoder.geocode(address)
        }

        return spot.copy(
            coordinate = coordinate,
        )
    }

    private fun List<CollectionSpot>.filterByTypes(
        types: Set<CollectionSpotType>,
    ): List<CollectionSpot> {
        if (types.isEmpty()) return this

        return filter { spot ->
            spot.type in types
        }
    }

    private suspend fun List<CollectionSpot>.geocodeAll(): List<CollectionSpot> {
        if (isEmpty()) return this

        return supervisorScope {
            val addressKeys = mapNotNull { spot -> spot.address.toGeocodeKey() }
                .distinct()
            val geocodeJobsByAddress = LinkedHashMap<String, Deferred<Coordinate?>>()
            val geocodeSemaphore = Semaphore(GEOCODING_CONCURRENCY_LIMIT)
            val geocodingStartedAtNanos = System.nanoTime()

            mapSearchTimingLogger.log(
                "geocoding started targetCount=${addressKeys.size} " +
                    "concurrency=$GEOCODING_CONCURRENCY_LIMIT",
            )

            addressKeys.forEach { addressKey ->
                geocodeJobsByAddress.getOrPut(addressKey) {
                    async {
                        geocodeSafely(
                            address = addressKey,
                            semaphore = geocodeSemaphore,
                        )
                    }
                }
            }

            val coordinatesByAddress = geocodeJobsByAddress
                .mapValues { (_, geocodeJob) -> geocodeJob.await() }
            val geocodeSuccessCount = coordinatesByAddress.values.count { coordinate -> coordinate != null }

            mapSearchTimingLogger.log(
                "geocoding finished success=$geocodeSuccessCount " +
                    "null=${coordinatesByAddress.size - geocodeSuccessCount} " +
                    "elapsedMs=${geocodingStartedAtNanos.elapsedMs()}",
            )

            map { spot ->
                val addressKey = spot.address.toGeocodeKey()
                    ?: return@map spot
                val coordinate = coordinatesByAddress[addressKey]

                spot.copy(
                    coordinate = coordinate,
                )
            }
        }
    }

    private suspend fun geocodeSafely(
        address: String,
        semaphore: Semaphore,
    ): Coordinate? {
        return semaphore.withPermit {
            runCatching {
                spotGeocoder.geocode(address)
            }.getOrElse { exception ->
                if (exception is CancellationException) throw exception

                null
            }
        }
    }

    private fun String.toGeocodeKey(): String? {
        return replace(PARENTHESIZED_TEXT_REGEX, " ")
            .trim()
            .replace(WHITESPACE_REGEX, " ")
            .takeIf { it.isNotBlank() }
    }

    private companion object {
        const val GEOCODING_CONCURRENCY_LIMIT = 3
        val PARENTHESIZED_TEXT_REGEX = "\\([^)]*\\)".toRegex()
        val WHITESPACE_REGEX = "\\s+".toRegex()
    }
}

private fun Long.elapsedMs(): Long =
    (System.nanoTime() - this) / NANOS_PER_MILLISECOND

private const val NANOS_PER_MILLISECOND = 1_000_000L
