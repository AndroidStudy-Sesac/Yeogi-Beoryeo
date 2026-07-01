package com.team.yeogibeoryeo.presentation.map

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheEntry
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
class CollectionSpotMapCacheViewModelTest : CollectionSpotMapViewModelTestFixture() {
    @Test
    fun `현재 위치 검색은 신선한 캐시가 있으면 캐시를 먼저 표시하고 조용히 갱신한다`() =
        runTest {
            val currentCoordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881)
            val currentLocationResult = CompletableDeferred<CurrentLocationResult>()
            val cachedSpot = sampleSpot("cache", CollectionSpotType.STANDARD_BAG_STORE)
            val refreshedSpot = sampleSpot("refresh", CollectionSpotType.RECYCLING_CENTER)
            val repository = FakeCollectionSpotRepository(
                locationSpots = listOf(refreshedSpot),
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
            advanceUntilIdle()

            assertEquals(listOf(cachedSpot), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(0, repository.locationSearchCallCount)

            currentLocationResult.complete(
                CurrentLocationResult.Found(
                    currentCoordinate,
                ),
            )
            advanceUntilIdle()

            assertEquals(listOf(refreshedSpot).withDistanceFrom(currentCoordinate), viewModel.uiState.value.spots)
            assertEquals(1, repository.locationSearchCallCount)
        }

    @Test
    fun `유효한 캐시와 위치 권한이 있으면 지도 진입 시 캐시 결과를 즉시 표시한다`() =
        runTest {
            val locationResult = CompletableDeferred<CurrentLocationResult>()
            val cachedSpot = sampleSpot("cache", CollectionSpotType.STANDARD_BAG_STORE)
            val cache = FakeRecentCurrentLocationSpotCacheRepository(
                entry = freshCacheEntry(listOf(cachedSpot)),
            )
            val repository = FakeCollectionSpotRepository(
                locationSpots = listOf(sampleSpot("refresh", CollectionSpotType.RECYCLING_CENTER)),
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

            assertEquals(listOf(cachedSpot), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(0, repository.locationSearchCallCount)
        }

    @Test
    fun `캐시 결과에서도 필터칩 선택 시 정상 필터링된다`() =
        runTest {
            val locationResult = CompletableDeferred<CurrentLocationResult>()
            val standardBagSpot = sampleSpot("cache-standard", CollectionSpotType.STANDARD_BAG_STORE)
            val recyclingCenterSpot = sampleSpot("cache-recycling", CollectionSpotType.RECYCLING_CENTER)
            val cache = FakeRecentCurrentLocationSpotCacheRepository(
                entry = freshCacheEntry(
                    spots = listOf(standardBagSpot, recyclingCenterSpot),
                ),
            )
            val repository = FakeCollectionSpotRepository(
                locationSpots = listOf(sampleSpot("refresh", CollectionSpotType.OTHER)),
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
            viewModel.onSpotTypeClick(CollectionSpotType.RECYCLING_CENTER)

            assertEquals(setOf(CollectionSpotType.RECYCLING_CENTER), viewModel.uiState.value.selectedTypes)
            assertEquals(listOf(recyclingCenterSpot), viewModel.uiState.value.spots)
            assertEquals(0, repository.locationSearchCallCount)
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
    fun `유효한 캐시 표시 후 refresh 실패 시 기존 캐시 결과를 유지한다`() =
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

            assertEquals(listOf(cachedSpot), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.locationNotice)
            assertNull(viewModel.uiState.value.locationNoticeMessage)
            assertNull(viewModel.uiState.value.errorMessage)
            assertEquals(0, repository.locationSearchCallCount)
            assertEquals(0, cache.saveCallCount)
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
            assertNull(viewModel.uiState.value.locationNotice)
            assertNull(viewModel.uiState.value.locationNoticeMessage)
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
