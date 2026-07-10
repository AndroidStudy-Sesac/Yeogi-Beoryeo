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
class CollectionSpotMapCurrentLocationViewModelTest : CollectionSpotMapViewModelTestFixture() {
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
            assertEquals(MapLocationNotices.PermissionDenied.titleResId, viewModel.uiState.value.locationNotice?.titleResId)
            assertEquals(
                MapLocationNotices.PermissionDenied.messageResId,
                viewModel.uiState.value.locationNotice?.messageResId,
            )
            assertEquals(MapLocationNoticeAction.OpenAppSettings, viewModel.uiState.value.locationNotice?.action)
        }

    @Test
    fun `위치 권한이 없으면 수동 현재 위치 검색 시 캐시를 표시하지 않고 삭제한다`() =
        runTest {
            val cachedSpot = sampleSpot("cache", CollectionSpotType.STANDARD_BAG_STORE)
            val cache = FakeRecentCurrentLocationSpotCacheRepository(
                entry = freshCacheEntry(listOf(cachedSpot)),
            )
            val repository = FakeCollectionSpotRepository()
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
                hasFineLocationPermission = false,
                recentCurrentLocationSpotCacheRepository = cache,
            )

            viewModel.searchByCurrentLocation()
            advanceUntilIdle()

            assertEquals(0, cache.getCallCount)
            assertEquals(1, cache.clearCallCount)
            assertNull(cache.entry)
            assertEquals(0, repository.locationSearchCallCount)
            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.KEYWORD, viewModel.uiState.value.searchMode)
            assertEquals(MapLocationNotices.PermissionDenied.titleResId, viewModel.uiState.value.locationNotice?.titleResId)
        }

    @Test
    fun `캐시 표시 후 권한 거부 결과를 받으면 캐시와 현재 위치 결과를 정리한다`() =
        runTest {
            val cachedSpot = sampleSpot("cache", CollectionSpotType.STANDARD_BAG_STORE)
            val cache = FakeRecentCurrentLocationSpotCacheRepository(
                entry = freshCacheEntry(listOf(cachedSpot)),
            )
            val repository = FakeCollectionSpotRepository()
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.PermissionDenied,
                hasFineLocationPermission = true,
                recentCurrentLocationSpotCacheRepository = cache,
            )

            viewModel.searchByCurrentLocation()
            advanceUntilIdle()

            assertEquals(1, cache.getCallCount)
            assertEquals(1, cache.clearCallCount)
            assertNull(cache.entry)
            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.KEYWORD, viewModel.uiState.value.searchMode)
            assertEquals(MapLocationNotices.PermissionDenied.titleResId, viewModel.uiState.value.locationNotice?.titleResId)
        }

    @Test
    fun `권한 철회 시 현재 위치 캐시와 현재 위치 결과를 정리한다`() =
        runTest {
            val currentCoordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881)
            val locationSpot = sampleSpot("location", CollectionSpotType.STANDARD_BAG_STORE)
            val cache = FakeRecentCurrentLocationSpotCacheRepository()
            val repository = FakeCollectionSpotRepository(locationSpots = listOf(locationSpot))
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(currentCoordinate),
                recentCurrentLocationSpotCacheRepository = cache,
            )
            viewModel.searchByCurrentLocation()
            advanceUntilIdle()

            viewModel.onLocationPermissionRevoked()
            advanceUntilIdle()

            assertEquals(1, cache.clearCallCount)
            assertNull(cache.entry)
            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.KEYWORD, viewModel.uiState.value.searchMode)
            assertEquals(MapLocationNotices.PermissionDenied.titleResId, viewModel.uiState.value.locationNotice?.titleResId)
        }

    @Test
    fun `권한 철회 시 키워드 검색 결과는 유지하고 현재 위치 캐시만 삭제한다`() =
        runTest {
            val keywordSpot = sampleSpot("keyword", CollectionSpotType.OTHER)
            val cachedSpot = sampleSpot("cache", CollectionSpotType.STANDARD_BAG_STORE)
            val cache = FakeRecentCurrentLocationSpotCacheRepository(
                entry = freshCacheEntry(listOf(cachedSpot)),
            )
            val repository = FakeCollectionSpotRepository(keywordSpots = listOf(keywordSpot))
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
                recentCurrentLocationSpotCacheRepository = cache,
            )
            viewModel.onSearchKeywordChanged("문래동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            viewModel.onLocationPermissionRevoked()
            advanceUntilIdle()

            assertEquals(1, cache.clearCallCount)
            assertNull(cache.entry)
            assertEquals(listOf(keywordSpot), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.KEYWORD, viewModel.uiState.value.searchMode)
            assertNull(viewModel.uiState.value.locationNotice)
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
                MapLocationNotices.CurrentLocationUnavailable.titleResId,
                viewModel.uiState.value.locationNotice?.titleResId,
            )
            assertEquals(
                MapLocationNotices.CurrentLocationUnavailable.messageResId,
                viewModel.uiState.value.locationNotice?.messageResId,
            )
            assertNull(viewModel.uiState.value.locationNotice?.action)
        }

    @Test
    fun `위치 서비스가 꺼져 있으면 위치 설정 안내를 표시한다`() =
        runTest {
            val repository = FakeCollectionSpotRepository()
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.LocationServiceDisabled,
            )

            viewModel.searchByCurrentLocation()

            assertEquals(0, repository.locationSearchCallCount)
            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(MapSearchMode.KEYWORD, viewModel.uiState.value.searchMode)
            assertEquals(
                MapLocationNotices.LocationServiceDisabled.titleResId,
                viewModel.uiState.value.locationNotice?.titleResId,
            )
            assertEquals(
                MapLocationNotices.LocationServiceDisabled.messageResId,
                viewModel.uiState.value.locationNotice?.messageResId,
            )
            assertEquals(MapLocationNoticeAction.OpenLocationSettings, viewModel.uiState.value.locationNotice?.action)
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
            assertEquals(expectedSpots.withDistanceFrom(currentCoordinate), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
            assertNull(viewModel.uiState.value.errorMessageResId)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `현재 위치 주변 수거 장소 검색 실패 시 errorMessage를 표시하고 notice는 설정하지 않는다`() =
        runTest {
            val repository = FakeCollectionSpotRepository(
                locationSearchThrowable = IllegalStateException(
                    "Unable to resolve host \"apis.data.go.kr\": No address associated with hostname",
                ),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
            )

            viewModel.searchByCurrentLocation()

            assertEquals(1, repository.locationSearchCallCount)
            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(
                MapLocationNotices.CurrentLocationSpotSearchFailureMessageResId,
                viewModel.uiState.value.errorMessageResId,
            )
            assertNull(viewModel.uiState.value.locationNotice)
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
            assertNull(viewModel.uiState.value.locationNotice)
            assertNull(viewModel.uiState.value.errorMessageResId)
        }

    @Test
    fun `현재 위치 검색 실패 안내 후 재시도하면 이전 notice를 새 실패 상태로 갱신한다`() =
        runTest {
            val firstResult = CompletableDeferred<CurrentLocationResult>()
            val secondResult = CompletableDeferred<CurrentLocationResult>()
            var requestCount = 0
            val repository = FakeCollectionSpotRepository()
            val viewModel = createViewModel(
                repository = repository,
                currentLocationProvider = FakeCurrentLocationProvider {
                    requestCount += 1
                    if (requestCount == 1) {
                        firstResult.await()
                    } else {
                        secondResult.await()
                    }
                },
            )

            viewModel.searchByCurrentLocation()
            firstResult.complete(CurrentLocationResult.NotFound)
            advanceUntilIdle()
            viewModel.searchByCurrentLocation()
            secondResult.complete(CurrentLocationResult.LocationServiceDisabled)
            advanceUntilIdle()

            assertEquals(
                MapLocationNotices.LocationServiceDisabled.titleResId,
                viewModel.uiState.value.locationNotice?.titleResId,
            )
            assertEquals(MapLocationNoticeAction.OpenLocationSettings, viewModel.uiState.value.locationNotice?.action)
            assertEquals(
                MapLocationNotices.LocationServiceDisabled.messageResId,
                viewModel.uiState.value.locationNotice?.messageResId,
            )
            assertNull(viewModel.uiState.value.errorMessageResId)
        }

    @Test
    fun `현재 위치 검색 실패 안내 후 재시도에 성공하면 이전 notice를 정리하고 결과를 표시한다`() =
        runTest {
            val currentCoordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881)
            val locationSpot = sampleSpot("location", CollectionSpotType.STANDARD_BAG_STORE)
            var currentLocationResult: CurrentLocationResult = CurrentLocationResult.LocationServiceDisabled
            val repository = FakeCollectionSpotRepository(
                locationSpots = listOf(locationSpot),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationProvider = FakeCurrentLocationProvider {
                    currentLocationResult
                },
            )

            viewModel.searchByCurrentLocation()
            advanceUntilIdle()

            assertEquals(
                MapLocationNotices.LocationServiceDisabled.titleResId,
                viewModel.uiState.value.locationNotice?.titleResId,
            )

            currentLocationResult = CurrentLocationResult.Found(currentCoordinate)
            viewModel.searchByCurrentLocation()
            advanceUntilIdle()

            assertEquals(listOf(locationSpot).withDistanceFrom(currentCoordinate), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
            assertNull(viewModel.uiState.value.locationNotice)
            assertNull(viewModel.uiState.value.errorMessageResId)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `키워드 검색 후 현재 위치 검색을 실행하면 검색어를 비우고 현재 위치 결과를 반영한다`() =
        runTest {
            val keywordSpot = sampleSpot("keyword", CollectionSpotType.OTHER)
            val locationSpot = sampleSpot("location", CollectionSpotType.STANDARD_BAG_STORE)
            val repository = FakeCollectionSpotRepository(
                keywordSpots = listOf(keywordSpot),
                locationSpots = listOf(locationSpot),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
            )

            viewModel.onSearchKeywordChanged("용답동")
            viewModel.searchByKeyword()
            advanceUntilIdle()
            viewModel.searchByCurrentLocation()
            advanceUntilIdle()

            assertEquals("", viewModel.uiState.value.searchKeyword)
            assertEquals(
                listOf(locationSpot).withDistanceFrom(Coordinate(latitude = 37.5666102, longitude = 126.9783881)),
                viewModel.uiState.value.spots,
            )
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
            assertFalse(viewModel.uiState.value.isLoading)
        }

}
