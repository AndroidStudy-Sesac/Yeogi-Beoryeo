package com.team.yeogibeoryeo.presentation.map

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotRepository
import com.team.yeogibeoryeo.domain.spot.usecase.FilterCollectionSpotsUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.SearchCollectionSpotsByKeywordUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.SearchCollectionSpotsByLocationUseCase
import com.team.yeogibeoryeo.presentation.map.location.CurrentLocationProvider
import com.team.yeogibeoryeo.presentation.map.location.CurrentLocationResult
import com.team.yeogibeoryeo.presentation.search.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionSpotMapViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `현재 위치 권한이 거부되면 직접 검색 안내를 표시하고 위치 검색을 실행하지 않는다`() =
        runTest {
            val repository = FakeCollectionSpotRepository()
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.PermissionDenied,
            )

            viewModel.searchByCurrentLocation()

            assertEquals(0, repository.locationSearchCallCount)
            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(MapSearchMode.KEYWORD, viewModel.uiState.value.searchMode)
            assertEquals(
                "현재 위치 검색은 정확한 위치 권한을 허용하면 사용할 수 있어요. 직접 동네나 주소를 검색할 수도 있습니다.",
                viewModel.uiState.value.locationNoticeMessage,
            )
        }

    @Test
    fun `최근 위치 캐시가 없으면 현재 위치 조회 실패 안내를 표시한다`() =
        runTest {
            val repository = FakeCollectionSpotRepository()
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.searchByCurrentLocation()

            assertEquals(0, repository.locationSearchCallCount)
            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(MapSearchMode.KEYWORD, viewModel.uiState.value.searchMode)
            assertEquals(
                "현재 위치를 확인하지 못했습니다. 직접 동네나 주소를 검색해 주세요.",
                viewModel.uiState.value.locationNoticeMessage,
            )
        }

    @Test
    fun `현재 위치 조회가 성공하면 위치 기반 수거 장소를 검색한다`() =
        runTest {
            val currentCoordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881)
            val expectedSpots = listOf(sampleSpot("1", CollectionSpotType.STANDARD_BAG_STORE))
            val repository = FakeCollectionSpotRepository(
                locationSpots = expectedSpots,
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(currentCoordinate),
            )

            viewModel.searchByCurrentLocation()

            assertEquals(currentCoordinate, repository.lastLocationCoordinate)
            assertEquals(500, repository.lastRadiusMeter)
            assertEquals(expectedSpots, viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
            assertNull(viewModel.uiState.value.locationNoticeMessage)
            assertNull(viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `현재 위치 검색 결과도 필터칩 선택 시 정상 필터링된다`() =
        runTest {
            val standardBagSpot = sampleSpot("1", CollectionSpotType.STANDARD_BAG_STORE)
            val recyclingCenterSpot = sampleSpot("2", CollectionSpotType.RECYCLING_CENTER)
            val repository = FakeCollectionSpotRepository(
                locationSpots = listOf(standardBagSpot, recyclingCenterSpot),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
            )

            viewModel.searchByCurrentLocation()
            viewModel.onSpotTypeClick(CollectionSpotType.RECYCLING_CENTER)

            assertEquals(setOf(CollectionSpotType.RECYCLING_CENTER), viewModel.uiState.value.selectedTypes)
            assertEquals(listOf(recyclingCenterSpot), viewModel.uiState.value.spots)
        }

    @Test
    fun `선택된 장소는 현재 위치 검색 결과에서도 유지된다`() =
        runTest {
            val selectedSpot = sampleSpot("1", CollectionSpotType.STANDARD_BAG_STORE)
            val repository = FakeCollectionSpotRepository(
                locationSpots = listOf(selectedSpot),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
            )

            viewModel.searchByCurrentLocation()
            viewModel.onSpotClick(selectedSpot)

            assertEquals(selectedSpot, viewModel.uiState.value.selectedSpot)
        }

    @Test
    fun `현재 위치 검색 중 키워드 검색을 실행하면 최신 키워드 검색 결과를 유지한다`() =
        runTest {
            val locationResult = CompletableDeferred<CurrentLocationResult>()
            val keywordSpot = sampleSpot("keyword", CollectionSpotType.OTHER)
            val locationSpot = sampleSpot("location", CollectionSpotType.STANDARD_BAG_STORE)
            val repository = FakeCollectionSpotRepository(
                keywordSpots = listOf(keywordSpot),
                locationSpots = listOf(locationSpot),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationProvider = FakeCurrentLocationProvider {
                    locationResult.await()
                },
            )

            viewModel.searchByCurrentLocation()
            viewModel.onSearchKeywordChanged("문래동")
            viewModel.searchByKeyword()

            locationResult.complete(
                CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
            )
            advanceUntilIdle()

            assertEquals(listOf(keywordSpot), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.KEYWORD, viewModel.uiState.value.searchMode)
        }

    @Test
    fun `위치 권한 거부 후에도 키워드 검색은 정상 동작한다`() =
        runTest {
            val expectedSpot = sampleSpot("keyword", CollectionSpotType.OTHER)
            val repository = FakeCollectionSpotRepository(keywordSpots = listOf(expectedSpot))
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.PermissionDenied,
            )

            viewModel.searchByCurrentLocation()
            viewModel.onSearchKeywordChanged("문래동")
            viewModel.searchByKeyword()

            assertEquals(listOf("문래동"), repository.keywords)
            assertEquals(listOf(expectedSpot), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.KEYWORD, viewModel.uiState.value.searchMode)
            assertNull(viewModel.uiState.value.locationNoticeMessage)
        }

    private fun createViewModel(
        repository: FakeCollectionSpotRepository,
        currentLocationResult: CurrentLocationResult,
    ): CollectionSpotMapViewModel {
        return createViewModel(
            repository = repository,
            currentLocationProvider = FakeCurrentLocationProvider(currentLocationResult),
        )
    }

    private fun createViewModel(
        repository: FakeCollectionSpotRepository,
        currentLocationProvider: CurrentLocationProvider,
    ): CollectionSpotMapViewModel {
        return CollectionSpotMapViewModel(
            searchCollectionSpotsByKeywordUseCase = SearchCollectionSpotsByKeywordUseCase(repository),
            searchCollectionSpotsByLocationUseCase = SearchCollectionSpotsByLocationUseCase(repository),
            filterCollectionSpotsUseCase = FilterCollectionSpotsUseCase(),
            currentLocationProvider = currentLocationProvider,
        )
    }

    private fun sampleSpot(
        id: String,
        type: CollectionSpotType,
    ): CollectionSpot {
        return CollectionSpot(
            id = id,
            name = "수거 장소 $id",
            type = type,
            address = "서울시 중구",
            detailLocation = null,
            coordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881),
        )
    }

    private class FakeCurrentLocationProvider(
        private val resultProvider: suspend () -> CurrentLocationResult,
    ) : CurrentLocationProvider {

        constructor(result: CurrentLocationResult) : this({ result })

        override suspend fun getCurrentLocation(): CurrentLocationResult = resultProvider()
    }

    private class FakeCollectionSpotRepository(
        private val keywordSpots: List<CollectionSpot> = emptyList(),
        private val locationSpots: List<CollectionSpot> = emptyList(),
    ) : CollectionSpotRepository {
        val keywords = mutableListOf<String>()
        var locationSearchCallCount = 0
        var lastLocationCoordinate: Coordinate? = null
        var lastRadiusMeter: Int? = null

        override suspend fun searchByKeyword(
            keyword: String,
            types: Set<CollectionSpotType>,
        ): List<CollectionSpot> {
            keywords += keyword
            return keywordSpots
        }

        override suspend fun searchByLocation(
            coordinate: Coordinate,
            radiusMeter: Int,
            types: Set<CollectionSpotType>,
        ): List<CollectionSpot> {
            locationSearchCallCount += 1
            lastLocationCoordinate = coordinate
            lastRadiusMeter = radiusMeter
            return locationSpots
        }

        override suspend fun geocodeSpot(spot: CollectionSpot): CollectionSpot = spot
    }
}
