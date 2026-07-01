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
    fun `sido selection resets lower selections and applies latest sigungu options`() = runTest {
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
    fun `stale sigungu option loading does not override current sido selection`() = runTest {
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
    fun `sigungu selection resets eupmyeondong and applies latest eupmyeondong options`() = runTest {
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
    fun `selected region search maps success state and normalizes sigungu query`() = runTest {
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
    fun `keyword search collapses expanded region selector dropdown`() = runTest {
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
    fun `selected region search collapses expanded region selector dropdown`() = runTest {
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
    fun `retry last request repeats selected region lookup`() = runTest {
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
    fun `selected region search without info result shows api empty guide action`() = runTest {
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
    fun `selected eupmyeondong without direct guide match does not expose fallback candidates`() = runTest {
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


