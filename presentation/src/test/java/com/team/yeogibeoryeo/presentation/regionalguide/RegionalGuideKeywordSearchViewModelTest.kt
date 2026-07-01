package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionSearchCandidateUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class RegionalGuideKeywordSearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = RegionalGuideMainDispatcherRule()

    @Test
    fun `ambiguous keyword search exposes region candidates`() = runTest {
        val viewModel = createViewModel(
            regionOptionsRepository = FakeRegionOptionsRepository(
                keywordRegions = listOf(
                    Region(
                        sido = "울산광역시",
                        sigungu = "울주군",
                        eupmyeondong = "온양읍"
                    ),
                    Region(
                        sido = "충청남도",
                        sigungu = "아산시",
                        eupmyeondong = "온양1동"
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("온양")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        val state = viewModel.uiState.value as RegionalGuideUiState.Ambiguous

        assertEquals("온양", state.query)
        assertEquals(2, state.candidates.size)
        assertEquals("울산광역시 > 울주군 > 온양읍", state.candidates.first().displayText)
    }

    @Test
    fun `keyword input shows region candidates after debounce without explicit search`() = runTest {
        val viewModel = createViewModel(
            regionOptionsRepository = FakeRegionOptionsRepository(
                keywordRegions = listOf(
                    Region(
                        sido = "SidoA",
                        sigungu = "SigunguA",
                        eupmyeondong = "onyang"
                    ),
                    Region(
                        sido = "SidoB",
                        sigungu = "SigunguB",
                        eupmyeondong = "onyang2"
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("on")
        advanceTimeBy(399)

        assertEquals(RegionalGuideUiState.Idle, viewModel.uiState.value)

        advanceTimeBy(1)
        advanceUntilIdle()

        val state = viewModel.uiState.value as RegionalGuideUiState.Ambiguous

        assertEquals("on", viewModel.searchKeyword.value)
        assertEquals("on", state.query)
        assertEquals(2, state.candidates.size)

        with(viewModel.regionSelectorUiState.value) {
            assertNull(selectedSido)
            assertNull(selectedSigungu)
            assertNull(selectedEupmyeondong)
        }
    }

    @Test
    fun `keyword suggestion keeps typed typo keyword without replacing it`() = runTest {
        val viewModel = createViewModel(
            regionRepository = FakeRegionRepository(
                resolvedRegion = Region(sigungu = "중안구")
            ),
            regionOptionsRepository = FakeRegionOptionsRepository(
                keywordRegions = listOf(
                    Region(
                        sido = "경기도",
                        sigungu = "수원시 장안구"
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("중안구")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals("중안구", viewModel.searchKeyword.value)
        assertEquals(RegionalGuideUiState.Idle, viewModel.uiState.value)

        with(viewModel.regionSelectorUiState.value) {
            assertNull(selectedSido)
            assertNull(selectedSigungu)
            assertNull(selectedEupmyeondong)
        }
    }

    @Test
    fun `keyword search restores submitted keyword when current input was corrected by ime`() = runTest {
        val viewModel = createViewModel(
            regionRepository = FakeRegionRepository(
                resolvedRegion = Region(sigungu = "중안구")
            ),
            regionOptionsRepository = FakeRegionOptionsRepository(
                keywordRegions = listOf(
                    Region(
                        sido = "경기도",
                        sigungu = "수원시 장안구"
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("장안구")
        viewModel.searchByKeyword("중안구")
        advanceUntilIdle()

        assertEquals("중안구", viewModel.searchKeyword.value)
        val state = viewModel.uiState.value as RegionalGuideUiState.Empty
        assertEquals("검색 결과를 찾지 못했어요.", state.title)
        assertEquals("시/군/구까지 입력해 다시 검색해 주세요.", state.message)
        assertEquals(RegionalGuideEmptyActionType.SEARCH_AGAIN, state.action?.type)
        assertEquals("다시 검색하기", state.action?.label)
    }

    @Test
    fun `search again action clears empty result while keeping keyword`() = runTest {
        val viewModel = createViewModel(
            regionRepository = FakeRegionRepository(
                resolvedRegion = Region(sigungu = "중안구")
            ),
            regionOptionsRepository = FakeRegionOptionsRepository(
                keywordRegions = listOf(
                    Region(
                        sido = "경기도",
                        sigungu = "수원시 장안구"
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("중안구")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is RegionalGuideUiState.Empty)

        viewModel.prepareSearchAgain()

        assertEquals("중안구", viewModel.searchKeyword.value)
        assertEquals(RegionalGuideUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `candidate list is cleared when keyword changes after ambiguous search`() = runTest {
        val viewModel = createViewModel(
            regionOptionsRepository = FakeRegionOptionsRepository(
                keywordRegions = listOf(
                    Region(
                        sido = "SidoA",
                        sigungu = "SigunguA",
                        eupmyeondong = "onyang"
                    ),
                    Region(
                        sido = "SidoB",
                        sigungu = "SigunguB",
                        eupmyeondong = "onyang2"
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("on")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is RegionalGuideUiState.Ambiguous)

        viewModel.onSearchKeywordChanged("o")

        assertEquals("o", viewModel.searchKeyword.value)
        assertEquals(RegionalGuideUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `candidate selection keeps search keyword and updates selector state`() = runTest {
        val regionalGuideRepository = FakeRegionalDisposalGuideRepository(
            candidates = listOf(
                sampleGuide(
                    sido = "울산광역시",
                    sigungu = "울주군",
                    targetRegionName = "온양읍"
                )
            )
        )
        val viewModel = createViewModel(
            regionOptionsRepository = FakeRegionOptionsRepository(
                sigunguOptionsBySido = mapOf(
                    "울산광역시" to listOf("울주군")
                ),
                eupmyeondongOptionsByRegion = mapOf(
                    "울산광역시" to mapOf(
                        "울주군" to listOf("온양읍")
                    )
                ),
                keywordRegions = listOf(
                    Region(
                        sido = "울산광역시",
                        sigungu = "울주군",
                        eupmyeondong = "온양읍"
                    ),
                    Region(
                        sido = "충청남도",
                        sigungu = "아산시",
                        eupmyeondong = "온양1동"
                    )
                )
            ),
            regionalGuideRepository = regionalGuideRepository
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("온양")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        val candidate = (viewModel.uiState.value as RegionalGuideUiState.Ambiguous)
            .candidates
            .first()

        viewModel.onRegionCandidateSelected(candidate)
        advanceUntilIdle()

        val state = viewModel.uiState.value as RegionalGuideUiState.Success

        assertEquals("온양", viewModel.searchKeyword.value)
        assertEquals(candidate.displayText, state.query)
        assertEquals("울주군", regionalGuideRepository.queries.single().sigunguQuery)

        with(viewModel.regionSelectorUiState.value) {
            assertEquals("울산광역시", selectedSido)
            assertEquals("울주군", selectedSigungu)
            assertEquals("온양읍", selectedEupmyeondong)
        }
    }

    @Test
    fun `retry after candidate selection uses selected region instead of search keyword`() = runTest {
        val regionalGuideRepository = FakeRegionalDisposalGuideRepository(
            candidates = listOf(
                sampleGuide(
                    sido = "울산광역시",
                    sigungu = "울주군",
                    targetRegionName = "온양읍"
                )
            )
        )
        val viewModel = createViewModel(
            regionOptionsRepository = FakeRegionOptionsRepository(
                sigunguOptionsBySido = mapOf(
                    "울산광역시" to listOf("울주군")
                ),
                eupmyeondongOptionsByRegion = mapOf(
                    "울산광역시" to mapOf(
                        "울주군" to listOf("온양읍")
                    )
                ),
                keywordRegions = listOf(
                    Region(
                        sido = "울산광역시",
                        sigungu = "울주군",
                        eupmyeondong = "온양읍"
                    ),
                    Region(
                        sido = "충청남도",
                        sigungu = "아산시",
                        eupmyeondong = "온양1동"
                    )
                )
            ),
            regionalGuideRepository = regionalGuideRepository
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("온양")
        advanceTimeBy(400)
        advanceUntilIdle()

        val candidate = (viewModel.uiState.value as RegionalGuideUiState.Ambiguous)
            .candidates
            .first()

        viewModel.onRegionCandidateSelected(candidate)
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("새검색")
        viewModel.retryLastRequest()
        advanceUntilIdle()

        assertEquals("새검색", viewModel.searchKeyword.value)
        assertEquals(2, regionalGuideRepository.queries.size)
        assertTrue(
            regionalGuideRepository.queries.all { query ->
                query.sigunguQuery == "울주군"
            }
        )
    }

    @Test
    fun `candidate selection normalizes administrative district for selector state`() = runTest {
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
                ),
                eupmyeondongOptionsByRegion = mapOf(
                    "경기도" to mapOf(
                        "수원시" to listOf("파장동")
                    )
                )
            ),
            regionalGuideRepository = regionalGuideRepository
        )
        advanceUntilIdle()

        viewModel.onRegionCandidateSelected(
            RegionSearchCandidateUiModel(
                sido = "경기도",
                sigungu = "수원시 장안구",
                eupmyeondong = "파장동"
            )
        )
        advanceUntilIdle()

        assertEquals("수원시", regionalGuideRepository.queries.single().sigunguQuery)

        with(viewModel.regionSelectorUiState.value) {
            assertEquals("경기도", selectedSido)
            assertEquals("수원시", selectedSigungu)
            assertEquals("파장동", selectedEupmyeondong)
        }
    }

}


