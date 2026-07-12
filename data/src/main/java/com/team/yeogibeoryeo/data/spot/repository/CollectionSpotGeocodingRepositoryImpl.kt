package com.team.yeogibeoryeo.data.spot.repository

import com.team.yeogibeoryeo.data.spot.geocoder.SpotGeocoder
import com.team.yeogibeoryeo.domain.spot.log.MapSearchTimingLogger
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotGeocodingRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class CollectionSpotGeocodingRepositoryImpl @Inject constructor(
    private val spotGeocoder: SpotGeocoder,
    private val mapSearchTimingLogger: MapSearchTimingLogger = MapSearchTimingLogger.NoOp,
) : CollectionSpotGeocodingRepository {

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

    override suspend fun geocodeSpots(
        spots: List<CollectionSpot>,
    ): List<CollectionSpot> {
        if (spots.isEmpty()) return spots

        return supervisorScope {
            val addressKeys = spots.mapNotNull { spot -> spot.address.toGeocodeKey() }
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

            spots.map { spot ->
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
            .normalizeIntegratedGwangjuJeonnamAddress()
            .takeIf { it.isNotBlank() }
    }

    private fun String.normalizeIntegratedGwangjuJeonnamAddress(): String {
        if (!startsWith(GWANGJU_JEONNAM_INTEGRATED_SIDO)) return this

        val sigungu = split(WHITESPACE_REGEX)
            .getOrNull(INTEGRATED_SIDO_SIGUNGU_INDEX)
            .orEmpty()
        val normalizedSido = if (sigungu in GWANGJU_SIGUNGU_NAMES) {
            GWANGJU_SIDO
        } else {
            JEONNAM_SIDO
        }

        return replaceFirst(GWANGJU_JEONNAM_INTEGRATED_SIDO, normalizedSido)
    }

    private companion object {
        const val GEOCODING_CONCURRENCY_LIMIT = 4
        const val GWANGJU_JEONNAM_INTEGRATED_SIDO = "전남광주통합특별시"
        const val GWANGJU_SIDO = "광주광역시"
        const val JEONNAM_SIDO = "전라남도"
        const val INTEGRATED_SIDO_SIGUNGU_INDEX = 1
        val PARENTHESIZED_TEXT_REGEX = "\\([^)]*\\)".toRegex()
        val WHITESPACE_REGEX = "\\s+".toRegex()
        val GWANGJU_SIGUNGU_NAMES = setOf(
            "동구",
            "서구",
            "남구",
            "북구",
            "광산구",
        )
    }
}

private fun Long.elapsedMs(): Long =
    (System.nanoTime() - this) / NANOS_PER_MILLISECOND

private const val NANOS_PER_MILLISECOND = 1_000_000L
