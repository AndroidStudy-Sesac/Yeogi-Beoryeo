package com.team.yeogibeoryeo.presentation.map

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.model.MapRegionSearchCandidate
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
    fun `성동구 금호동 입력 시 금호동으로 보정해 키워드 검색을 요청한다`() =
        runTest {
            val expectedSpots = listOf(
                sampleSpot("geumho", CollectionSpotType.BATTERY_BIN)
                    .copy(address = "서울특별시 성동구 금호동")
            )
            val repository = FakeCollectionSpotRepository(
                keywordSpots = expectedSpots,
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.onSearchKeywordChanged("성동구 금호동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            assertEquals(listOf("금호동"), repository.keywords)
            assertEquals("성동구 금호동", viewModel.uiState.value.searchKeyword)
            assertEquals(expectedSpots, viewModel.uiState.value.spots)
            assertEquals(MapSearchMode.KEYWORD, viewModel.uiState.value.searchMode)
        }

    @Test
    fun `서울 성동구 금호동 입력 시 금호동으로 보정해 키워드 검색을 요청한다`() =
        runTest {
            val repository = FakeCollectionSpotRepository()
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.onSearchKeywordChanged("서울 성동구 금호동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            assertEquals(listOf("금호동"), repository.keywords)
            assertEquals("서울 성동구 금호동", viewModel.uiState.value.searchKeyword)
        }

    @Test
    fun `기존 동 단독 검색어는 그대로 키워드 검색을 요청한다`() =
        runTest {
            val repository = FakeCollectionSpotRepository()
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.onSearchKeywordChanged("금호동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            assertEquals(listOf("금호동"), repository.keywords)
        }

    @Test
    fun `도로명 주소 입력은 동 검색어로 보정하지 않고 원문으로 검색한다`() =
        runTest {
            val repository = FakeCollectionSpotRepository()
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.onSearchKeywordChanged("서울 성동구 독서당로 303-5")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            assertEquals(listOf("서울 성동구 독서당로 303-5"), repository.keywords)
        }

    @Test
    fun `검색 결과 주소가 입력 동명을 포함하지 않아도 결과를 유지한다`() =
        runTest {
            val roadAddressSpot = sampleSpot("road-address", CollectionSpotType.STANDARD_BAG_STORE)
                .copy(address = "서울특별시 성동구 독서당로 303-5")
            val repository = FakeCollectionSpotRepository(
                keywordSpots = listOf(roadAddressSpot),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.onSearchKeywordChanged("성동구 금호동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            assertEquals(listOf("금호동"), repository.keywords)
            assertEquals(listOf(roadAddressSpot), viewModel.uiState.value.spots)
    }

    @Test
    fun `명동 검색 결과에서 봉명동 주소는 제외한다`() =
        runTest {
            val myeongDongSpot = sampleSpot("myeongdong", CollectionSpotType.STANDARD_BAG_STORE)
                .copy(address = "서울특별시 중구 명동길 26 (명동)")
            val bongMyeongDongSpot = sampleSpot("bongmyeongdong", CollectionSpotType.STANDARD_BAG_STORE)
                .copy(address = "충청북도 청주시 흥덕구 봉명동 1순환로584번길 59")
            val repository = FakeCollectionSpotRepository(
                keywordSpots = listOf(myeongDongSpot, bongMyeongDongSpot),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.onSearchKeywordChanged("명동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            assertEquals(listOf("명동"), repository.keywords)
            assertEquals(listOf(myeongDongSpot), viewModel.uiState.value.spots)
        }

    @Test
    fun `동 단독 검색어에 여러 지역 후보가 있으면 검색 대신 후보 목록을 표시한다`() =
        runTest {
            val repository = FakeCollectionSpotRepository()
            val regionOptionsRepository = FakeMapRegionOptionsRepository(
                eupmyeondongCandidates = mapOf(
                    "명동" to listOf(
                        Region(sido = "서울특별시", sigungu = "중구", eupmyeondong = "명동"),
                        Region(sido = "충청북도", sigungu = "제천시", eupmyeondong = "명동"),
                    ),
                ),
                legalDongKeywords = mapOf(
                    "서울특별시|중구|명동|명동" to listOf("명동1가", "명동2가"),
                ),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
                regionOptionsRepository = regionOptionsRepository,
            )

            viewModel.onSearchKeywordChanged("명동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            assertEquals(emptyList<String>(), repository.keywords)
            assertEquals(
                listOf("서울특별시 중구 명동", "충청북도 제천시 명동"),
                viewModel.uiState.value.regionSearchCandidates.map { candidate -> candidate.displayName },
            )
            assertFalse(viewModel.uiState.value.hasSearched)
        }

    @Test
    fun `지역 후보를 선택하면 선택한 동 검색어로 검색을 요청한다`() =
        runTest {
            val seoulSpot = sampleSpot("seoul-myeongdong", CollectionSpotType.STANDARD_BAG_STORE)
                .copy(address = "서울특별시 중구 명동길 3")
            val jecheonSpot = sampleSpot("jecheon-myeongdong", CollectionSpotType.STANDARD_BAG_STORE)
                .copy(address = "충청북도 제천시 명동 1")
            val repository = FakeCollectionSpotRepository(
                keywordSpots = listOf(seoulSpot, jecheonSpot),
            )
            val regionOptionsRepository = FakeMapRegionOptionsRepository(
                eupmyeondongCandidates = mapOf(
                    "명동" to listOf(
                        Region(sido = "서울특별시", sigungu = "중구", eupmyeondong = "명동"),
                        Region(sido = "충청북도", sigungu = "제천시", eupmyeondong = "명동"),
                    ),
                ),
                legalDongKeywords = mapOf(
                    "서울특별시|중구|명동|명동" to listOf("명동1가", "명동2가"),
                ),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
                regionOptionsRepository = regionOptionsRepository,
            )

            viewModel.onSearchKeywordChanged("명동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            val candidate = viewModel.uiState.value.regionSearchCandidates.first()
            viewModel.onRegionSearchCandidateClick(candidate)
            advanceUntilIdle()

            assertEquals(listOf("명동", "명동1가", "명동2가"), repository.keywords)
            assertEquals(emptyList<MapRegionSearchCandidate>(), viewModel.uiState.value.regionSearchCandidates)
            assertEquals(listOf(seoulSpot), viewModel.uiState.value.spots)
            assertEquals("명동", viewModel.uiState.value.searchKeyword)
        }

    @Test
    fun `지역 범위가 포함된 동 검색어는 후보를 좁혀 바로 검색한다`() =
        runTest {
            val expectedSpot = sampleSpot("seoul-myeongdong", CollectionSpotType.STANDARD_BAG_STORE)
                .copy(address = "서울특별시 중구 명동길 3")
            val repository = FakeCollectionSpotRepository(
                keywordSpots = listOf(expectedSpot),
            )
            val regionOptionsRepository = FakeMapRegionOptionsRepository(
                eupmyeondongCandidates = mapOf(
                    "명동" to listOf(
                        Region(sido = "서울특별시", sigungu = "중구", eupmyeondong = "명동"),
                        Region(sido = "충청북도", sigungu = "제천시", eupmyeondong = "명동"),
                    ),
                ),
                legalDongKeywords = mapOf(
                    "서울특별시|중구|명동|명동" to listOf("명동1가", "명동2가"),
                ),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
                regionOptionsRepository = regionOptionsRepository,
            )

            viewModel.onSearchKeywordChanged("서울 중구 명동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            assertEquals(listOf("명동", "명동1가", "명동2가"), repository.keywords)
            assertEquals(emptyList<MapRegionSearchCandidate>(), viewModel.uiState.value.regionSearchCandidates)
            assertEquals(expectedSpot, viewModel.uiState.value.spots.single())
        }

    @Test
    fun `검색어를 수정하면 표시 중인 지역 후보 목록을 닫는다`() =
        runTest {
            val repository = FakeCollectionSpotRepository()
            val regionOptionsRepository = FakeMapRegionOptionsRepository(
                eupmyeondongCandidates = mapOf(
                    "명동" to listOf(
                        Region(sido = "서울특별시", sigungu = "중구", eupmyeondong = "명동"),
                        Region(sido = "충청북도", sigungu = "제천시", eupmyeondong = "명동"),
                    ),
                ),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
                regionOptionsRepository = regionOptionsRepository,
            )

            viewModel.onSearchKeywordChanged("명동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            viewModel.onSearchKeywordChanged("명동1가")

            assertEquals(emptyList<MapRegionSearchCandidate>(), viewModel.uiState.value.regionSearchCandidates)
            assertEquals("명동1가", viewModel.uiState.value.searchKeyword)
        }

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
                MapLocationNotices.SpotSearchFailureMessageResId,
                viewModel.uiState.value.errorMessageResId,
            )
            assertNull(viewModel.uiState.value.locationNotice)
        }

}
