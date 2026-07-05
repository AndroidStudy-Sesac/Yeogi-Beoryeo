package com.team.yeogibeoryeo.data.spot.repository

import com.team.yeogibeoryeo.data.core.key.AppKeyProvider
import com.team.yeogibeoryeo.data.spot.geocoder.SpotGeocoder
import com.team.yeogibeoryeo.data.spot.mapper.SpotMapper
import com.team.yeogibeoryeo.data.spot.remote.datasource.SpotRemoteDataSource
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.supervisorScope

class CollectionSpotRepositoryImpl @Inject constructor(
    private val remoteDataSource: SpotRemoteDataSource,
    private val spotMapper: SpotMapper,
    private val spotGeocoder: SpotGeocoder,
    private val publicDataKeyProvider: AppKeyProvider,
) : CollectionSpotRepository {

    override suspend fun searchByKeyword(
        keyword: String,
        types: Set<CollectionSpotType>,
    ): List<CollectionSpot> {
        val spots = remoteDataSource.searchByKeyword(
            serviceKey = publicDataKeyProvider.publicDataServiceKey,
            keyword = keyword,
        ).let { dtoList ->
            spotMapper.mapToDomainList(dtoList)
        }

        return spots
            .filterByTypes(types)
            .geocodeAll()
    }

    override suspend fun searchByLocation(
        coordinate: Coordinate,
        radiusMeter: Int,
        types: Set<CollectionSpotType>,
    ): List<CollectionSpot> {
        val spots = remoteDataSource.searchByLocation(
            serviceKey = publicDataKeyProvider.publicDataServiceKey,
            latitude = coordinate.latitude,
            longitude = coordinate.longitude,
            radiusMeter = radiusMeter,
        ).let { dtoList ->
            spotMapper.mapToDomainList(dtoList)
        }

        return spots
            .filterByTypes(types)
            .geocodeAll()
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
            val geocodeJobsByAddress = LinkedHashMap<String, Deferred<Coordinate?>>()
            val geocodeSemaphore = Semaphore(MAX_CONCURRENT_GEOCODING_COUNT)

            forEach { spot ->
                val addressKey = spot.address.toGeocodeKey() ?: return@forEach

                geocodeJobsByAddress.getOrPut(addressKey) {
                    async {
                        geocodeSafely(
                            address = addressKey,
                            semaphore = geocodeSemaphore,
                        )
                    }
                }
            }

            geocodeJobsByAddress.values.awaitAll()

            map { spot ->
                val addressKey = spot.address.toGeocodeKey()
                    ?: return@map spot
                val coordinate = geocodeJobsByAddress[addressKey]?.await()

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
        const val MAX_CONCURRENT_GEOCODING_COUNT = 3
        val PARENTHESIZED_TEXT_REGEX = "\\([^)]*\\)".toRegex()
        val WHITESPACE_REGEX = "\\s+".toRegex()
    }
}
