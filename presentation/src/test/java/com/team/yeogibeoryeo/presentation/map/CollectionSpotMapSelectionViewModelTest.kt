package com.team.yeogibeoryeo.presentation.map

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.presentation.map.location.CurrentLocationResult
import com.team.yeogibeoryeo.presentation.map.model.FavoriteSpotMapMoveRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionSpotMapSelectionViewModelTest : CollectionSpotMapViewModelTestFixture() {
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
            assertEquals(
                listOf(recyclingCenterSpot).withDistanceFrom(Coordinate(latitude = 37.5666102, longitude = 126.9783881)),
                viewModel.uiState.value.spots,
            )
        }

    @Test
    fun `필터 결과만 비어 있으면 필터 빈 결과 상태로 구분하고 필터 해제 시 전체 결과를 표시한다`() =
        runTest {
            val standardBagSpot = sampleSpot("1", CollectionSpotType.STANDARD_BAG_STORE)
            val repository = FakeCollectionSpotRepository(
                locationSpots = listOf(standardBagSpot),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
            )

            viewModel.searchByCurrentLocation()
            viewModel.onSpotTypeClick(CollectionSpotType.FLUORESCENT_LAMP_BIN)

            assertEquals(setOf(CollectionSpotType.FLUORESCENT_LAMP_BIN), viewModel.uiState.value.selectedTypes)
            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertEquals(true, viewModel.uiState.value.isFilterResultEmpty)
            assertEquals(
                Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                viewModel.uiState.value.searchFocusCoordinate,
            )

            viewModel.onSearchKeywordChanged("문래동")

            assertEquals(true, viewModel.uiState.value.isFilterResultEmpty)
            assertEquals(
                Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                viewModel.uiState.value.searchFocusCoordinate,
            )

            viewModel.clearSpotTypeFilters()

            assertEquals(emptySet<CollectionSpotType>(), viewModel.uiState.value.selectedTypes)
            assertEquals(
                listOf(standardBagSpot).withDistanceFrom(Coordinate(latitude = 37.5666102, longitude = 126.9783881)),
                viewModel.uiState.value.spots,
            )
            assertEquals(
                Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                viewModel.uiState.value.searchFocusCoordinate,
            )
            assertFalse(viewModel.uiState.value.isFilterResultEmpty)
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
    fun `선택 상세를 닫으면 선택된 장소만 해제하고 검색 결과와 필터 상태는 유지한다`() =
        runTest {
            val selectedSpot = sampleSpot("1", CollectionSpotType.STANDARD_BAG_STORE)
            val filteredOutSpot = sampleSpot("2", CollectionSpotType.RECYCLING_CENTER)
            val repository = FakeCollectionSpotRepository(
                locationSpots = listOf(selectedSpot, filteredOutSpot),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
            )

            viewModel.searchByCurrentLocation()
            viewModel.onSpotTypeClick(CollectionSpotType.STANDARD_BAG_STORE)
            viewModel.onSpotClick(selectedSpot)

            viewModel.clearSelectedSpot()

            assertNull(viewModel.uiState.value.selectedSpot)
            assertEquals(setOf(CollectionSpotType.STANDARD_BAG_STORE), viewModel.uiState.value.selectedTypes)
            assertEquals(
                listOf(selectedSpot).withDistanceFrom(Coordinate(latitude = 37.5666102, longitude = 126.9783881)),
                viewModel.uiState.value.spots,
            )
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
            assertEquals(true, viewModel.uiState.value.hasSearched)
        }

    @Test
    fun `검색 결과 없이 선택된 즐겨찾기 장소 상세를 닫아도 앱이 깨지지 않고 선택만 해제한다`() =
        runTest {
            val request = FavoriteSpotMapMoveRequest(
                requestId = "request-1",
                targetId = "favorite-spot",
                name = "즐겨찾기 수거함",
                type = CollectionSpotType.STANDARD_BAG_STORE,
                address = "서울특별시 성동구 용답동",
                detailLocation = null,
                coordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881),
            )
            val viewModel = createViewModel(
                repository = FakeCollectionSpotRepository(),
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.showFavoriteSpot(request)
            viewModel.clearSelectedSpot()

            assertNull(viewModel.uiState.value.selectedSpot)
            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
        }

}
