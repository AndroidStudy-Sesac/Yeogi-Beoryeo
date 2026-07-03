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
class CollectionSpotMapSearchViewModelTest : CollectionSpotMapViewModelTestFixture() {
    @Test
    fun `지도 중심 검색은 전달된 카메라 중심 좌표로 수거 장소를 검색한다`() =
        runTest {
            val mapCenterCoordinate = Coordinate(latitude = 37.5701, longitude = 127.0012)
            val expectedSpots = listOf(sampleSpot("map-center", CollectionSpotType.RECYCLING_CENTER))
            val repository = FakeCollectionSpotRepository(
                locationSpots = expectedSpots,
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.onSearchKeywordChanged("문래동")
            viewModel.searchByMapCenter(mapCenterCoordinate)
            advanceUntilIdle()

            assertEquals(mapCenterCoordinate, repository.lastLocationCoordinate)
            assertEquals(500, repository.lastRadiusMeter)
            assertEquals(expectedSpots, viewModel.uiState.value.spots)
            assertEquals("문래동", viewModel.uiState.value.searchKeyword)
            assertEquals(MapSearchMode.MAP_CENTER, viewModel.uiState.value.searchMode)
            assertNull(viewModel.uiState.value.selectedSpot)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `지도 중심 검색 중 검색어를 입력하면 진행 중인 지도 중심 검색을 취소한다`() =
        runTest {
            val mapCenterCoordinate = Coordinate(latitude = 37.5701, longitude = 127.0012)
            val mapCenterSearchResult = CompletableDeferred<List<CollectionSpot>>()
            val repository = FakeCollectionSpotRepository(
                locationSearchResultProvider = { mapCenterSearchResult.await() },
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.searchByMapCenter(mapCenterCoordinate)
            advanceUntilIdle()

            assertEquals(true, viewModel.uiState.value.isLoading)
            assertEquals(MapSearchMode.MAP_CENTER, viewModel.uiState.value.searchMode)

            viewModel.onSearchKeywordChanged("문래동")
            mapCenterSearchResult.complete(
                listOf(sampleSpot("map-center", CollectionSpotType.RECYCLING_CENTER)),
            )
            advanceUntilIdle()

            assertEquals("문래동", viewModel.uiState.value.searchKeyword)
            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertEquals(false, viewModel.uiState.value.isLoading)
            assertEquals(false, viewModel.uiState.value.hasSearched)
            assertEquals(MapSearchMode.KEYWORD, viewModel.uiState.value.searchMode)
        }

    @Test
    fun `현재 위치 기준 검색과 지도 중심 기준 검색은 서로 다른 좌표를 사용한다`() =
        runTest {
            val currentCoordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881)
            val mapCenterCoordinate = Coordinate(latitude = 37.5701, longitude = 127.0012)
            val repository = FakeCollectionSpotRepository(
                locationSpots = listOf(sampleSpot("location", CollectionSpotType.STANDARD_BAG_STORE)),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(currentCoordinate),
            )

            viewModel.searchByCurrentLocation()
            advanceUntilIdle()
            assertEquals(currentCoordinate, repository.lastLocationCoordinate)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)

            viewModel.searchByMapCenter(mapCenterCoordinate)
            advanceUntilIdle()

            assertEquals(mapCenterCoordinate, repository.lastLocationCoordinate)
            assertEquals(MapSearchMode.MAP_CENTER, viewModel.uiState.value.searchMode)
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
            assertEquals(
                "잠시 후 다시 시도하거나 네트워크 연결을 확인해 주세요.",
                viewModel.uiState.value.errorMessage,
            )
            assertNull(viewModel.uiState.value.locationNotice)
            assertNull(viewModel.uiState.value.locationNoticeMessage)
        }

}
