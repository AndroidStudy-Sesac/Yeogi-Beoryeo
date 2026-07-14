package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.presentation.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class RegionalGuideSelectorViewModelTest {

    @get:Rule
    val mainDispatcherRule = RegionalGuideMainDispatcherRule()

    @Test
    fun `시도 선택은 하위 선택을 초기화하고 최신 시군구 옵션을 반영한다`() = runTest {
        val regionOptionsRepository = FakeRegionOptionsRepository(
            sigunguOptionsBySido = mapOf(
                "서울특별시" to listOf("중구"),
                "경기도" to listOf("수원시", "성남시")
            ),
            eupmyeondongOptionsByRegion = mapOf(
                "서울특별시" to mapOf("중구" to listOf("신당동"))
            )
        )
        val viewModel = createViewModel(regionOptionsRepository = regionOptionsRepository)
        advanceUntilIdle()

        viewModel.onSidoSelected("서울특별시")
        advanceUntilIdle()
        viewModel.onSigunguSelected("중구")
        advanceUntilIdle()
        viewModel.onEupmyeondongSelected("신당동")

        viewModel.onSidoSelected("경기도")
        advanceUntilIdle()

        with(viewModel.regionSelectorUiState.value) {
            assertEquals("경기도", selectedSido)
            assertNull(selectedSigungu)
            assertNull(selectedEupmyeondong)
            assertEquals(listOf("수원시", "성남시"), sigunguOptions)
            assertEquals(emptyList<String>(), eupmyeondongOptions)
        }
    }

    @Test
    fun `이전 시군구 옵션 로딩 결과는 현재 시도 선택을 덮어쓰지 않는다`() = runTest {
        val delayedSeoulOptions = CompletableDeferred<List<String>>()
        val regionOptionsRepository = FakeRegionOptionsRepository(
            sigunguOptionsBySido = mapOf(
                "경기도" to listOf("수원시")
            ),
            delayedSigunguOptionsBySido = mapOf(
                "서울특별시" to delayedSeoulOptions
            )
        )
        val viewModel = createViewModel(regionOptionsRepository = regionOptionsRepository)
        advanceUntilIdle()

        viewModel.onSidoSelected("서울특별시")
        viewModel.onSidoSelected("경기도")
        advanceUntilIdle()

        delayedSeoulOptions.complete(listOf("중구"))
        advanceUntilIdle()

        with(viewModel.regionSelectorUiState.value) {
            assertEquals("경기도", selectedSido)
            assertEquals(listOf("수원시"), sigunguOptions)
        }
    }

    @Test
    fun `시군구 선택은 읍면동 선택을 초기화하고 최신 읍면동 옵션을 반영한다`() = runTest {
        val regionOptionsRepository = FakeRegionOptionsRepository(
            sigunguOptionsBySido = mapOf(
                "서울특별시" to listOf("중구", "종로구")
            ),
            eupmyeondongOptionsByRegion = mapOf(
                "서울특별시" to mapOf(
                    "중구" to listOf("신당동"),
                    "종로구" to listOf("청운효자동")
                )
            )
        )
        val viewModel = createViewModel(regionOptionsRepository = regionOptionsRepository)
        advanceUntilIdle()

        viewModel.onSidoSelected("서울특별시")
        advanceUntilIdle()
        viewModel.onSigunguSelected("중구")
        advanceUntilIdle()
        viewModel.onEupmyeondongSelected("신당동")

        viewModel.onSigunguSelected("종로구")
        advanceUntilIdle()

        with(viewModel.regionSelectorUiState.value) {
            assertEquals("서울특별시", selectedSido)
            assertEquals("종로구", selectedSigungu)
            assertNull(selectedEupmyeondong)
            assertEquals(listOf("청운효자동"), eupmyeondongOptions)
        }
    }

    @Test
    fun `시군구 선택은 지역 가이드 권역 후보에 맞는 읍면동 옵션만 반영한다`() = runTest {
        val viewModel = createViewModel(
            regionOptionsRepository = FakeRegionOptionsRepository(
                sigunguOptionsBySido = mapOf(
                    "경상북도" to listOf("김천시")
                ),
                eupmyeondongOptionsByRegion = mapOf(
                    "경상북도" to mapOf(
                        "김천시" to listOf("아포읍", "봉산면", "율곡동", "평화남산동")
                    )
                )
            ),
            regionalGuideOptionRepository = FakeRegionalDisposalGuideRepository(
                candidates = listOf(
                    sampleGuide(
                        sido = "경상북도",
                        sigungu = "김천시",
                        targetRegionName = "동지역"
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSidoSelected("경상북도")
        advanceUntilIdle()
        viewModel.onSigunguSelected("김천시")
        advanceUntilIdle()

        with(viewModel.regionSelectorUiState.value) {
            assertEquals("김천시", selectedSigungu)
            assertEquals(listOf("율곡동", "평화남산동"), eupmyeondongOptions)
        }
    }

    @Test
    fun `지역 가이드 권역에 맞는 읍면동이 없으면 빈 선택지를 완료 상태로 반영한다`() = runTest {
        val viewModel = createViewModel(
            regionOptionsRepository = FakeRegionOptionsRepository(
                sigunguOptionsBySido = mapOf(
                    "경상북도" to listOf("김천시")
                ),
                eupmyeondongOptionsByRegion = mapOf(
                    "경상북도" to mapOf(
                        "김천시" to listOf("아포읍", "봉산면")
                    )
                )
            ),
            regionalGuideOptionRepository = FakeRegionalDisposalGuideRepository(
                candidates = listOf(
                    sampleGuide(
                        sido = "경상북도",
                        sigungu = "김천시",
                        targetRegionName = "동지역"
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSidoSelected("경상북도")
        advanceUntilIdle()
        viewModel.onSigunguSelected("김천시")
        advanceUntilIdle()

        with(viewModel.regionSelectorUiState.value) {
            assertEquals("김천시", selectedSigungu)
            assertEquals(emptyList<String>(), eupmyeondongOptions)
        }
    }

    @Test
    fun `선택 지역 검색은 성공 상태를 매핑하고 시군구 조회값을 정규화한다`() = runTest {
        val regionalGuideRepository = FakeRegionalDisposalGuideRepository(
            candidates = listOf(
                sampleGuide(
                    sido = "경기도",
                    sigungu = "수원시",
                    targetRegionName = "수원시 전체"
                )
            )
        )
        val viewModel = createViewModel(
            regionOptionsRepository = FakeRegionOptionsRepository(
                sigunguOptionsBySido = mapOf(
                    "경기도" to listOf("수원시 장안구")
                )
            ),
            regionalGuideRepository = regionalGuideRepository
        )
        advanceUntilIdle()

        viewModel.onSidoSelected("경기도")
        advanceUntilIdle()
        viewModel.onSigunguSelected("수원시 장안구")
        advanceUntilIdle()
        viewModel.onRegionSelectionSearchClick()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is RegionalGuideUiState.Success)
        assertEquals("수원시", regionalGuideRepository.queries.single().sigunguQuery)
    }

    @Test
    fun `선택 지역 검색은 검색어를 선택 지역 문구로 갱신한다`() = runTest {
        val regionalGuideRepository = FakeRegionalDisposalGuideRepository(
            candidates = listOf(
                sampleGuide(
                    sido = "광주광역시",
                    sigungu = "동구",
                    targetRegionName = "동구 전체"
                )
            )
        )
        val viewModel = createViewModel(
            regionOptionsRepository = FakeRegionOptionsRepository(
                sigunguOptionsBySido = mapOf(
                    "광주광역시" to listOf("동구")
                )
            ),
            regionalGuideRepository = regionalGuideRepository
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("금호동")
        viewModel.onSidoSelected("광주광역시")
        advanceUntilIdle()
        viewModel.onSigunguSelected("동구")
        advanceUntilIdle()
        viewModel.onRegionSelectionSearchClick()
        advanceUntilIdle()

        assertEquals("광주광역시 > 동구", viewModel.searchKeyword.value)
        assertTrue(viewModel.uiState.value is RegionalGuideUiState.Success)
    }

    @Test
    fun `키워드 검색은 열린 지역 선택 드롭다운을 접는다`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onRegionSelectorDropdownExpanded(RegionSelectorDropdown.SIDO)

        assertEquals(
            RegionSelectorDropdown.SIDO,
            viewModel.regionSelectorUiState.value.expandedDropdown
        )

        viewModel.onSearchKeywordChanged("없는지역")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        assertNull(viewModel.regionSelectorUiState.value.expandedDropdown)
    }

    @Test
    fun `선택 지역 검색은 열린 지역 선택 드롭다운을 접는다`() = runTest {
        val regionalGuideRepository = FakeRegionalDisposalGuideRepository(
            candidates = listOf(
                sampleGuide(
                    sido = "경기도",
                    sigungu = "수원시",
                    targetRegionName = "없음"
                )
            )
        )
        val viewModel = createViewModel(
            regionOptionsRepository = FakeRegionOptionsRepository(
                sigunguOptionsBySido = mapOf(
                    "경기도" to listOf("수원시")
                )
            ),
            regionalGuideRepository = regionalGuideRepository
        )
        advanceUntilIdle()

        viewModel.onSidoSelected("경기도")
        advanceUntilIdle()
        viewModel.onSigunguSelected("수원시")
        advanceUntilIdle()
        viewModel.onRegionSelectorDropdownExpanded(RegionSelectorDropdown.SIGUNGU)

        viewModel.onRegionSelectionSearchClick()
        advanceUntilIdle()

        assertNull(viewModel.regionSelectorUiState.value.expandedDropdown)
        assertTrue(viewModel.uiState.value is RegionalGuideUiState.Success)
    }

    @Test
    fun `마지막 요청 재시도는 선택 지역 조회를 반복한다`() = runTest {
        val regionalGuideRepository = FakeRegionalDisposalGuideRepository(
            candidates = listOf(
                sampleGuide(
                    sido = "경기도",
                    sigungu = "수원시",
                    targetRegionName = "수원시 전체"
                )
            )
        )
        val viewModel = createViewModel(
            regionOptionsRepository = FakeRegionOptionsRepository(
                sigunguOptionsBySido = mapOf(
                    "경기도" to listOf("수원시 장안구")
                )
            ),
            regionalGuideRepository = regionalGuideRepository
        )
        advanceUntilIdle()

        viewModel.onSidoSelected("경기도")
        advanceUntilIdle()
        viewModel.onSigunguSelected("수원시 장안구")
        advanceUntilIdle()
        viewModel.onRegionSelectionSearchClick()
        advanceUntilIdle()

        viewModel.retryLastRequest()
        advanceUntilIdle()

        assertEquals(2, regionalGuideRepository.queries.size)
        assertTrue(viewModel.uiState.value is RegionalGuideUiState.Success)
    }

    @Test
    fun `안내 결과가 없는 선택 지역 검색은 공공데이터 빈 결과 안내 동작을 보여준다`() = runTest {
        val viewModel = createViewModel(
            regionOptionsRepository = FakeRegionOptionsRepository(
                sigunguOptionsBySido = mapOf(
                    "경기도" to listOf("수원시")
                )
            ),
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(
                candidates = emptyList()
            )
        )
        advanceUntilIdle()

        viewModel.onSidoSelected("경기도")
        advanceUntilIdle()
        viewModel.onSigunguSelected("수원시")
        advanceUntilIdle()
        viewModel.onRegionSelectionSearchClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value as RegionalGuideUiState.Empty

        assertEquals(R.string.regional_guide_empty_info_not_found_title, state.titleResId)
        assertEquals(R.string.regional_guide_empty_info_not_found_message, state.messageResId)
        assertEquals(RegionalGuideEmptyActionType.SELECT_REGION, state.action?.type)
        assertEquals(R.string.regional_guide_empty_action_select_region, state.action?.labelResId)
    }

    @Test
    fun `직접 안내가 없는 선택 읍면동 검색은 대체 후보를 노출하지 않는다`() = runTest {
        val viewModel = createViewModel(
            regionOptionsRepository = FakeRegionOptionsRepository(
                sigunguOptionsBySido = mapOf(
                    "경기도" to listOf("수원시")
                ),
                eupmyeondongOptionsByRegion = mapOf(
                    "경기도" to mapOf(
                        "수원시" to listOf("파장동")
                    )
                )
            ),
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(
                candidates = listOf(
                    sampleGuide(
                        sido = "경기도",
                        sigungu = "성남시",
                        targetRegionName = "성남시 전체"
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSidoSelected("경기도")
        advanceUntilIdle()
        viewModel.onSigunguSelected("수원시")
        advanceUntilIdle()
        viewModel.onEupmyeondongSelected("파장동")
        viewModel.onRegionSelectionSearchClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value as RegionalGuideUiState.Empty

        assertEquals(R.string.regional_guide_empty_candidate_not_found_title, state.titleResId)
        assertEquals(R.string.regional_guide_empty_candidate_not_found_message, state.messageResId)
        assertEquals(RegionalGuideEmptyActionType.SELECT_REGION, state.action?.type)
    }
}


