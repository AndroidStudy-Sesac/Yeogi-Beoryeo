package com.team.yeogibeoryeo.presentation.map

import com.team.yeogibeoryeo.domain.favorite.model.CollectionSpotFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.presentation.map.location.CurrentLocationResult
import com.team.yeogibeoryeo.presentation.map.model.FavoriteSpotMapMoveRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionSpotMapFavoriteViewModelTest : CollectionSpotMapViewModelTestFixture() {
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
            val refreshResult = CompletableDeferred<List<CollectionSpot>>()
            val viewModel = createViewModel(
                repository = FakeCollectionSpotRepository(
                    locationSearchResultProvider = {
                        refreshResult.await()
                    },
                ),
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
            locationResult.complete(CurrentLocationResult.Found(DEFAULT_CURRENT_COORDINATE))
            runCurrent()

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

}
