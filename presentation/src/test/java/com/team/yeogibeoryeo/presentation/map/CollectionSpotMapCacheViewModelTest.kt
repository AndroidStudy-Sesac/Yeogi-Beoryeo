package com.team.yeogibeoryeo.presentation.map

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheEntry
import com.team.yeogibeoryeo.presentation.cache.RecentCurrentLocationCacheClearNotifier
import com.team.yeogibeoryeo.presentation.map.location.CurrentLocationResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionSpotMapCacheViewModelTest : CollectionSpotMapViewModelTestFixture() {
    @Test
    fun `위치 캐시 삭제 이벤트가 발생하면 현재 위치 검색 화면 상태를 초기화한다`() =
        runTest {
            val notifier = RecentCurrentLocationCacheClearNotifier()
            val locationResult = CompletableDeferred<CurrentLocationResult>()
            val refreshResult = CompletableDeferred<List<CollectionSpot>>()
            val cachedSpot = sampleSpot("cache", CollectionSpotType.STANDARD_BAG_STORE)
            val viewModel = createViewModel(
                repository = FakeCollectionSpotRepository(
                    locationSearchResultProvider = {
                        refreshResult.await()
                    },
                ),
                currentLocationProvider = FakeCurrentLocationProvider {
                    locationResult.await()
                },
                recentCurrentLocationSpotCacheRepository =
                    FakeRecentCurrentLocationSpotCacheRepository(
                        entry = freshCacheEntry(listOf(cachedSpot)),
                    ),
                recentCurrentLocationCacheClearNotifier = notifier,
            )

            viewModel.searchByCurrentLocation()
            locationResult.complete(CurrentLocationResult.Found(DEFAULT_CURRENT_COORDINATE))
            runCurrent()
            assertEquals(listOf(cachedSpot), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)

            notifier.notifyCleared()
            advanceUntilIdle()

            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertNull(viewModel.uiState.value.selectedSpot)
            assertFalse(viewModel.uiState.value.hasSearched)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(MapSearchMode.KEYWORD, viewModel.uiState.value.searchMode)
        }

    @Test
    fun `위치 캐시 삭제 이벤트가 발생해도 키워드 검색 결과는 유지한다`() =
        runTest {
            val notifier = RecentCurrentLocationCacheClearNotifier()
            val keywordSpot = sampleSpot("keyword", CollectionSpotType.STANDARD_BAG_STORE)
            val viewModel = createViewModel(
                repository = FakeCollectionSpotRepository(keywordSpots = listOf(keywordSpot)),
                currentLocationResult = CurrentLocationResult.NotFound,
                recentCurrentLocationCacheClearNotifier = notifier,
            )

            viewModel.onSearchKeywordChanged("문래동")
            viewModel.searchByKeyword()
            advanceUntilIdle()
            assertEquals(listOf(keywordSpot), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.KEYWORD, viewModel.uiState.value.searchMode)

            notifier.notifyCleared()
            advanceUntilIdle()

            assertEquals(listOf(keywordSpot), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.KEYWORD, viewModel.uiState.value.searchMode)
        }

    @Test
    fun `위치 캐시 삭제 요청 이벤트만 발생하면 현재 위치 검색 화면 상태를 유지한다`() =
        runTest {
            val notifier = RecentCurrentLocationCacheClearNotifier()
            val cachedSpot = sampleSpot("cache", CollectionSpotType.STANDARD_BAG_STORE)
            val refreshResult = CompletableDeferred<List<CollectionSpot>>()
            val viewModel = createViewModel(
                repository = FakeCollectionSpotRepository(
                    locationSearchResultProvider = {
                        refreshResult.await()
                    },
                ),
                currentLocationResult = CurrentLocationResult.Found(DEFAULT_CURRENT_COORDINATE),
                recentCurrentLocationSpotCacheRepository =
                    FakeRecentCurrentLocationSpotCacheRepository(
                        entry = freshCacheEntry(listOf(cachedSpot)),
                    ),
                recentCurrentLocationCacheClearNotifier = notifier,
            )

            viewModel.searchByCurrentLocation()
            advanceUntilIdle()
            assertEquals(listOf(cachedSpot), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)

            notifier.notifyClearRequested()
            advanceUntilIdle()

            assertEquals(listOf(cachedSpot), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
        }

    @Test
    fun `현재 위치 검색 중 캐시 삭제 이벤트가 발생하면 완료된 검색 결과를 캐시에 다시 저장하지 않는다`() =
        runTest {
            val notifier = RecentCurrentLocationCacheClearNotifier()
            val searchResult = CompletableDeferred<List<CollectionSpot>>()
            val locationSpot = sampleSpot("location", CollectionSpotType.STANDARD_BAG_STORE)
            val cache = FakeRecentCurrentLocationSpotCacheRepository()
            val repository = FakeCollectionSpotRepository(
                locationSearchResultProvider = {
                    searchResult.await()
                },
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
                recentCurrentLocationSpotCacheRepository = cache,
                recentCurrentLocationCacheClearNotifier = notifier,
            )

            viewModel.searchByCurrentLocation()
            runCurrent()
            assertEquals(1, repository.locationSearchCallCount)

            notifier.notifyCleared()
            searchResult.complete(listOf(locationSpot))
            advanceUntilIdle()

            assertEquals(0, cache.saveCallCount)
            assertNull(cache.entry)
            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertFalse(viewModel.uiState.value.hasSearched)
            assertEquals(MapSearchMode.KEYWORD, viewModel.uiState.value.searchMode)
        }

    @Test
    fun `현재 위치 캐시 조회 중 삭제 요청 이벤트가 발생하면 조회된 캐시를 화면에 표시하지 않는다`() =
        runTest {
            val notifier = RecentCurrentLocationCacheClearNotifier()
            val getCompletion = CompletableDeferred<Unit>()
            val cachedSpot = sampleSpot("cache", CollectionSpotType.STANDARD_BAG_STORE)
            val cache = FakeRecentCurrentLocationSpotCacheRepository(
                entry = freshCacheEntry(listOf(cachedSpot)),
                getCompletion = getCompletion,
            )
            val viewModel = createViewModel(
                repository = FakeCollectionSpotRepository(),
                currentLocationResult = CurrentLocationResult.Found(DEFAULT_CURRENT_COORDINATE),
                recentCurrentLocationSpotCacheRepository = cache,
                recentCurrentLocationCacheClearNotifier = notifier,
            )

            viewModel.searchByCurrentLocation()
            runCurrent()
            assertEquals(1, cache.getCallCount)

            notifier.notifyClearRequested()
            getCompletion.complete(Unit)
            advanceUntilIdle()

            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertEquals(true, viewModel.uiState.value.hasSearched)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
        }

    @Test
    fun `초기 지도 진입 캐시 조회 중 삭제 요청 이벤트가 발생하면 조회된 캐시를 화면에 표시하지 않는다`() =
        runTest {
            val notifier = RecentCurrentLocationCacheClearNotifier()
            val getCompletion = CompletableDeferred<Unit>()
            val cachedSpot = sampleSpot("cache", CollectionSpotType.STANDARD_BAG_STORE)
            val cache = FakeRecentCurrentLocationSpotCacheRepository(
                entry = freshCacheEntry(listOf(cachedSpot)),
                getCompletion = getCompletion,
            )
            val viewModel = createViewModel(
                repository = FakeCollectionSpotRepository(),
                currentLocationResult = CurrentLocationResult.Found(DEFAULT_CURRENT_COORDINATE),
                hasFineLocationPermission = true,
                recentCurrentLocationSpotCacheRepository = cache,
                recentCurrentLocationCacheClearNotifier = notifier,
            )

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted()
            runCurrent()
            assertEquals(1, cache.getCallCount)

            notifier.notifyClearRequested()
            getCompletion.complete(Unit)
            advanceUntilIdle()

            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertEquals(true, viewModel.uiState.value.hasSearched)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
        }

    @Test
    fun `현재 위치 검색은 신선한 캐시가 있으면 캐시를 먼저 표시하고 조용히 갱신한다`() =
        runTest {
            val currentCoordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881)
            val currentLocationResult = CompletableDeferred<CurrentLocationResult>()
            val refreshResult = CompletableDeferred<List<CollectionSpot>>()
            val cachedSpot = sampleSpot("cache", CollectionSpotType.STANDARD_BAG_STORE)
            val refreshedSpot = sampleSpot("refresh", CollectionSpotType.RECYCLING_CENTER)
            val repository = FakeCollectionSpotRepository(
                locationSearchResultProvider = {
                    refreshResult.await()
                },
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationProvider = FakeCurrentLocationProvider {
                    currentLocationResult.await()
                },
                recentCurrentLocationSpotCacheRepository =
                    FakeRecentCurrentLocationSpotCacheRepository(
                        entry = freshCacheEntry(listOf(cachedSpot)),
                    ),
            )

            viewModel.searchByCurrentLocation()

            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(0, repository.locationSearchCallCount)

            currentLocationResult.complete(
                CurrentLocationResult.Found(
                    currentCoordinate,
                ),
            )
            runCurrent()

            assertEquals(listOf(cachedSpot), viewModel.uiState.value.spots)
            assertEquals(1, repository.locationSearchCallCount)

            refreshResult.complete(listOf(refreshedSpot))
            advanceUntilIdle()

            assertEquals(listOf(refreshedSpot).withDistanceFrom(currentCoordinate), viewModel.uiState.value.spots)
        }

    @Test
    fun `현재 위치 요청이 지연되면 잠시 후 loading을 표시한다`() =
        runTest {
            val locationResult = CompletableDeferred<CurrentLocationResult>()
            val viewModel = createViewModel(
                repository = FakeCollectionSpotRepository(),
                currentLocationProvider = FakeCurrentLocationProvider {
                    locationResult.await()
                },
            )

            viewModel.searchByCurrentLocation()
            runCurrent()

            assertFalse(viewModel.uiState.value.isLoading)

            advanceTimeBy(300L)
            runCurrent()

            assertTrue(viewModel.uiState.value.isLoading)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
        }

    @Test
    fun `유효한 캐시와 위치 권한이 있으면 지도 진입 시 현재 위치 확인 후 캐시 결과를 표시한다`() =
        runTest {
            val locationResult = CompletableDeferred<CurrentLocationResult>()
            val refreshResult = CompletableDeferred<List<CollectionSpot>>()
            val cachedSpot = sampleSpot("cache", CollectionSpotType.STANDARD_BAG_STORE)
            val cache = FakeRecentCurrentLocationSpotCacheRepository(
                entry = freshCacheEntry(listOf(cachedSpot)),
            )
            val repository = FakeCollectionSpotRepository(
                locationSearchResultProvider = {
                    refreshResult.await()
                },
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationProvider = FakeCurrentLocationProvider {
                    locationResult.await()
                },
                hasFineLocationPermission = true,
                recentCurrentLocationSpotCacheRepository = cache,
            )

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted()

            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
            assertFalse(viewModel.uiState.value.isLoading)

            locationResult.complete(CurrentLocationResult.Found(DEFAULT_CURRENT_COORDINATE))
            runCurrent()

            assertEquals(listOf(cachedSpot), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(1, repository.locationSearchCallCount)
        }

    @Test
    fun `캐시 결과에서도 필터칩 선택 시 정상 필터링된다`() =
        runTest {
            val locationResult = CompletableDeferred<CurrentLocationResult>()
            val refreshResult = CompletableDeferred<List<CollectionSpot>>()
            val standardBagSpot = sampleSpot("cache-standard", CollectionSpotType.STANDARD_BAG_STORE)
            val recyclingCenterSpot = sampleSpot("cache-recycling", CollectionSpotType.RECYCLING_CENTER)
            val cache = FakeRecentCurrentLocationSpotCacheRepository(
                entry = freshCacheEntry(
                    spots = listOf(standardBagSpot, recyclingCenterSpot),
                ),
            )
            val repository = FakeCollectionSpotRepository(
                locationSearchResultProvider = {
                    refreshResult.await()
                },
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationProvider = FakeCurrentLocationProvider {
                    locationResult.await()
                },
                hasFineLocationPermission = true,
                recentCurrentLocationSpotCacheRepository = cache,
            )

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted()
            locationResult.complete(CurrentLocationResult.Found(DEFAULT_CURRENT_COORDINATE))
            runCurrent()
            viewModel.onSpotTypeClick(CollectionSpotType.RECYCLING_CENTER)

            assertEquals(setOf(CollectionSpotType.RECYCLING_CENTER), viewModel.uiState.value.selectedTypes)
            assertEquals(listOf(recyclingCenterSpot), viewModel.uiState.value.spots)
            assertEquals(1, repository.locationSearchCallCount)
        }

    @Test
    fun `유효한 캐시 표시 후 refresh 성공 시 화면 결과와 캐시를 최신 결과로 교체한다`() =
        runTest {
            val currentCoordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881)
            val cachedSpot = sampleSpot("cache", CollectionSpotType.STANDARD_BAG_STORE)
            val refreshedSpot = sampleSpot("refresh", CollectionSpotType.RECYCLING_CENTER)
            val cache = FakeRecentCurrentLocationSpotCacheRepository(
                entry = freshCacheEntry(listOf(cachedSpot)),
            )
            val repository = FakeCollectionSpotRepository(locationSpots = listOf(refreshedSpot))
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
                hasFineLocationPermission = true,
                recentCurrentLocationSpotCacheRepository = cache,
            )

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted()
            advanceUntilIdle()

            assertEquals(listOf(refreshedSpot).withDistanceFrom(currentCoordinate), viewModel.uiState.value.spots)
            assertEquals(listOf(refreshedSpot).withDistanceFrom(currentCoordinate), cache.entry?.spots)
            assertEquals(1, repository.locationSearchCallCount)
            assertEquals(1, cache.saveCallCount)
        }

    @Test
    fun `현재 위치 조회 실패 시 유효한 캐시가 있어도 표시하지 않는다`() =
        runTest {
            val cachedSpot = sampleSpot("cache", CollectionSpotType.STANDARD_BAG_STORE)
            val cache = FakeRecentCurrentLocationSpotCacheRepository(
                entry = freshCacheEntry(listOf(cachedSpot)),
            )
            val repository = FakeCollectionSpotRepository()
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
                hasFineLocationPermission = true,
                recentCurrentLocationSpotCacheRepository = cache,
            )

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted()
            advanceUntilIdle()

            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.KEYWORD, viewModel.uiState.value.searchMode)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(
                MapLocationNotices.CurrentLocationUnavailable.titleResId,
                viewModel.uiState.value.locationNotice?.titleResId,
            )
            assertNull(viewModel.uiState.value.errorMessageResId)
            assertEquals(0, repository.locationSearchCallCount)
            assertEquals(0, cache.saveCallCount)
        }

    @Test
    fun `지도 진입 시 캐시가 fresh해도 현재 위치와 멀면 캐시를 표시하지 않고 새 위치로 검색한다`() =
        runTest {
            val currentCoordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881)
            val cachedSpot = sampleSpot("cache", CollectionSpotType.OTHER)
            val expectedSpot = sampleSpot("location", CollectionSpotType.STANDARD_BAG_STORE)
            val cache = FakeRecentCurrentLocationSpotCacheRepository(
                entry = freshCacheEntry(
                    spots = listOf(cachedSpot),
                    searchCoordinate = Coordinate(latitude = 37.0, longitude = 126.0),
                ),
            )
            val repository = FakeCollectionSpotRepository(locationSpots = listOf(expectedSpot))
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(currentCoordinate),
                hasFineLocationPermission = true,
                recentCurrentLocationSpotCacheRepository = cache,
            )

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted()
            advanceUntilIdle()

            assertEquals(1, cache.clearCallCount)
            assertEquals(1, repository.locationSearchCallCount)
            assertEquals(currentCoordinate, repository.lastLocationCoordinate)
            assertEquals(listOf(expectedSpot).withDistanceFrom(currentCoordinate), viewModel.uiState.value.spots)
            assertEquals(listOf(expectedSpot).withDistanceFrom(currentCoordinate), cache.entry?.spots)
            assertEquals(currentCoordinate, cache.entry?.searchCoordinate)
        }

    @Test
    fun `캐시가 없으면 기존 현재 위치 자동 검색 흐름을 실행한다`() =
        runTest {
            val currentCoordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881)
            val expectedSpot = sampleSpot("location", CollectionSpotType.STANDARD_BAG_STORE)
            val cache = FakeRecentCurrentLocationSpotCacheRepository()
            val repository = FakeCollectionSpotRepository(locationSpots = listOf(expectedSpot))
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(currentCoordinate),
                hasFineLocationPermission = true,
                recentCurrentLocationSpotCacheRepository = cache,
            )

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted()
            advanceUntilIdle()

            assertEquals(1, cache.getCallCount)
            assertEquals(1, repository.locationSearchCallCount)
            assertEquals(currentCoordinate, repository.lastLocationCoordinate)
            assertEquals(listOf(expectedSpot).withDistanceFrom(currentCoordinate), viewModel.uiState.value.spots)
        }

    @Test
    fun `캐시가 만료되면 기존 현재 위치 자동 검색 흐름을 실행한다`() =
        runTest {
            val expiredSpot = sampleSpot("expired", CollectionSpotType.OTHER)
            val expectedSpot = sampleSpot("location", CollectionSpotType.STANDARD_BAG_STORE)
            val cache = FakeRecentCurrentLocationSpotCacheRepository(
                entry = RecentCurrentLocationSpotCacheEntry(
                    spots = listOf(expiredSpot),
                    searchCoordinate = DEFAULT_CURRENT_COORDINATE,
                    savedAtMillis = 0L,
                ),
            )
            val repository = FakeCollectionSpotRepository(locationSpots = listOf(expectedSpot))
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
                hasFineLocationPermission = true,
                recentCurrentLocationSpotCacheRepository = cache,
            )

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted()
            advanceUntilIdle()

            assertEquals(1, repository.locationSearchCallCount)
            assertEquals(1, cache.clearCallCount)
            assertEquals(
                listOf(expectedSpot).withDistanceFrom(Coordinate(latitude = 37.5666102, longitude = 126.9783881)),
                cache.entry?.spots,
            )
            assertEquals(
                listOf(expectedSpot).withDistanceFrom(Coordinate(latitude = 37.5666102, longitude = 126.9783881)),
                viewModel.uiState.value.spots,
            )
        }

    @Test
    fun `위치 권한이 없으면 캐시가 있어도 자동 표시하지 않는다`() =
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

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted()

            assertEquals(0, cache.getCallCount)
            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertFalse(viewModel.uiState.value.hasSearched)
            assertEquals(MapLocationNotices.PermissionDenied, viewModel.uiState.value.locationNotice)
            assertEquals(0, repository.locationSearchCallCount)
        }

    @Test
    fun `refresh 중 키워드 검색이 시작되면 refresh 결과가 키워드 검색 결과를 덮어쓰지 않는다`() =
        runTest {
            val locationResult = CompletableDeferred<CurrentLocationResult>()
            val cachedSpot = sampleSpot("cache", CollectionSpotType.STANDARD_BAG_STORE)
            val keywordSpot = sampleSpot("keyword", CollectionSpotType.OTHER)
            val refreshSpot = sampleSpot("refresh", CollectionSpotType.RECYCLING_CENTER)
            val cache = FakeRecentCurrentLocationSpotCacheRepository(
                entry = freshCacheEntry(listOf(cachedSpot)),
            )
            val repository = FakeCollectionSpotRepository(
                keywordSpots = listOf(keywordSpot),
                locationSpots = listOf(refreshSpot),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationProvider = FakeCurrentLocationProvider {
                    locationResult.await()
                },
                hasFineLocationPermission = true,
                recentCurrentLocationSpotCacheRepository = cache,
            )

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted()
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
            assertEquals(0, repository.locationSearchCallCount)
            assertEquals(0, cache.saveCallCount)
        }

    @Test
    fun `현재 위치 버튼 수동 검색 성공 시 캐시를 갱신한다`() =
        runTest {
            val expectedSpot = sampleSpot("location", CollectionSpotType.STANDARD_BAG_STORE)
            val cache = FakeRecentCurrentLocationSpotCacheRepository()
            val repository = FakeCollectionSpotRepository(locationSpots = listOf(expectedSpot))
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
                recentCurrentLocationSpotCacheRepository = cache,
            )

            viewModel.searchByCurrentLocation()

            assertEquals(1, cache.saveCallCount)
            assertEquals(
                listOf(expectedSpot).withDistanceFrom(Coordinate(latitude = 37.5666102, longitude = 126.9783881)),
                cache.entry?.spots,
            )
            assertEquals(Coordinate(latitude = 37.5666102, longitude = 126.9783881), cache.entry?.searchCoordinate)
        }

    @Test
    fun `키워드 검색 성공 시 캐시를 갱신하지 않는다`() =
        runTest {
            val keywordSpot = sampleSpot("keyword", CollectionSpotType.OTHER)
            val cache = FakeRecentCurrentLocationSpotCacheRepository()
            val repository = FakeCollectionSpotRepository(keywordSpots = listOf(keywordSpot))
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
                recentCurrentLocationSpotCacheRepository = cache,
            )

            viewModel.onSearchKeywordChanged("문래동")
            viewModel.searchByKeyword()

            assertEquals(listOf(keywordSpot), viewModel.uiState.value.spots)
            assertEquals(0, cache.saveCallCount)
            assertNull(cache.entry)
        }

}
