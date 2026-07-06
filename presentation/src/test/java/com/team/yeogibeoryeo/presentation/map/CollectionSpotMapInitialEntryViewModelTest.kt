package com.team.yeogibeoryeo.presentation.map

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.presentation.map.location.CurrentLocationResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionSpotMapInitialEntryViewModelTest : CollectionSpotMapViewModelTestFixture() {
    @Test
    fun `지도 진입 시 위치 권한이 있으면 현재 위치 검색을 자동 실행한다`() =
        runTest {
            val currentCoordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881)
            val expectedSpots = listOf(sampleSpot("location", CollectionSpotType.STANDARD_BAG_STORE))
            val repository = FakeCollectionSpotRepository(locationSpots = expectedSpots)
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(currentCoordinate),
                hasFineLocationPermission = true,
            )

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted()

            assertEquals(1, repository.locationSearchCallCount)
            assertEquals(currentCoordinate, repository.lastLocationCoordinate)
            assertEquals(expectedSpots.withDistanceFrom(currentCoordinate), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
        }

    @Test
    fun `지도 진입 시 위치 권한이 없으면 자동 현재 위치 검색을 실행하지 않고 권한 안내를 표시한다`() =
        runTest {
            val repository = FakeCollectionSpotRepository()
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
                hasFineLocationPermission = false,
            )

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted()

            assertEquals(0, repository.locationSearchCallCount)
            assertFalse(viewModel.uiState.value.hasSearched)
            assertNull(viewModel.uiState.value.errorMessageResId)
            assertEquals(MapLocationNotices.PermissionDenied, viewModel.uiState.value.locationNotice)
        }

    @Test
    fun `지도 진입 자동 현재 위치 검색은 여러 번 호출되어도 한 번만 실행된다`() =
        runTest {
            val repository = FakeCollectionSpotRepository(
                locationSpots = listOf(sampleSpot("location", CollectionSpotType.STANDARD_BAG_STORE)),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
                hasFineLocationPermission = true,
            )

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted()
            viewModel.searchByCurrentLocationOnMapEntryIfPermitted()

            assertEquals(1, repository.locationSearchCallCount)
        }

    @Test
    fun `지도 진입 시 이미 검색 결과가 있으면 자동 현재 위치 검색을 실행하지 않는다`() =
        runTest {
            val keywordSpot = sampleSpot("keyword", CollectionSpotType.OTHER)
            val repository = FakeCollectionSpotRepository(
                keywordSpots = listOf(keywordSpot),
                locationSpots = listOf(sampleSpot("location", CollectionSpotType.STANDARD_BAG_STORE)),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
                hasFineLocationPermission = true,
            )

            viewModel.onSearchKeywordChanged("문래동")
            viewModel.searchByKeyword()
            advanceUntilIdle()
            viewModel.searchByCurrentLocationOnMapEntryIfPermitted()

            assertEquals(0, repository.locationSearchCallCount)
            assertEquals(listOf(keywordSpot), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.KEYWORD, viewModel.uiState.value.searchMode)
        }

    @Test
    fun `지도 진입 시 다른 검색이 로딩 중이면 자동 현재 위치 검색을 실행하지 않는다`() =
        runTest {
            val locationResult = CompletableDeferred<CurrentLocationResult>()
            val locationSpot = sampleSpot("location", CollectionSpotType.STANDARD_BAG_STORE)
            val repository = FakeCollectionSpotRepository(locationSpots = listOf(locationSpot))
            val viewModel = createViewModel(
                repository = repository,
                currentLocationProvider = FakeCurrentLocationProvider {
                    locationResult.await()
                },
                hasFineLocationPermission = true,
            )

            viewModel.searchByCurrentLocation()
            viewModel.searchByCurrentLocationOnMapEntryIfPermitted()
            locationResult.complete(
                CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
            )
            advanceUntilIdle()

            assertEquals(1, repository.locationSearchCallCount)
            assertEquals(
                listOf(locationSpot).withDistanceFrom(Coordinate(latitude = 37.5666102, longitude = 126.9783881)),
                viewModel.uiState.value.spots,
            )
        }

    @Test
    fun `지도 진입 시 사용자가 검색어를 입력한 상태면 자동 현재 위치 검색을 실행하지 않는다`() =
        runTest {
            val repository = FakeCollectionSpotRepository()
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
                hasFineLocationPermission = true,
            )

            viewModel.onSearchKeywordChanged("문래동")
            viewModel.searchByCurrentLocationOnMapEntryIfPermitted()

            assertEquals(0, repository.locationSearchCallCount)
            assertFalse(viewModel.uiState.value.hasSearched)
            assertEquals("문래동", viewModel.uiState.value.searchKeyword)
        }

    @Test
    fun `지도 진입 자동 현재 위치 검색 중 검색어를 입력하면 현재 위치 결과를 반영하지 않는다`() =
        runTest {
            val locationResult = CompletableDeferred<CurrentLocationResult>()
            val locationSpot = sampleSpot("location", CollectionSpotType.STANDARD_BAG_STORE)
            val repository = FakeCollectionSpotRepository(locationSpots = listOf(locationSpot))
            val viewModel = createViewModel(
                repository = repository,
                currentLocationProvider = FakeCurrentLocationProvider {
                    locationResult.await()
                },
                hasFineLocationPermission = true,
            )

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted()
            viewModel.onSearchKeywordChanged("문래동")
            locationResult.complete(
                CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
            )
            advanceUntilIdle()

            assertEquals(0, repository.locationSearchCallCount)
            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertFalse(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.hasSearched)
            assertEquals("문래동", viewModel.uiState.value.searchKeyword)
            assertEquals(MapSearchMode.KEYWORD, viewModel.uiState.value.searchMode)
        }

    @Test
    fun `초기 타입으로 지도 진입 시 해당 타입을 선택하고 현재 위치 검색 결과를 필터링한다`() =
        runTest {
            val currentCoordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881)
            val medicineSpot = sampleSpot("medicine", CollectionSpotType.MEDICINE_DROP_BOX)
            val batterySpot = sampleSpot("battery", CollectionSpotType.BATTERY_BIN)
            val repository = FakeCollectionSpotRepository(
                locationSpots = listOf(medicineSpot, batterySpot),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(currentCoordinate),
                hasFineLocationPermission = true,
            )

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted(CollectionSpotType.MEDICINE_DROP_BOX)

            assertEquals(setOf(CollectionSpotType.MEDICINE_DROP_BOX), viewModel.uiState.value.selectedTypes)
            assertEquals(listOf(medicineSpot).withDistanceFrom(currentCoordinate), viewModel.uiState.value.spots)
            assertEquals(1, repository.locationSearchCallCount)
            assertEquals(currentCoordinate, repository.lastLocationCoordinate)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
        }

    @Test
    fun `초기 타입으로 지도 진입 시 위치 권한이 없으면 타입을 선택하고 권한 안내를 표시한다`() =
        runTest {
            val repository = FakeCollectionSpotRepository()
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
                hasFineLocationPermission = false,
            )

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted(CollectionSpotType.FLUORESCENT_LAMP_BIN)

            assertEquals(setOf(CollectionSpotType.FLUORESCENT_LAMP_BIN), viewModel.uiState.value.selectedTypes)
            assertEquals(0, repository.locationSearchCallCount)
            assertFalse(viewModel.uiState.value.hasSearched)
            assertEquals(MapLocationNotices.PermissionDenied, viewModel.uiState.value.locationNotice)
            assertNull(viewModel.uiState.value.errorMessageResId)
        }

    @Test
    fun `초기 타입 지도 진입은 자동 검색은 한 번만 실행하고 타입 선택은 새 진입 값을 반영한다`() =
        runTest {
            val medicineSpot = sampleSpot("medicine", CollectionSpotType.MEDICINE_DROP_BOX)
            val repository = FakeCollectionSpotRepository(locationSpots = listOf(medicineSpot))
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
                hasFineLocationPermission = true,
            )

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted(CollectionSpotType.MEDICINE_DROP_BOX)
            viewModel.searchByCurrentLocationOnMapEntryIfPermitted(CollectionSpotType.BATTERY_BIN)

            assertEquals(setOf(CollectionSpotType.BATTERY_BIN), viewModel.uiState.value.selectedTypes)
            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertEquals(1, repository.locationSearchCallCount)
        }

    @Test
    fun `초기 타입으로 다시 지도 진입하면 기존 검색 상태가 있어도 타입 선택을 반영한다`() =
        runTest {
            val medicineSpot = sampleSpot("medicine", CollectionSpotType.MEDICINE_DROP_BOX)
            val batterySpot = sampleSpot("battery", CollectionSpotType.BATTERY_BIN)
            val repository = FakeCollectionSpotRepository(
                locationSpots = listOf(medicineSpot, batterySpot),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
                hasFineLocationPermission = true,
            )

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted()
            viewModel.searchByCurrentLocationOnMapEntryIfPermitted(CollectionSpotType.BATTERY_BIN)

            assertEquals(setOf(CollectionSpotType.BATTERY_BIN), viewModel.uiState.value.selectedTypes)
            assertEquals(
                listOf(batterySpot).withDistanceFrom(Coordinate(latitude = 37.5666102, longitude = 126.9783881)),
                viewModel.uiState.value.spots,
            )
            assertEquals(1, repository.locationSearchCallCount)
        }

    @Test
    fun `검색어가 입력된 상태에서 초기 타입으로 지도 진입하면 자동 검색 없이 타입 선택만 반영한다`() =
        runTest {
            val repository = FakeCollectionSpotRepository()
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
                hasFineLocationPermission = true,
            )

            viewModel.onSearchKeywordChanged("문래동")
            viewModel.searchByCurrentLocationOnMapEntryIfPermitted(CollectionSpotType.CLOTHING_BIN)

            assertEquals(setOf(CollectionSpotType.CLOTHING_BIN), viewModel.uiState.value.selectedTypes)
            assertEquals("문래동", viewModel.uiState.value.searchKeyword)
            assertEquals(0, repository.locationSearchCallCount)
            assertFalse(viewModel.uiState.value.hasSearched)
        }

}
