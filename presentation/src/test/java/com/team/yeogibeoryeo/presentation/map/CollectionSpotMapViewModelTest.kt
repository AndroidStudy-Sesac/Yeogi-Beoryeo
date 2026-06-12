package com.team.yeogibeoryeo.presentation.map

import com.team.yeogibeoryeo.domain.favorite.model.CollectionSpotFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.model.toFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteSnapshotRepository
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoritesUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ToggleCollectionSpotFavoriteUseCase
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheEntry
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotRepository
import com.team.yeogibeoryeo.domain.spot.repository.RecentCurrentLocationSpotCacheRepository
import com.team.yeogibeoryeo.domain.spot.usecase.FilterCollectionSpotsUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.GetFreshRecentCurrentLocationSpotsUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.SaveRecentCurrentLocationSpotsUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.SearchCollectionSpotsByKeywordUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.SearchCollectionSpotsByLocationUseCase
import com.team.yeogibeoryeo.domain.time.TimeProvider
import com.team.yeogibeoryeo.presentation.map.location.CurrentLocationProvider
import com.team.yeogibeoryeo.presentation.map.location.CurrentLocationResult
import com.team.yeogibeoryeo.presentation.map.location.LocationPermissionChecker
import com.team.yeogibeoryeo.presentation.map.model.FavoriteSpotMapMoveRequest
import com.team.yeogibeoryeo.presentation.search.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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
            assertEquals("위치 권한이 필요합니다.", viewModel.uiState.value.locationNotice?.title)
            assertEquals(
                viewModel.uiState.value.locationNotice?.message,
                viewModel.uiState.value.locationNoticeMessage,
            )
            assertEquals(MapLocationNoticeAction.OpenAppSettings, viewModel.uiState.value.locationNotice?.action)
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
                "현재 위치를 확인하지 못했습니다. 잠시 후 다시 시도하거나 직접 동네명/주소를 검색해 주세요.",
                viewModel.uiState.value.locationNoticeMessage,
            )
            assertEquals("현재 위치를 확인하지 못했습니다.", viewModel.uiState.value.locationNotice?.title)
            assertEquals(
                viewModel.uiState.value.locationNotice?.message,
                viewModel.uiState.value.locationNoticeMessage,
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
                "기기의 위치 서비스가 꺼져 있어 현재 위치를 확인할 수 없어요. 위치 서비스를 켠 뒤 다시 시도하거나 직접 동네명/주소를 검색해 주세요.",
                viewModel.uiState.value.locationNoticeMessage,
            )
            assertEquals("위치 서비스가 꺼져 있습니다.", viewModel.uiState.value.locationNotice?.title)
            assertEquals(
                viewModel.uiState.value.locationNotice?.message,
                viewModel.uiState.value.locationNoticeMessage,
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
            assertEquals(expectedSpots, viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
            assertNull(viewModel.uiState.value.locationNoticeMessage)
            assertNull(viewModel.uiState.value.errorMessage)
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
                "네트워크 연결을 확인한 뒤 다시 시도하거나 직접 동네명/주소를 검색해 주세요.",
                viewModel.uiState.value.errorMessage,
            )
            assertNull(viewModel.uiState.value.locationNotice)
            assertNull(viewModel.uiState.value.locationNoticeMessage)
        }

    @Test
    fun `키워드 검색 실패 시 원문 예외 대신 안내 문구를 표시한다`() =
        runTest {
            val repository = FakeCollectionSpotRepository(
                keywordSearchThrowable = IllegalStateException(
                    "Unable to resolve host \"apis.data.go.kr\": No address associated with hostname",
                ),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.onSearchKeywordChanged("용답동")
            viewModel.searchByKeyword()

            assertEquals(listOf("용답동"), repository.keywords)
            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals("네트워크 연결을 확인한 뒤 다시 시도해 주세요.", viewModel.uiState.value.errorMessage)
            assertNull(viewModel.uiState.value.locationNotice)
            assertNull(viewModel.uiState.value.locationNoticeMessage)
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
            assertNull(viewModel.uiState.value.locationNotice)
            assertNull(viewModel.uiState.value.locationNoticeMessage)
            assertNull(viewModel.uiState.value.errorMessage)
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

            assertEquals("위치 서비스가 꺼져 있습니다.", viewModel.uiState.value.locationNotice?.title)
            assertEquals(MapLocationNoticeAction.OpenLocationSettings, viewModel.uiState.value.locationNotice?.action)
            assertEquals(
                viewModel.uiState.value.locationNotice?.message,
                viewModel.uiState.value.locationNoticeMessage,
            )
            assertNull(viewModel.uiState.value.errorMessage)
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
            assertEquals(listOf(locationSpot), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
            assertFalse(viewModel.uiState.value.isLoading)
        }

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
            assertEquals(expectedSpots, viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
        }

    @Test
    fun `지도 진입 시 위치 권한이 없으면 자동 현재 위치 검색을 실행하지 않는다`() =
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
            assertNull(viewModel.uiState.value.locationNoticeMessage)
            assertNull(viewModel.uiState.value.errorMessage)
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
            assertEquals(listOf(locationSpot), viewModel.uiState.value.spots)
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

            assertEquals(listOf(refreshedSpot), viewModel.uiState.value.spots)
            assertEquals(listOf(refreshedSpot), cache.entry?.spots)
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
            assertEquals(listOf(expectedSpot), viewModel.uiState.value.spots)
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
            assertEquals(listOf(expectedSpot), viewModel.uiState.value.spots)
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
            assertEquals(listOf(expectedSpot), cache.entry?.spots)
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

    @Test
    fun `키워드 검색 결과에 즐겨찾기 저장소 기준 상태를 반영한다`() =
        runTest {
            val favoriteSpot = sampleSpot("favorite", CollectionSpotType.BATTERY_BIN)
            val normalSpot = sampleSpot("normal", CollectionSpotType.RECYCLING_CENTER)
            val favoriteRepository = FakeFavoriteRepository(
                initialFavorites = listOf(collectionSpotFavorite(favoriteSpot.id)),
            )
            val viewModel = createViewModel(
                repository = FakeCollectionSpotRepository(
                    keywordSpots = listOf(favoriteSpot, normalSpot),
                ),
                currentLocationResult = CurrentLocationResult.NotFound,
                favoriteRepository = favoriteRepository,
            )
            advanceUntilIdle()

            viewModel.onSearchKeywordChanged("문래동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            assertEquals(
                listOf(true, false),
                viewModel.uiState.value.spots.map { spot -> spot.isBookmarked },
            )
        }

    @Test
    fun `캐시 결과에 즐겨찾기 저장소 기준 상태를 반영한다`() =
        runTest {
            val cachedSpot = sampleSpot("cache", CollectionSpotType.STANDARD_BAG_STORE)
            val locationResult = CompletableDeferred<CurrentLocationResult>()
            val viewModel = createViewModel(
                repository = FakeCollectionSpotRepository(),
                currentLocationProvider = FakeCurrentLocationProvider {
                    locationResult.await()
                },
                hasFineLocationPermission = true,
                recentCurrentLocationSpotCacheRepository = FakeRecentCurrentLocationSpotCacheRepository(
                    entry = freshCacheEntry(listOf(cachedSpot.copy(isBookmarked = false))),
                ),
                favoriteRepository = FakeFavoriteRepository(
                    initialFavorites = listOf(collectionSpotFavorite(cachedSpot.id)),
                ),
            )
            advanceUntilIdle()

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted()
            advanceUntilIdle()

            assertEquals(true, viewModel.uiState.value.spots.single().isBookmarked)
        }

    @Test
    fun `즐겨찾기 토글 시 공통 Favorite와 스냅샷을 함께 저장한다`() =
        runTest {
            val spot = sampleSpot("spot", CollectionSpotType.BATTERY_BIN)
            val favoriteRepository = FakeFavoriteRepository()
            val snapshotRepository = FakeCollectionSpotFavoriteSnapshotRepository()
            val viewModel = createViewModel(
                repository = FakeCollectionSpotRepository(keywordSpots = listOf(spot)),
                currentLocationResult = CurrentLocationResult.NotFound,
                favoriteRepository = favoriteRepository,
                snapshotRepository = snapshotRepository,
            )

            viewModel.onSearchKeywordChanged("문래동")
            viewModel.searchByKeyword()
            advanceUntilIdle()
            viewModel.onSpotFavoriteClick(spot)
            advanceUntilIdle()

            assertEquals(true, favoriteRepository.isFavorite(FavoriteTargetType.COLLECTION_SPOT, spot.id))
            assertEquals(spot.id, snapshotRepository.snapshots.value.single().targetId)
            assertEquals(true, viewModel.uiState.value.spots.single().isBookmarked)
        }

    @Test
    fun `즐겨찾기 해제 시 공통 Favorite와 스냅샷을 함께 삭제한다`() =
        runTest {
            val spot = sampleSpot("spot", CollectionSpotType.BATTERY_BIN)
            val favoriteRepository = FakeFavoriteRepository(
                initialFavorites = listOf(collectionSpotFavorite(spot.id)),
            )
            val snapshotRepository = FakeCollectionSpotFavoriteSnapshotRepository(
                initialSnapshots = listOf(
                    CollectionSpotFavoriteSnapshot(
                        targetId = spot.id,
                        name = spot.name,
                        type = spot.type,
                        address = spot.address,
                        detailLocation = spot.detailLocation,
                        coordinate = spot.coordinate,
                    ),
                ),
            )
            val viewModel = createViewModel(
                repository = FakeCollectionSpotRepository(keywordSpots = listOf(spot)),
                currentLocationResult = CurrentLocationResult.NotFound,
                favoriteRepository = favoriteRepository,
                snapshotRepository = snapshotRepository,
            )

            viewModel.onSearchKeywordChanged("문래동")
            viewModel.searchByKeyword()
            advanceUntilIdle()
            viewModel.onSpotFavoriteClick(viewModel.uiState.value.spots.single())
            advanceUntilIdle()

            assertEquals(false, favoriteRepository.isFavorite(FavoriteTargetType.COLLECTION_SPOT, spot.id))
            assertEquals(emptyList<CollectionSpotFavoriteSnapshot>(), snapshotRepository.snapshots.value)
            assertEquals(false, viewModel.uiState.value.spots.single().isBookmarked)
        }

    @Test
    fun `즐겨찾기 장소 지도 이동 요청을 selectedSpot으로 반영한다`() =
        runTest {
            val request =
                FavoriteSpotMapMoveRequest(
                    requestId = "request-1",
                    targetId = "favorite-spot",
                    name = "폐건전지 수거함",
                    type = CollectionSpotType.BATTERY_BIN,
                    address = "서울특별시 영등포구 문래동",
                    detailLocation = "주민센터 앞",
                    coordinate = Coordinate(latitude = 37.5, longitude = 126.9),
                )
            val viewModel = createViewModel(
                repository = FakeCollectionSpotRepository(),
                currentLocationResult = CurrentLocationResult.NotFound,
                favoriteRepository = FakeFavoriteRepository(
                    initialFavorites = listOf(collectionSpotFavorite(request.targetId)),
                ),
            )
            advanceUntilIdle()

            viewModel.showFavoriteSpot(request)

            assertEquals(request.targetId, viewModel.uiState.value.selectedSpot?.id)
            assertEquals(request.coordinate, viewModel.uiState.value.selectedSpot?.coordinate)
            assertEquals(true, viewModel.uiState.value.selectedSpot?.isBookmarked)
            assertEquals(request.targetId, viewModel.uiState.value.favoriteSpotMoveRequestId)
            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
        }

    @Test
    fun `현재 검색 결과에 같은 즐겨찾기 장소가 있으면 기존 장소 정보를 선택한다`() =
        runTest {
            val existingSpot = sampleSpot("favorite-spot", CollectionSpotType.RECYCLING_CENTER)
            val request =
                FavoriteSpotMapMoveRequest(
                    requestId = "request-1",
                    targetId = existingSpot.id,
                    name = "스냅샷 이름",
                    type = CollectionSpotType.BATTERY_BIN,
                    address = "스냅샷 주소",
                    detailLocation = null,
                    coordinate = Coordinate(latitude = 37.5, longitude = 126.9),
                )
            val viewModel = createViewModel(
                repository = FakeCollectionSpotRepository(keywordSpots = listOf(existingSpot)),
                currentLocationResult = CurrentLocationResult.NotFound,
                favoriteRepository = FakeFavoriteRepository(
                    initialFavorites = listOf(collectionSpotFavorite(existingSpot.id)),
                ),
            )
            advanceUntilIdle()
            viewModel.onSearchKeywordChanged("문래동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            viewModel.showFavoriteSpot(request)

            assertEquals(existingSpot.name, viewModel.uiState.value.selectedSpot?.name)
            assertEquals(existingSpot.type, viewModel.uiState.value.selectedSpot?.type)
            assertEquals(true, viewModel.uiState.value.selectedSpot?.isBookmarked)
        }

    @Test
    fun `즐겨찾기 장소 지도 이동 후 해당 좌표 기준 주변 수거 장소 목록을 갱신한다`() =
        runTest {
            val request =
                FavoriteSpotMapMoveRequest(
                    requestId = "request-1",
                    targetId = "favorite-spot",
                    name = "폐건전지 수거함",
                    type = CollectionSpotType.BATTERY_BIN,
                    address = "서울특별시 성동구 용답동",
                    detailLocation = "주민센터 앞",
                    coordinate = Coordinate(latitude = 37.5, longitude = 126.9),
                )
            val nearbySpot = sampleSpot("nearby", CollectionSpotType.RECYCLING_CENTER)
            val repository =
                FakeCollectionSpotRepository(
                    locationSpots = listOf(nearbySpot),
                )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.showFavoriteSpot(request)
            advanceUntilIdle()

            assertEquals(request.coordinate, repository.lastLocationCoordinate)
            assertEquals(listOf(nearbySpot), viewModel.uiState.value.spots)
            assertEquals(request.targetId, viewModel.uiState.value.selectedSpot?.id)
            assertEquals(true, viewModel.uiState.value.hasSearched)
            assertEquals(false, viewModel.uiState.value.isFavoriteSpotNearbyLoading)
        }

    @Test
    fun `즐겨찾기 장소 주변 목록 조회 중 로딩 상태를 표시한다`() =
        runTest {
            val request = sampleFavoriteSpotMapMoveRequest()
            val nearbySpot = sampleSpot("nearby", CollectionSpotType.RECYCLING_CENTER)
            val locationSearchResult = CompletableDeferred<List<CollectionSpot>>()
            val repository =
                FakeCollectionSpotRepository(
                    locationSearchResultProvider = { locationSearchResult.await() },
                )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.showFavoriteSpot(request)
            advanceUntilIdle()

            assertEquals(request.targetId, viewModel.uiState.value.selectedSpot?.id)
            assertEquals(true, viewModel.uiState.value.isFavoriteSpotNearbyLoading)

            locationSearchResult.complete(listOf(nearbySpot))
            advanceUntilIdle()

            assertEquals(listOf(nearbySpot), viewModel.uiState.value.spots)
            assertEquals(false, viewModel.uiState.value.isFavoriteSpotNearbyLoading)
        }

    @Test
    fun `즐겨찾기 장소 주변 목록 조회 실패 시에도 selectedSpot을 유지한다`() =
        runTest {
            val request = sampleFavoriteSpotMapMoveRequest()
            val viewModel = createViewModel(
                repository = FakeCollectionSpotRepository(
                    locationSearchThrowable = IllegalStateException("network error"),
                ),
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.showFavoriteSpot(request)
            advanceUntilIdle()

            assertEquals(request.targetId, viewModel.uiState.value.selectedSpot?.id)
            assertEquals(request.coordinate, viewModel.uiState.value.selectedSpot?.coordinate)
            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertEquals(false, viewModel.uiState.value.isFavoriteSpotNearbyLoading)
        }

    @Test
    fun `같은 즐겨찾기 장소 이동 요청도 다시 처리할 수 있도록 sequence를 증가시킨다`() =
        runTest {
            val request = sampleFavoriteSpotMapMoveRequest(requestId = "request-1")
            val viewModel = createViewModel(
                repository = FakeCollectionSpotRepository(),
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.showFavoriteSpot(request)
            advanceUntilIdle()
            val firstSequence = viewModel.uiState.value.favoriteSpotMoveRequestSequence

            viewModel.showFavoriteSpot(request.copy(requestId = "request-2"))
            advanceUntilIdle()

            assertEquals(request.targetId, viewModel.uiState.value.selectedSpot?.id)
            assertEquals(firstSequence + 1, viewModel.uiState.value.favoriteSpotMoveRequestSequence)
        }

    @Test
    fun `이미 소비한 즐겨찾기 장소 이동 요청은 다시 처리하지 않는다`() =
        runTest {
            val request = sampleFavoriteSpotMapMoveRequest(requestId = "request-1")
            val viewModel = createViewModel(
                repository = FakeCollectionSpotRepository(),
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.showFavoriteSpot(request)
            advanceUntilIdle()
            val firstSequence = viewModel.uiState.value.favoriteSpotMoveRequestSequence

            viewModel.showFavoriteSpot(request)
            advanceUntilIdle()

            assertEquals(firstSequence, viewModel.uiState.value.favoriteSpotMoveRequestSequence)
        }

    @Test
    fun `즐겨찾기 장소 주변 목록 조회 중 키워드 검색을 시작하면 로딩 상태를 정리한다`() =
        runTest {
            val request = sampleFavoriteSpotMapMoveRequest()
            val locationSearchResult = CompletableDeferred<List<CollectionSpot>>()
            val keywordSpot = sampleSpot("keyword", CollectionSpotType.OTHER)
            val repository =
                FakeCollectionSpotRepository(
                    keywordSpots = listOf(keywordSpot),
                    locationSearchResultProvider = { locationSearchResult.await() },
                )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.showFavoriteSpot(request)
            advanceUntilIdle()
            assertEquals(true, viewModel.uiState.value.isFavoriteSpotNearbyLoading)

            viewModel.onSearchKeywordChanged("문래동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            assertEquals(false, viewModel.uiState.value.isFavoriteSpotNearbyLoading)
            assertEquals(listOf(keywordSpot), viewModel.uiState.value.spots)
            assertEquals(null, viewModel.uiState.value.selectedSpot)
        }

    private fun createViewModel(
        repository: FakeCollectionSpotRepository,
        currentLocationResult: CurrentLocationResult,
        hasFineLocationPermission: Boolean = true,
        recentCurrentLocationSpotCacheRepository: RecentCurrentLocationSpotCacheRepository =
            FakeRecentCurrentLocationSpotCacheRepository(),
        timeProvider: TimeProvider = FakeTimeProvider(),
        favoriteRepository: FakeFavoriteRepository = FakeFavoriteRepository(),
        snapshotRepository: FakeCollectionSpotFavoriteSnapshotRepository =
            FakeCollectionSpotFavoriteSnapshotRepository(),
    ): CollectionSpotMapViewModel {
        return createViewModel(
            repository = repository,
            currentLocationProvider = FakeCurrentLocationProvider(currentLocationResult),
            hasFineLocationPermission = hasFineLocationPermission,
            recentCurrentLocationSpotCacheRepository = recentCurrentLocationSpotCacheRepository,
            timeProvider = timeProvider,
            favoriteRepository = favoriteRepository,
            snapshotRepository = snapshotRepository,
        )
    }

    private fun createViewModel(
        repository: FakeCollectionSpotRepository,
        currentLocationProvider: CurrentLocationProvider,
        hasFineLocationPermission: Boolean = true,
        recentCurrentLocationSpotCacheRepository: RecentCurrentLocationSpotCacheRepository =
            FakeRecentCurrentLocationSpotCacheRepository(),
        timeProvider: TimeProvider = FakeTimeProvider(),
        favoriteRepository: FakeFavoriteRepository = FakeFavoriteRepository(),
        snapshotRepository: FakeCollectionSpotFavoriteSnapshotRepository =
            FakeCollectionSpotFavoriteSnapshotRepository(),
    ): CollectionSpotMapViewModel {
        return CollectionSpotMapViewModel(
            searchCollectionSpotsByKeywordUseCase = SearchCollectionSpotsByKeywordUseCase(repository),
            searchCollectionSpotsByLocationUseCase = SearchCollectionSpotsByLocationUseCase(repository),
            filterCollectionSpotsUseCase = FilterCollectionSpotsUseCase(),
            currentLocationProvider = currentLocationProvider,
            locationPermissionChecker = FakeLocationPermissionChecker(hasFineLocationPermission),
            getFreshRecentCurrentLocationSpotsUseCase = GetFreshRecentCurrentLocationSpotsUseCase(
                repository = recentCurrentLocationSpotCacheRepository,
                timeProvider = timeProvider,
            ),
            saveRecentCurrentLocationSpotsUseCase = SaveRecentCurrentLocationSpotsUseCase(
                repository = recentCurrentLocationSpotCacheRepository,
                timeProvider = timeProvider,
            ),
            observeFavoritesUseCase = ObserveFavoritesUseCase(favoriteRepository),
            toggleCollectionSpotFavoriteUseCase = ToggleCollectionSpotFavoriteUseCase(
                collectionSpotFavoriteRepository =
                    FakeCollectionSpotFavoriteRepository(
                        favoriteRepository = favoriteRepository,
                        snapshotRepository = snapshotRepository,
                    ),
            ),
        )
    }

    private fun freshCacheEntry(
        spots: List<CollectionSpot>,
    ): RecentCurrentLocationSpotCacheEntry {
        return RecentCurrentLocationSpotCacheEntry(
            spots = spots,
            savedAtMillis = TEST_NOW_MILLIS,
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

    private fun sampleFavoriteSpotMapMoveRequest(
        requestId: String = "request-1",
    ): FavoriteSpotMapMoveRequest {
        return FavoriteSpotMapMoveRequest(
            requestId = requestId,
            targetId = "favorite-spot",
            name = "폐건전지 수거함",
            type = CollectionSpotType.BATTERY_BIN,
            address = "서울특별시 영등포구 문래동",
            detailLocation = "주민센터 앞",
            coordinate = Coordinate(latitude = 37.5, longitude = 126.9),
        )
    }

    private class FakeCurrentLocationProvider(
        private val resultProvider: suspend () -> CurrentLocationResult,
    ) : CurrentLocationProvider {

        constructor(result: CurrentLocationResult) : this({ result })

        override suspend fun getCurrentLocation(): CurrentLocationResult = resultProvider()
    }

    private class FakeLocationPermissionChecker(
        private val hasFineLocationPermission: Boolean,
    ) : LocationPermissionChecker {
        override fun hasFineLocationPermission(): Boolean = hasFineLocationPermission
    }

    private class FakeTimeProvider(
        private val nowMillis: Long = TEST_NOW_MILLIS,
    ) : TimeProvider {
        override fun currentTimeMillis(): Long = nowMillis
    }

    private class FakeRecentCurrentLocationSpotCacheRepository(
        var entry: RecentCurrentLocationSpotCacheEntry? = null,
    ) : RecentCurrentLocationSpotCacheRepository {
        var getCallCount = 0
            private set
        var saveCallCount = 0
            private set
        var clearCallCount = 0
            private set

        override suspend fun getRecentCurrentLocationSpots(): RecentCurrentLocationSpotCacheEntry? {
            getCallCount += 1
            return entry
        }

        override suspend fun saveRecentCurrentLocationSpots(entry: RecentCurrentLocationSpotCacheEntry) {
            saveCallCount += 1
            this.entry = entry
        }

        override suspend fun clearRecentCurrentLocationSpots() {
            clearCallCount += 1
            entry = null
        }
    }

    private class FakeCollectionSpotRepository(
        private val keywordSpots: List<CollectionSpot> = emptyList(),
        private val locationSpots: List<CollectionSpot> = emptyList(),
        private val keywordSearchThrowable: Throwable? = null,
        private val locationSearchThrowable: Throwable? = null,
        private val locationSearchResultProvider: (suspend () -> List<CollectionSpot>)? = null,
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
            keywordSearchThrowable?.let { throw it }
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
            locationSearchThrowable?.let { throw it }
            locationSearchResultProvider?.let { provider -> return provider() }
            return locationSpots
        }

        override suspend fun geocodeSpot(spot: CollectionSpot): CollectionSpot = spot
    }

    private fun collectionSpotFavorite(targetId: String): Favorite =
        Favorite(
            type = FavoriteTargetType.COLLECTION_SPOT,
            targetId = targetId,
            savedAtMillis = 1L,
        )

    private class FakeFavoriteRepository(
        initialFavorites: List<Favorite> = emptyList(),
    ) : FavoriteRepository {
        private val favorites = MutableStateFlow(initialFavorites)

        override fun observeFavorites(): Flow<List<Favorite>> = favorites

        override fun observeFavorite(
            type: FavoriteTargetType,
            targetId: String,
        ): Flow<Boolean> =
            favorites.map { items ->
                items.any { favorite -> favorite.type == type && favorite.targetId == targetId }
            }

        override suspend fun isFavorite(
            type: FavoriteTargetType,
            targetId: String,
        ): Boolean =
            favorites.value.any { favorite -> favorite.type == type && favorite.targetId == targetId }

        override suspend fun toggleFavorite(favorite: Favorite): Boolean {
            return if (isFavorite(favorite.type, favorite.targetId)) {
                removeFavorite(favorite.type, favorite.targetId)
                false
            } else {
                addFavorite(favorite)
                true
            }
        }

        override suspend fun addFavorite(favorite: Favorite) {
            favorites.value =
                favorites.value
                    .filterNot { it.type == favorite.type && it.targetId == favorite.targetId } + favorite
        }

        override suspend fun removeFavorite(
            type: FavoriteTargetType,
            targetId: String,
        ) {
            favorites.value =
                favorites.value.filterNot { it.type == type && it.targetId == targetId }
        }
    }

    private class FakeCollectionSpotFavoriteSnapshotRepository(
        initialSnapshots: List<CollectionSpotFavoriteSnapshot> = emptyList(),
    ) : CollectionSpotFavoriteSnapshotRepository {
        val snapshots = MutableStateFlow(initialSnapshots)

        override fun observeSnapshots(): Flow<List<CollectionSpotFavoriteSnapshot>> = snapshots

        override suspend fun getSnapshot(targetId: String): CollectionSpotFavoriteSnapshot? =
            snapshots.value.firstOrNull { snapshot -> snapshot.targetId == targetId }

        override suspend fun upsertSnapshot(snapshot: CollectionSpotFavoriteSnapshot) {
            snapshots.value =
                snapshots.value
                    .filterNot { it.targetId == snapshot.targetId } + snapshot
        }

        override suspend fun deleteSnapshot(targetId: String) {
            snapshots.value = snapshots.value.filterNot { it.targetId == targetId }
        }
    }

    private class FakeCollectionSpotFavoriteRepository(
        private val favoriteRepository: FakeFavoriteRepository,
        private val snapshotRepository: FakeCollectionSpotFavoriteSnapshotRepository,
    ) : CollectionSpotFavoriteRepository {
        override suspend fun toggleFavorite(spot: CollectionSpot): Boolean {
            val isFavorite =
                favoriteRepository.toggleFavorite(
                    Favorite(
                        type = FavoriteTargetType.COLLECTION_SPOT,
                        targetId = spot.id,
                        savedAtMillis = TEST_NOW_MILLIS,
                    ),
                )

            if (isFavorite) {
                snapshotRepository.upsertSnapshot(spot.toFavoriteSnapshot())
            } else {
                snapshotRepository.deleteSnapshot(spot.id)
            }

            return isFavorite
        }

        override suspend fun removeFavorite(targetId: String) {
            favoriteRepository.removeFavorite(FavoriteTargetType.COLLECTION_SPOT, targetId)
            snapshotRepository.deleteSnapshot(targetId)
        }
    }

    private companion object {
        const val TEST_NOW_MILLIS = 20 * 60 * 1_000L
    }
}
