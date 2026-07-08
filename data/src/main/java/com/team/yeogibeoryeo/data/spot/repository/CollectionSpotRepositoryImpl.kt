package com.team.yeogibeoryeo.data.spot.repository

import com.team.yeogibeoryeo.data.core.key.AppKeyProvider
import com.team.yeogibeoryeo.data.spot.mapper.SpotMapper
import com.team.yeogibeoryeo.data.spot.remote.datasource.SpotRemoteDataSource
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotSearchResult
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.log.MapSearchTimingLogger
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotRepository
import javax.inject.Inject

class CollectionSpotRepositoryImpl @Inject constructor(
    private val remoteDataSource: SpotRemoteDataSource,
    private val spotMapper: SpotMapper,
    private val publicDataKeyProvider: AppKeyProvider,
    private val mapSearchTimingLogger: MapSearchTimingLogger = MapSearchTimingLogger.NoOp,
) : CollectionSpotRepository {

    override suspend fun searchRawByKeyword(
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

        mapSearchTimingLogger.log(
            "repository raw search finished rawCount=${typeFilteredSpots.size} " +
                "elapsedMs=${repositorySearchStartedAtNanos.elapsedMs()}",
        )

        return CollectionSpotSearchResult(
            spots = typeFilteredSpots,
            isPartial = result.isPartial,
        )
    }

    override suspend fun searchRawByLocation(
        coordinate: Coordinate,
        radiusMeter: Int,
        types: Set<CollectionSpotType>,
    ): List<CollectionSpot> {
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
    }

    private fun List<CollectionSpot>.filterByTypes(
        types: Set<CollectionSpotType>,
    ): List<CollectionSpot> {
        if (types.isEmpty()) return this

        return filter { spot ->
            spot.type in types
        }
    }
}

private fun Long.elapsedMs(): Long =
    (System.nanoTime() - this) / NANOS_PER_MILLISECOND

private const val NANOS_PER_MILLISECOND = 1_000_000L
