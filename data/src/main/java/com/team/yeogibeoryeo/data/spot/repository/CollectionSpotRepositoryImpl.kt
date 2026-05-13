package com.team.yeogibeoryeo.data.spot.repository

import com.team.yeogibeoryeo.data.BuildConfig
import com.team.yeogibeoryeo.data.spot.geocoder.SpotGeocoder
import com.team.yeogibeoryeo.data.spot.mapper.SpotMapper
import com.team.yeogibeoryeo.data.spot.remote.datasource.SpotRemoteDataSource
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotRepository
import javax.inject.Inject

class CollectionSpotRepositoryImpl @Inject constructor(
    private val remoteDataSource: SpotRemoteDataSource,
    private val spotMapper: SpotMapper,
    private val spotGeocoder: SpotGeocoder,
) : CollectionSpotRepository {

    override suspend fun searchByKeyword(
        keyword: String,
        types: Set<CollectionSpotType>,
    ): List<CollectionSpot> {
        val spots = remoteDataSource.searchByKeyword(
            serviceKey = BuildConfig.PUBLIC_DATA_SERVICE_KEY,
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
            serviceKey = BuildConfig.PUBLIC_DATA_SERVICE_KEY,
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
        val coordinate = spotGeocoder.geocode(spot.address)

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
        return map { spot ->
            geocodeSpot(spot)
        }
    }
}