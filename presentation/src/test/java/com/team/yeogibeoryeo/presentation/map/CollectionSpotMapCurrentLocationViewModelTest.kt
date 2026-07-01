package com.team.yeogibeoryeo.presentation.map

import com.team.yeogibeoryeo.domain.favorite.model.CollectionSpotFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheEntry
import com.team.yeogibeoryeo.presentation.map.location.CurrentLocationProvider
import com.team.yeogibeoryeo.presentation.map.location.CurrentLocationResult
import com.team.yeogibeoryeo.presentation.map.model.FavoriteSpotMapMoveRequest
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
            assertEquals(expectedSpots.withDistanceFrom(currentCoordinate), viewModel.uiState.value.spots)
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

            assertEquals("위치 서비스가 꺼져 있습니다.", viewModel.uiState.value.locationNotice?.title)

            currentLocationResult = CurrentLocationResult.Found(currentCoordinate)
            viewModel.searchByCurrentLocation()
            advanceUntilIdle()

            assertEquals(listOf(locationSpot).withDistanceFrom(currentCoordinate), viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
            assertNull(viewModel.uiState.value.locationNotice)
            assertNull(viewModel.uiState.value.locationNoticeMessage)
            assertNull(viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `키워드 검색 후 현재 위치 검색을 실행하면 검색어를 유지하고 현재 위치 결과를 반영한다`() =
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

            assertEquals("용답동", viewModel.uiState.value.searchKeyword)
            assertEquals(
                listOf(locationSpot).withDistanceFrom(Coordinate(latitude = 37.5666102, longitude = 126.9783881)),
                viewModel.uiState.value.spots,
            )
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
            assertFalse(viewModel.uiState.value.isLoading)
        }

}
