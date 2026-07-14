package com.team.yeogibeoryeo.domain.spot.usecase

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotSearchResult
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotGeocodingRepository
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchCollectionSpotsByLocationUseCaseTest {

    private val repository = FakeCollectionSpotRepository()
    private val geocodingRepository = FakeCollectionSpotGeocodingRepository()
    private val useCase = SearchCollectionSpotsByLocationUseCase(
        repository = repository,
        geocodingRepository = geocodingRepository,
    )

    @Test
    fun `현재 위치 검색은 raw location search 후 geocoding한다`() = runBlocking {
        val coordinate = Coordinate(latitude = 37.5182396969791, longitude = 126.895880210522)
        val rawSpot = collectionSpot(id = "spot-1", coordinate = null)
        repository.locationSpots = listOf(rawSpot)

        val result = useCase(
            coordinate = coordinate,
            radiusMeter = 500,
        )

        assertEquals(coordinate, repository.lastCoordinate)
        assertEquals(500, repository.lastRadiusMeter)
        assertEquals(listOf("spot-1"), geocodingRepository.geocodedSpotIds)
        assertEquals(DEFAULT_COORDINATE, result.first().coordinate)
    }

    private class FakeCollectionSpotRepository : CollectionSpotRepository {
        var locationSpots: List<CollectionSpot> = emptyList()
        var lastCoordinate: Coordinate? = null
        var lastRadiusMeter: Int? = null

        override suspend fun searchRawByKeyword(
            keyword: String,
            types: Set<CollectionSpotType>,
        ): CollectionSpotSearchResult {
            return CollectionSpotSearchResult(spots = emptyList())
        }

        override suspend fun searchRawByLocation(
            coordinate: Coordinate,
            radiusMeter: Int,
            types: Set<CollectionSpotType>,
        ): List<CollectionSpot> {
            lastCoordinate = coordinate
            lastRadiusMeter = radiusMeter
            return locationSpots
        }
    }

    private class FakeCollectionSpotGeocodingRepository : CollectionSpotGeocodingRepository {
        val geocodedSpotIds = mutableListOf<String>()

        override suspend fun geocodeSpot(
            spot: CollectionSpot,
        ): CollectionSpot {
            geocodedSpotIds += spot.id
            return spot.copy(
                coordinate = spot.coordinate ?: DEFAULT_COORDINATE,
            )
        }
    }

    private fun collectionSpot(
        id: String,
        coordinate: Coordinate?,
    ): CollectionSpot {
        return CollectionSpot(
            id = id,
            name = "수거 장소 $id",
            type = CollectionSpotType.STANDARD_BAG_STORE,
            address = "서울특별시 영등포구 문래동",
            detailLocation = null,
            coordinate = coordinate,
        )
    }

    private companion object {
        val DEFAULT_COORDINATE = Coordinate(latitude = 37.5666102, longitude = 126.9783881)
    }
}
