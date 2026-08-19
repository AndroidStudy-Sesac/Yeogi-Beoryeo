package com.team.yeogibeoryeo.presentation.map

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotSearchResult
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.model.MapRegionSearchCandidate
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.map.location.CurrentLocationResult
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `세부 키워드가 있는 지역 후보를 선택하면 세부 지역 선택지를 표시한다`() =
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

            assertEquals(emptyList<String>(), repository.keywords)
            assertEquals(candidate, viewModel.uiState.value.regionDetailSearchCandidate)
            assertEquals(
                listOf("서울특별시 중구 명동", "충청북도 제천시 명동"),
                viewModel.uiState.value.regionSearchCandidates.map { it.displayName },
            )
            assertFalse(viewModel.uiState.value.hasSearched)
        }

    @Test
    fun `세부 지역 선택지에서 전체 보기를 누르면 기존 후보 키워드 전체로 검색한다`() =
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
            viewModel.onRegionDetailSearchAllClick()
            advanceUntilIdle()

            assertEquals(listOf("명동", "명동1가", "명동2가"), repository.keywords)
            assertEquals(emptyList<MapRegionSearchCandidate>(), viewModel.uiState.value.regionSearchCandidates)
            assertNull(viewModel.uiState.value.regionDetailSearchCandidate)
            assertEquals(listOf(seoulSpot), viewModel.uiState.value.spots)
            assertEquals("명동", viewModel.uiState.value.searchKeyword)
        }

    @Test
    fun `세부 지역 선택지에서 세부 키워드를 누르면 해당 키워드만 검색한다`() =
        runTest {
            val detailSpot = sampleSpot("seoul-myeongdong-1ga", CollectionSpotType.STANDARD_BAG_STORE)
                .copy(address = "서울특별시 중구 명동1가")
            val repository = FakeCollectionSpotRepository(
                keywordSpots = listOf(detailSpot),
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
            viewModel.onRegionDetailSearchKeywordClick("명동1가")
            advanceUntilIdle()

            assertEquals(listOf("명동1가"), repository.keywords)
            assertEquals(emptyList<MapRegionSearchCandidate>(), viewModel.uiState.value.regionSearchCandidates)
            assertNull(viewModel.uiState.value.regionDetailSearchCandidate)
            assertEquals(listOf(detailSpot), viewModel.uiState.value.spots)
            assertEquals("명동1가", viewModel.uiState.value.searchKeyword)
        }

    @Test
    fun `지역 후보 선택 후 다건 검색 결과를 선택 지역 필터와 함께 표시한다`() =
        runTest {
            val selectedRegionSpot = sampleSpot("gwangju-geumho", CollectionSpotType.STANDARD_BAG_STORE)
                .copy(address = "광주광역시 서구 금호동")
            val selectedRegionRoadAddressSpot = sampleSpot("gwangju-geumho-road", CollectionSpotType.BATTERY_BIN)
                .copy(address = "광주광역시 서구 운천로 10")
            val otherRegionSpot = sampleSpot("seoul-geumho", CollectionSpotType.STANDARD_BAG_STORE)
                .copy(address = "서울특별시 성동구 금호동")
            val repository = FakeCollectionSpotRepository(
                keywordSpots = listOf(
                    selectedRegionSpot,
                    selectedRegionRoadAddressSpot,
                    otherRegionSpot,
                ),
            )
            val regionOptionsRepository = FakeMapRegionOptionsRepository(
                eupmyeondongCandidates = mapOf(
                    "금호동" to listOf(
                        Region(sido = "서울특별시", sigungu = "성동구", eupmyeondong = "금호동"),
                        Region(sido = "광주광역시", sigungu = "서구", eupmyeondong = "금호동"),
                    ),
                ),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
                regionOptionsRepository = regionOptionsRepository,
            )

            viewModel.onSearchKeywordChanged("금호동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            val candidate = viewModel.uiState.value.regionSearchCandidates
                .first { it.displayName == "광주광역시 서구 금호동" }
            viewModel.onRegionSearchCandidateClick(candidate)
            advanceUntilIdle()

            assertEquals(listOf("금호동"), repository.keywords)
            assertEquals(
                listOf(selectedRegionSpot, selectedRegionRoadAddressSpot),
                viewModel.uiState.value.spots,
            )
        }

    @Test
    fun `지역 범위가 포함된 동 검색어도 세부 키워드가 있으면 세부 지역 선택지를 표시한다`() =
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

            assertEquals(emptyList<String>(), repository.keywords)
            assertEquals(emptyList<MapRegionSearchCandidate>(), viewModel.uiState.value.regionSearchCandidates)
            assertEquals(
                "서울특별시 중구 명동",
                viewModel.uiState.value.regionDetailSearchCandidate?.displayName,
            )

            viewModel.onRegionDetailSearchAllClick()
            advanceUntilIdle()

            assertEquals(listOf("명동", "명동1가", "명동2가"), repository.keywords)
            assertNull(viewModel.uiState.value.regionDetailSearchCandidate)
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
    fun `검색어를 수정하면 표시 중인 세부 지역 선택지를 닫는다`() =
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

            val candidate = viewModel.uiState.value.regionSearchCandidates.first()
            viewModel.onRegionSearchCandidateClick(candidate)

            viewModel.onSearchKeywordChanged("명동1가")

            assertEquals(emptyList<MapRegionSearchCandidate>(), viewModel.uiState.value.regionSearchCandidates)
            assertNull(viewModel.uiState.value.regionDetailSearchCandidate)
            assertEquals("명동1가", viewModel.uiState.value.searchKeyword)
        }

    @Test
    fun `세부 지역 선택지에서 뒤로가면 이전 지역 후보 목록으로 돌아간다`() =
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

            val candidate = viewModel.uiState.value.regionSearchCandidates.first()
            viewModel.onRegionSearchCandidateClick(candidate)
            viewModel.onRegionDetailSearchBack()

            assertNull(viewModel.uiState.value.regionDetailSearchCandidate)
            assertEquals(
                listOf("서울특별시 중구 명동", "충청북도 제천시 명동"),
                viewModel.uiState.value.regionSearchCandidates.map { it.displayName },
            )
            assertEquals(emptyList<String>(), repository.keywords)
        }

    @Test
    fun `지역 후보 목록에서 뒤로가면 검색어를 비우고 기본 위치 안내 상태로 돌아간다`() =
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

            val candidate = viewModel.uiState.value.regionSearchCandidates.first()
            viewModel.onRegionSearchCandidateClick(candidate)
            viewModel.onRegionDetailSearchBack()
            viewModel.onRegionSearchBack()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("", state.searchKeyword)
            assertEquals(emptyList<MapRegionSearchCandidate>(), state.regionSearchCandidates)
            assertNull(state.regionDetailSearchCandidate)
            assertEquals(emptyList<CollectionSpot>(), state.spots)
            assertFalse(state.isLoading)
            assertFalse(state.hasSearched)
            assertNull(state.errorMessageResId)
            assertEquals(MapLocationNotices.CurrentLocationUnavailable, state.locationNotice)
            assertEquals(emptyList<String>(), repository.keywords)
        }

    @Test
    fun `지역 후보 목록에서 뒤로가면 지도 진입 현재 위치 검색을 다시 실행한다`() =
        runTest {
            val currentCoordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881)
            val locationSpot = sampleSpot("location", CollectionSpotType.STANDARD_BAG_STORE)
            val repository = FakeCollectionSpotRepository(
                locationSpots = listOf(locationSpot),
            )
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
                currentLocationResult = CurrentLocationResult.Found(currentCoordinate),
                hasFineLocationPermission = true,
                regionOptionsRepository = regionOptionsRepository,
            )

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted()
            advanceUntilIdle()
            viewModel.onSearchKeywordChanged("명동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            val candidate = viewModel.uiState.value.regionSearchCandidates.first()
            viewModel.onRegionSearchCandidateClick(candidate)
            viewModel.onRegionSearchBack()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("", state.searchKeyword)
            assertEquals(emptyList<MapRegionSearchCandidate>(), state.regionSearchCandidates)
            assertNull(state.regionDetailSearchCandidate)
            assertEquals(listOf(locationSpot).withDistanceFrom(currentCoordinate), state.spots)
            assertEquals(MapSearchMode.CURRENT_LOCATION, state.searchMode)
            assertTrue(state.shouldKeepCurrentLocationSheetHiddenAfterRegionBack)
            assertEquals(2, repository.locationSearchCallCount)
        }

    @Test
    fun `지역 후보 뒤로가기 후 현재 위치 응답 대기 중에는 숨김 상태를 유지한다`() =
        runTest {
            val currentCoordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881)
            val locationSpot = sampleSpot("location", CollectionSpotType.STANDARD_BAG_STORE)
            val currentLocationResult = CompletableDeferred<CurrentLocationResult>()
            val repository = FakeCollectionSpotRepository(
                locationSpots = listOf(locationSpot),
            )
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
                currentLocationProvider = FakeCurrentLocationProvider {
                    currentLocationResult.await()
                },
                hasFineLocationPermission = true,
                regionOptionsRepository = regionOptionsRepository,
            )

            viewModel.onSearchKeywordChanged("명동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            val candidate = viewModel.uiState.value.regionSearchCandidates.first()
            viewModel.onRegionSearchCandidateClick(candidate)
            viewModel.onRegionSearchBack()
            runCurrent()

            val waitingState = viewModel.uiState.value
            assertEquals("", waitingState.searchKeyword)
            assertEquals(emptyList<CollectionSpot>(), waitingState.spots)
            assertTrue(waitingState.hasSearched)
            assertFalse(waitingState.isLoading)
            assertFalse(waitingState.isFilterResultEmpty)
            assertEquals(MapSearchMode.CURRENT_LOCATION, waitingState.searchMode)
            assertTrue(waitingState.shouldKeepCurrentLocationSheetHiddenAfterRegionBack)

            currentLocationResult.complete(CurrentLocationResult.Found(currentCoordinate))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(listOf(locationSpot).withDistanceFrom(currentCoordinate), state.spots)
            assertTrue(state.shouldKeepCurrentLocationSheetHiddenAfterRegionBack)
        }

    @Test
    fun `지역 후보 뒤로가기 후 현재 위치 필터 결과가 없으면 빈 결과 노출을 허용한다`() =
        runTest {
            val currentCoordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881)
            val locationSpot = sampleSpot("location", CollectionSpotType.STANDARD_BAG_STORE)
            val repository = FakeCollectionSpotRepository(
                locationSpots = listOf(locationSpot),
            )
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
                currentLocationResult = CurrentLocationResult.Found(currentCoordinate),
                hasFineLocationPermission = true,
                regionOptionsRepository = regionOptionsRepository,
            )

            viewModel.onSpotTypeClick(CollectionSpotType.BATTERY_BIN)
            viewModel.onSearchKeywordChanged("명동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            val candidate = viewModel.uiState.value.regionSearchCandidates.first()
            viewModel.onRegionSearchCandidateClick(candidate)
            viewModel.onRegionSearchBack()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(emptyList<CollectionSpot>(), state.spots)
            assertTrue(state.hasSearched)
            assertTrue(state.isFilterResultEmpty)
            assertEquals(MapSearchMode.CURRENT_LOCATION, state.searchMode)
            assertFalse(state.shouldKeepCurrentLocationSheetHiddenAfterRegionBack)
        }

    @Test
    fun `지역 후보 뒤로가기 후 마커 상세에서 즐겨찾기를 눌러도 상세 선택을 유지한다`() =
        runTest {
            val currentCoordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881)
            val locationSpot = sampleSpot("location", CollectionSpotType.STANDARD_BAG_STORE)
            val repository = FakeCollectionSpotRepository(
                locationSpots = listOf(locationSpot),
            )
            val favoriteRepository = FakeFavoriteRepository()
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
                currentLocationResult = CurrentLocationResult.Found(currentCoordinate),
                hasFineLocationPermission = true,
                favoriteRepository = favoriteRepository,
                regionOptionsRepository = regionOptionsRepository,
            )

            viewModel.onSearchKeywordChanged("명동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            val candidate = viewModel.uiState.value.regionSearchCandidates.first()
            viewModel.onRegionSearchCandidateClick(candidate)
            viewModel.onRegionSearchBack()
            advanceUntilIdle()

            val spot = viewModel.uiState.value.spots.first()
            assertTrue(viewModel.uiState.value.shouldKeepCurrentLocationSheetHiddenAfterRegionBack)

            viewModel.onSpotClick(spot)
            assertFalse(viewModel.uiState.value.shouldKeepCurrentLocationSheetHiddenAfterRegionBack)

            viewModel.onSpotFavoriteClick(spot)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(spot.id, state.selectedSpot?.id)
            assertTrue(state.selectedSpot?.isBookmarked == true)
            assertFalse(state.shouldKeepCurrentLocationSheetHiddenAfterRegionBack)
        }

    @Test
    fun `지역 후보 뒤로가기 후 지연된 현재 위치 갱신 중 마커를 선택해도 상세 선택을 유지한다`() =
        runTest {
            val cachedSpot = sampleSpot("location", CollectionSpotType.STANDARD_BAG_STORE)
                .copy(name = "캐시 수거 장소")
            val refreshedSpot = sampleSpot("location", CollectionSpotType.STANDARD_BAG_STORE)
                .copy(name = "갱신된 수거 장소")
            val refreshResult = CompletableDeferred<List<CollectionSpot>>()
            val repository = FakeCollectionSpotRepository(
                locationSearchResultProvider = {
                    refreshResult.await()
                },
            )
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
                currentLocationResult = CurrentLocationResult.Found(DEFAULT_CURRENT_COORDINATE),
                hasFineLocationPermission = true,
                recentCurrentLocationSpotCacheRepository = FakeRecentCurrentLocationSpotCacheRepository(
                    entry = freshCacheEntry(listOf(cachedSpot)),
                ),
                regionOptionsRepository = regionOptionsRepository,
            )

            viewModel.onSearchKeywordChanged("명동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            val candidate = viewModel.uiState.value.regionSearchCandidates.first()
            viewModel.onRegionSearchCandidateClick(candidate)
            viewModel.onRegionSearchBack()
            runCurrent()

            val cachedResultSpot = viewModel.uiState.value.spots.first()
            assertEquals(cachedSpot.name, cachedResultSpot.name)
            assertTrue(viewModel.uiState.value.shouldKeepCurrentLocationSheetHiddenAfterRegionBack)

            viewModel.onSpotClick(cachedResultSpot)
            assertEquals(cachedSpot.name, viewModel.uiState.value.selectedSpot?.name)

            refreshResult.complete(listOf(refreshedSpot))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(refreshedSpot.name, state.spots.first().name)
            assertEquals(refreshedSpot.id, state.selectedSpot?.id)
            assertEquals(refreshedSpot.name, state.selectedSpot?.name)
        }

    @Test
    fun `단일 후보의 세부 지역 선택지에서 뒤로가면 검색 전 상태로 돌아간다`() =
        runTest {
            val repository = FakeCollectionSpotRepository()
            val regionOptionsRepository = FakeMapRegionOptionsRepository(
                eupmyeondongCandidates = mapOf(
                    "명동" to listOf(
                        Region(sido = "서울특별시", sigungu = "중구", eupmyeondong = "명동"),
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

            viewModel.onRegionDetailSearchBack()

            assertNull(viewModel.uiState.value.regionDetailSearchCandidate)
            assertEquals(emptyList<MapRegionSearchCandidate>(), viewModel.uiState.value.regionSearchCandidates)
            assertFalse(viewModel.uiState.value.hasSearched)
            assertEquals(emptyList<String>(), repository.keywords)
        }

    @Test
    fun `지역 후보 조회 중 검색어를 수정하면 이전 후보 조회를 취소한다`() =
        runTest {
            val repository = FakeCollectionSpotRepository()
            val candidateSearchResult = CompletableDeferred<List<Region>>()
            val regionOptionsRepository = FakeMapRegionOptionsRepository(
                eupmyeondongCandidateProvider = { keyword ->
                    if (keyword == "명동") {
                        candidateSearchResult.await()
                    } else {
                        emptyList()
                    }
                },
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
                regionOptionsRepository = regionOptionsRepository,
            )

            viewModel.onSearchKeywordChanged("명동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            assertEquals(true, viewModel.uiState.value.isLoading)
            assertEquals(MapSearchMode.KEYWORD, viewModel.uiState.value.searchMode)

            viewModel.onSearchKeywordChanged("명동1가")
            candidateSearchResult.complete(
                listOf(
                    Region(sido = "서울특별시", sigungu = "중구", eupmyeondong = "명동"),
                    Region(sido = "충청북도", sigungu = "제천시", eupmyeondong = "명동"),
                ),
            )
            advanceUntilIdle()

            assertEquals("명동1가", viewModel.uiState.value.searchKeyword)
            assertEquals(emptyList<MapRegionSearchCandidate>(), viewModel.uiState.value.regionSearchCandidates)
            assertEquals(false, viewModel.uiState.value.isLoading)
            assertEquals(false, viewModel.uiState.value.hasSearched)
        }

    @Test
    fun `동일 검색어 후보 조회가 진행 중이면 중복 제출을 무시한다`() =
        runTest {
            val repository = FakeCollectionSpotRepository()
            val candidateSearchResult = CompletableDeferred<List<Region>>()
            val regionOptionsRepository = FakeMapRegionOptionsRepository(
                eupmyeondongCandidateProvider = { keyword ->
                    if (keyword == "명동") {
                        candidateSearchResult.await()
                    } else {
                        emptyList()
                    }
                },
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
                regionOptionsRepository = regionOptionsRepository,
            )

            viewModel.onSearchKeywordChanged("명동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            viewModel.searchByKeyword()

            assertEquals(listOf("명동"), regionOptionsRepository.eupmyeondongKeywords)

            candidateSearchResult.complete(emptyList())
            advanceUntilIdle()

            assertEquals(listOf("명동"), repository.keywords)
        }

    @Test
    fun `앞뒤 공백만 다른 동일 검색어로 수정 후 재제출해도 진행 중인 검색을 유지한다`() =
        runTest {
            val repository = FakeCollectionSpotRepository()
            val candidateSearchResult = CompletableDeferred<List<Region>>()
            val regionOptionsRepository = FakeMapRegionOptionsRepository(
                eupmyeondongCandidateProvider = { keyword ->
                    if (keyword == "명동") {
                        candidateSearchResult.await()
                    } else {
                        emptyList()
                    }
                },
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
                regionOptionsRepository = regionOptionsRepository,
            )

            viewModel.onSearchKeywordChanged(" 명동 ")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            viewModel.onSearchKeywordChanged("명동")
            viewModel.searchByKeyword()

            assertEquals(listOf("명동"), regionOptionsRepository.eupmyeondongKeywords)

            candidateSearchResult.complete(emptyList())
            advanceUntilIdle()

            assertEquals(listOf("명동"), repository.keywords)
        }

    @Test
    fun `검색 중 다른 검색어를 제출하면 기존 후보 조회를 취소하고 새 검색을 실행한다`() =
        runTest {
            val repository = FakeCollectionSpotRepository()
            val candidateSearchResult = CompletableDeferred<List<Region>>()
            val regionOptionsRepository = FakeMapRegionOptionsRepository(
                eupmyeondongCandidateProvider = { keyword ->
                    if (keyword == "명동") {
                        candidateSearchResult.await()
                    } else {
                        emptyList()
                    }
                },
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
            viewModel.searchByKeyword()
            candidateSearchResult.complete(
                listOf(Region(sido = "서울특별시", sigungu = "중구", eupmyeondong = "명동")),
            )
            advanceUntilIdle()

            assertEquals(listOf("명동", "명동1가"), regionOptionsRepository.eupmyeondongKeywords)
            assertEquals(listOf("명동1가"), repository.keywords)
            assertEquals("명동1가", viewModel.uiState.value.searchKeyword)
        }

    @Test
    fun `취소된 동일 검색어 job cleanup이 늦어도 새 검색 중복 제출 guard를 유지한다`() =
        runTest {
            val repository = FakeCollectionSpotRepository()
            val firstCleanupStarted = CompletableDeferred<Unit>()
            val firstCleanupCanFinish = CompletableDeferred<Unit>()
            val secondCandidateSearchResult = CompletableDeferred<List<Region>>()
            var myeongdongSearchCount = 0
            val regionOptionsRepository = FakeMapRegionOptionsRepository(
                eupmyeondongCandidateProvider = { keyword ->
                    if (keyword != "명동") return@FakeMapRegionOptionsRepository emptyList()

                    myeongdongSearchCount += 1
                    if (myeongdongSearchCount == 1) {
                        try {
                            awaitCancellation()
                        } finally {
                            withContext(NonCancellable) {
                                firstCleanupStarted.complete(Unit)
                                firstCleanupCanFinish.await()
                            }
                        }
                    }

                    secondCandidateSearchResult.await()
                },
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
                regionOptionsRepository = regionOptionsRepository,
            )

            viewModel.onSearchKeywordChanged("명동")
            viewModel.searchByKeyword()
            runCurrent()

            viewModel.onSearchKeywordChanged("명동1가")
            firstCleanupStarted.await()

            viewModel.onSearchKeywordChanged("명동")
            viewModel.searchByKeyword()
            runCurrent()

            firstCleanupCanFinish.complete(Unit)
            runCurrent()

            viewModel.searchByKeyword()
            runCurrent()

            assertEquals(listOf("명동", "명동"), regionOptionsRepository.eupmyeondongKeywords)

            secondCandidateSearchResult.complete(emptyList())
            advanceUntilIdle()

            assertEquals(listOf("명동"), repository.keywords)
        }

    @Test
    fun `검색 완료 후 같은 검색어 재제출은 새로고침처럼 다시 실행한다`() =
        runTest {
            val repository = FakeCollectionSpotRepository()
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.onSearchKeywordChanged("용답동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            viewModel.searchByKeyword()
            advanceUntilIdle()

            assertEquals(listOf("용답동", "용답동"), repository.keywords)
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
            assertEquals(mapCenterCoordinate, viewModel.uiState.value.searchFocusCoordinate)
            assertEquals("", viewModel.uiState.value.searchKeyword)
            assertEquals(MapSearchMode.MAP_CENTER, viewModel.uiState.value.searchMode)
            assertNull(viewModel.uiState.value.selectedSpot)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `지도 중심 검색 결과가 없어도 검색 기준 좌표와 검색 모드를 유지한다`() =
        runTest {
            val mapCenterCoordinate = Coordinate(latitude = 37.5701, longitude = 127.0012)
            val repository = FakeCollectionSpotRepository(
                locationSpots = emptyList(),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.searchByMapCenter(mapCenterCoordinate)
            advanceUntilIdle()

            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertEquals(mapCenterCoordinate, viewModel.uiState.value.searchFocusCoordinate)
            assertEquals(MapSearchMode.MAP_CENTER, viewModel.uiState.value.searchMode)
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
            assertNull(viewModel.uiState.value.searchFocusCoordinate)
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
            assertEquals(mapCenterCoordinate, viewModel.uiState.value.searchFocusCoordinate)
            assertEquals(MapSearchMode.MAP_CENTER, viewModel.uiState.value.searchMode)
        }

    @Test
    fun `키워드 검색 네트워크 실패 시 원문 예외 대신 네트워크 안내 상태를 표시한다`() =
        runTest {
            val repository = FakeCollectionSpotRepository(
                keywordSearchThrowable = UnknownHostException("apis.data.go.kr"),
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
            assertEquals(MapSearchFailureReason.Network, viewModel.uiState.value.searchFailure?.reason)
            assertEquals(
                R.string.map_search_network_failure_message,
                viewModel.uiState.value.searchFailure?.messageResId,
            )
            assertNull(viewModel.uiState.value.errorMessageResId)
            assertNull(viewModel.uiState.value.locationNotice)
        }

    @Test
    fun `키워드 검색 API 실패 시 외부 서비스 안내 상태를 표시한다`() =
        runTest {
            val repository = FakeCollectionSpotRepository(
                keywordSearchThrowable = IllegalStateException("수거 장소 API 오류(99): SERVICE ERROR"),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.onSearchKeywordChanged("용답동")
            viewModel.searchByKeyword()

            assertEquals(
                MapSearchFailureReason.ExternalService,
                viewModel.uiState.value.searchFailure?.reason,
            )
            assertEquals(
                R.string.map_search_external_service_failure_message,
                viewModel.uiState.value.searchFailure?.messageResId,
            )
            assertNull(viewModel.uiState.value.errorMessageResId)
        }

    @Test
    fun `키워드 검색 API 서비스 키 오류는 외부 서비스 일시 장애로 분류하지 않는다`() =
        runTest {
            val repository = FakeCollectionSpotRepository(
                keywordSearchThrowable = IllegalStateException(
                    "수거 장소 API 오류(30): SERVICE_KEY_IS_NOT_REGISTERED_ERROR",
                ),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.onSearchKeywordChanged("용답동")
            viewModel.searchByKeyword()

            assertEquals(
                MapSearchFailureReason.Unknown,
                viewModel.uiState.value.searchFailure?.reason,
            )
            assertEquals(
                R.string.map_search_unknown_failure_message,
                viewModel.uiState.value.searchFailure?.messageResId,
            )
            assertNull(viewModel.uiState.value.errorMessageResId)
        }

    @Test
    fun `키워드 검색 실패 후 검색어를 수정하면 실패 검색 완료 상태를 초기화한다`() =
        runTest {
            val repository = FakeCollectionSpotRepository(
                keywordSearchThrowable = UnknownHostException("apis.data.go.kr"),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.onSearchKeywordChanged("용답동")
            viewModel.searchByKeyword()

            assertEquals(true, viewModel.uiState.value.hasSearched)
            assertEquals(MapSearchFailureReason.Network, viewModel.uiState.value.searchFailure?.reason)
            assertNull(viewModel.uiState.value.errorMessageResId)

            viewModel.onSearchKeywordChanged("용답동1")

            assertEquals("용답동1", viewModel.uiState.value.searchKeyword)
            assertEquals(false, viewModel.uiState.value.hasSearched)
            assertNull(viewModel.uiState.value.searchFailure)
            assertNull(viewModel.uiState.value.errorMessageResId)
        }

    @Test
    fun `검색 요청 성공 후 결과가 0개이면 실패가 아닌 빈 결과 상태를 유지한다`() =
        runTest {
            val repository = FakeCollectionSpotRepository(keywordSpots = emptyList())
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.onSearchKeywordChanged("마포구")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            assertEquals(listOf("마포구"), repository.keywords)
            assertTrue(viewModel.uiState.value.hasSearched)
            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertNull(viewModel.uiState.value.searchFailure)
            assertNull(viewModel.uiState.value.errorMessageResId)
        }

    @Test
    fun `검색 실패 후 다시 시도하면 마지막 키워드 조건으로 재검색한다`() =
        runTest {
            val expectedSpots = listOf(sampleSpot("retry", CollectionSpotType.RECYCLING_CENTER))
            var shouldFail = true
            val repository = FakeCollectionSpotRepository(
                keywordSearchResultProvider = {
                    if (shouldFail) throw SocketTimeoutException("timeout")
                    CollectionSpotSearchResult(spots = expectedSpots)
                },
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.onSearchKeywordChanged("양평읍")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            assertEquals(
                MapSearchFailureReason.ExternalService,
                viewModel.uiState.value.searchFailure?.reason,
            )

            shouldFail = false
            viewModel.retrySpotSearch()
            advanceUntilIdle()

            assertEquals(listOf("양평읍", "양평읍"), repository.keywords)
            assertEquals(expectedSpots, viewModel.uiState.value.spots)
            assertNull(viewModel.uiState.value.searchFailure)
            assertNull(viewModel.uiState.value.errorMessageResId)
        }

    @Test
    fun `현재 위치 검색 실패 후 다시 시도하면 현재 위치 조건으로 재검색한다`() =
        runTest {
            val currentCoordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881)
            val expectedSpots = listOf(
                sampleSpot("current-location-retry", CollectionSpotType.BATTERY_BIN),
            )
            var shouldFail = true
            val repository = FakeCollectionSpotRepository(
                locationSearchResultProvider = {
                    if (shouldFail) throw UnknownHostException("apis.data.go.kr")
                    expectedSpots
                },
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(currentCoordinate),
            )

            viewModel.searchByCurrentLocation()
            advanceUntilIdle()

            assertEquals(MapSearchFailureReason.Network, viewModel.uiState.value.searchFailure?.reason)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)

            shouldFail = false
            viewModel.retrySpotSearch()
            advanceUntilIdle()

            assertEquals(2, repository.locationSearchCallCount)
            assertEquals(currentCoordinate, repository.lastLocationCoordinate)
            assertEquals(expectedSpots.withDistanceFrom(currentCoordinate), viewModel.uiState.value.spots)
            assertEquals(currentCoordinate, viewModel.uiState.value.searchFocusCoordinate)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
            assertNull(viewModel.uiState.value.searchFailure)
            assertNull(viewModel.uiState.value.errorMessageResId)
        }

    @Test
    fun `지도 중심 검색 실패 후 다시 시도하면 마지막 지도 중심 좌표로 재검색한다`() =
        runTest {
            val mapCenterCoordinate = Coordinate(latitude = 37.5701, longitude = 127.0012)
            val expectedSpots = listOf(
                sampleSpot("map-center-retry", CollectionSpotType.RECYCLING_CENTER),
            )
            var shouldFail = true
            val repository = FakeCollectionSpotRepository(
                locationSearchResultProvider = {
                    if (shouldFail) throw SocketTimeoutException("timeout")
                    expectedSpots
                },
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.searchByMapCenter(mapCenterCoordinate)
            advanceUntilIdle()

            assertEquals(
                MapSearchFailureReason.ExternalService,
                viewModel.uiState.value.searchFailure?.reason,
            )
            assertEquals(MapSearchMode.MAP_CENTER, viewModel.uiState.value.searchMode)

            shouldFail = false
            viewModel.retrySpotSearch()
            advanceUntilIdle()

            assertEquals(2, repository.locationSearchCallCount)
            assertEquals(mapCenterCoordinate, repository.lastLocationCoordinate)
            assertEquals(expectedSpots, viewModel.uiState.value.spots)
            assertEquals(mapCenterCoordinate, viewModel.uiState.value.searchFocusCoordinate)
            assertEquals(MapSearchMode.MAP_CENTER, viewModel.uiState.value.searchMode)
            assertNull(viewModel.uiState.value.searchFailure)
            assertNull(viewModel.uiState.value.errorMessageResId)
        }

    @Test
    fun `키워드 검색이 일부 실패하면 조회된 결과와 일부 실패 안내를 함께 표시한다`() =
        runTest {
            val expectedSpots = listOf(sampleSpot("partial", CollectionSpotType.BATTERY_BIN))
            val repository = FakeCollectionSpotRepository(
                keywordSpots = expectedSpots,
                isKeywordSearchPartial = true,
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.NotFound,
            )

            viewModel.onSearchKeywordChanged("상동")
            viewModel.searchByKeyword()
            advanceUntilIdle()

            assertEquals(expectedSpots, viewModel.uiState.value.spots)
            assertEquals(
                R.string.map_spot_search_partial_failure_message,
                viewModel.uiState.value.partialWarningMessageResId,
            )
            assertNull(viewModel.uiState.value.errorMessageResId)
        }

}
