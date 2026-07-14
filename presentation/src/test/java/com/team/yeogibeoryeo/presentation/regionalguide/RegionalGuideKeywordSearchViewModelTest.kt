package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteSchedule
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteType
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionSearchCandidateUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class RegionalGuideKeywordSearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = RegionalGuideMainDispatcherRule()

    @Test
    fun `모호한 키워드 검색은 지역 후보 목록을 보여준다`() = runTest {
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
    fun `명시적 검색 없이도 키워드 입력은 디바운스 후 지역 후보를 보여준다`() = runTest {
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
    fun `키워드 추천은 오타 입력값을 임의로 교체하지 않고 유지한다`() = runTest {
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
    fun `입력기로 현재 입력값이 보정되어도 검색 실행 키워드를 복원한다`() = runTest {
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
        assertEquals(R.string.regional_guide_empty_keyword_not_found_title, state.titleResId)
        assertEquals(R.string.regional_guide_empty_keyword_not_found_message, state.messageResId)
        assertEquals(RegionalGuideEmptyActionType.SEARCH_AGAIN, state.action?.type)
        assertEquals(R.string.regional_guide_empty_action_search_again, state.action?.labelResId)
    }

    @Test
    fun `다시 검색 동작은 키워드를 유지한 채 빈 결과를 초기화한다`() = runTest {
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
    fun `모호한 검색 후 키워드가 변경되면 후보 목록을 초기화한다`() = runTest {
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
    fun `후보 선택은 검색어를 유지하고 지역 선택 상태를 갱신한다`() = runTest {
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
    fun `지역 후보 상세에서 뒤로가기를 요청하면 이전 지역 후보 목록을 복원한다`() = runTest {
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
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(
                candidates = listOf(
                    sampleGuide(
                        sido = "울산광역시",
                        sigungu = "울주군",
                        targetRegionName = "온양읍"
                    )
                )
            )
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

        val successState = viewModel.uiState.value as RegionalGuideUiState.Success
        assertTrue(successState.canRestoreCandidates)

        assertTrue(viewModel.restoreCandidatesFromDetail())

        val restoredState = viewModel.uiState.value as RegionalGuideUiState.Ambiguous
        assertEquals("온양", restoredState.query)
        assertEquals(2, restoredState.candidates.size)
        assertEquals("온양", viewModel.searchKeyword.value)
        with(viewModel.regionSelectorUiState.value) {
            assertNull(selectedSido)
            assertNull(selectedSigungu)
            assertNull(selectedEupmyeondong)
        }
    }

    @Test
    fun `검색 후보에서 파생된 가이드 후보는 뒤로가기로 검색 후보 목록까지 순서대로 복원한다`() = runTest {
        val viewModel = createViewModel(
            regionOptionsRepository = FakeRegionOptionsRepository(
                sigunguOptionsBySido = mapOf(
                    "대전광역시" to listOf("중구")
                ),
                keywordRegions = listOf(
                    Region(sido = "대전광역시", sigungu = "중구"),
                    Region(sido = "서울특별시", sigungu = "중구")
                )
            ),
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(
                candidates = listOf(
                    sampleGuide(
                        sido = "대전광역시",
                        sigungu = "중구",
                        targetRegionName = "은행동"
                    ),
                    sampleGuide(
                        sido = "대전광역시",
                        sigungu = "중구",
                        targetRegionName = "대흥동"
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("중구")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        val searchCandidate = (viewModel.uiState.value as RegionalGuideUiState.Ambiguous)
            .candidates
            .first { candidate -> candidate.sido == "대전광역시" }

        viewModel.onRegionCandidateSelected(searchCandidate)
        advanceUntilIdle()

        val guideCandidatesState = viewModel.uiState.value as RegionalGuideUiState.GuideCandidates
        assertTrue(guideCandidatesState.canRestoreCandidates)

        viewModel.onRegionalGuideCandidateSelected(guideCandidatesState.candidates.first())

        val successState = viewModel.uiState.value as RegionalGuideUiState.Success
        assertTrue(successState.canRestoreCandidates)

        assertTrue(viewModel.restoreCandidatesFromDetail())

        val restoredGuideCandidatesState = viewModel.uiState.value as RegionalGuideUiState.GuideCandidates
        assertEquals("대전광역시 > 중구", restoredGuideCandidatesState.query)
        assertEquals(2, restoredGuideCandidatesState.candidates.size)
        assertTrue(restoredGuideCandidatesState.canRestoreCandidates)

        assertTrue(viewModel.restoreCandidatesFromDetail())

        val restoredSearchCandidatesState = viewModel.uiState.value as RegionalGuideUiState.Ambiguous
        assertEquals("중구", restoredSearchCandidatesState.query)
        assertEquals(4, restoredSearchCandidatesState.candidates.size)
        assertEquals("중구", viewModel.searchKeyword.value)

        assertTrue(viewModel.restoreCandidatesFromDetail())
        assertEquals(RegionalGuideUiState.Idle, viewModel.uiState.value)
        assertEquals("", viewModel.searchKeyword.value)
        with(viewModel.regionSelectorUiState.value) {
            assertNull(selectedSido)
            assertNull(selectedSigungu)
            assertNull(selectedEupmyeondong)
        }
    }

    @Test
    fun `후보에서 파생된 조회가 로딩 중이면 뒤로가기로 이전 검색 후보 목록을 복원한다`() = runTest {
        val viewModel = createViewModel(
            regionOptionsRepository = FakeRegionOptionsRepository(
                sigunguOptionsBySido = mapOf(
                    "대전광역시" to listOf("중구")
                ),
                keywordRegions = listOf(
                    Region(sido = "대전광역시", sigungu = "중구"),
                    Region(sido = "서울특별시", sigungu = "중구")
                )
            ),
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(
                candidates = listOf(
                    sampleGuide(
                        sido = "대전광역시",
                        sigungu = "중구",
                        targetRegionName = "은행동"
                    )
                ),
                delayMillis = 1_000L
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("중구")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        val searchCandidate = (viewModel.uiState.value as RegionalGuideUiState.Ambiguous)
            .candidates
            .first { candidate -> candidate.sido == "대전광역시" }

        viewModel.onRegionCandidateSelected(searchCandidate)
        runCurrent()

        val loadingState = viewModel.uiState.value as RegionalGuideUiState.Loading
        assertEquals("대전광역시 > 중구", loadingState.query)
        assertTrue(loadingState.canRestoreCandidates)

        assertTrue(viewModel.restoreCandidatesFromDetail())
        advanceUntilIdle()

        val restoredSearchCandidatesState = viewModel.uiState.value as RegionalGuideUiState.Ambiguous
        assertEquals("중구", restoredSearchCandidatesState.query)
        assertEquals(4, restoredSearchCandidatesState.candidates.size)
    }

    @Test
    fun `후보에서 파생된 조회가 실패하면 뒤로가기로 이전 검색 후보 목록을 복원한다`() = runTest {
        val viewModel = createViewModel(
            regionOptionsRepository = FakeRegionOptionsRepository(
                sigunguOptionsBySido = mapOf(
                    "대전광역시" to listOf("중구")
                ),
                keywordRegions = listOf(
                    Region(sido = "대전광역시", sigungu = "중구"),
                    Region(sido = "서울특별시", sigungu = "중구")
                )
            ),
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(
                failure = IllegalStateException("조회 실패")
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("중구")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        val searchCandidate = (viewModel.uiState.value as RegionalGuideUiState.Ambiguous)
            .candidates
            .first { candidate -> candidate.sido == "대전광역시" }

        viewModel.onRegionCandidateSelected(searchCandidate)
        advanceUntilIdle()

        val errorState = viewModel.uiState.value as RegionalGuideUiState.Error

        assertEquals("대전광역시 > 중구", errorState.query)
        assertTrue(errorState.canRestoreCandidates)
        assertTrue(viewModel.restoreCandidatesFromDetail())

        val restoredSearchCandidatesState = viewModel.uiState.value as RegionalGuideUiState.Ambiguous
        assertEquals("중구", restoredSearchCandidatesState.query)
        assertEquals(4, restoredSearchCandidatesState.candidates.size)
    }

    @Test
    fun `후보에서 파생된 조회가 예외를 던지면 뒤로가기로 이전 검색 후보 목록을 복원한다`() = runTest {
        val viewModel = createViewModel(
            regionOptionsRepository = FakeRegionOptionsRepository(
                sigunguOptionsBySido = mapOf(
                    "대전광역시" to listOf("중구")
                ),
                keywordRegions = listOf(
                    Region(sido = "대전광역시", sigungu = "중구"),
                    Region(sido = "서울특별시", sigungu = "중구")
                )
            ),
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(
                throwable = IllegalStateException("조회 실패")
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("중구")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        val searchCandidate = (viewModel.uiState.value as RegionalGuideUiState.Ambiguous)
            .candidates
            .first { candidate -> candidate.sido == "대전광역시" }

        viewModel.onRegionCandidateSelected(searchCandidate)
        advanceUntilIdle()

        val errorState = viewModel.uiState.value as RegionalGuideUiState.Error

        assertEquals("대전광역시 > 중구", errorState.query)
        assertTrue(errorState.canRestoreCandidates)
        assertTrue(viewModel.restoreCandidatesFromDetail())

        val restoredSearchCandidatesState = viewModel.uiState.value as RegionalGuideUiState.Ambiguous
        assertEquals("중구", restoredSearchCandidatesState.query)
        assertEquals(4, restoredSearchCandidatesState.candidates.size)
    }

    @Test
    fun `후보에서 파생된 조회 결과가 없으면 후보 복원 스택을 비운다`() = runTest {
        val viewModel = createViewModel(
            regionOptionsRepository = FakeRegionOptionsRepository(
                sigunguOptionsBySido = mapOf(
                    "대전광역시" to listOf("중구")
                ),
                keywordRegions = listOf(
                    Region(sido = "대전광역시", sigungu = "중구"),
                    Region(sido = "서울특별시", sigungu = "중구")
                )
            ),
            regionalGuideRepository = FakeRegionalDisposalGuideRepository()
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("중구")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        val searchCandidate = (viewModel.uiState.value as RegionalGuideUiState.Ambiguous)
            .candidates
            .first { candidate -> candidate.sido == "대전광역시" }

        viewModel.onRegionCandidateSelected(searchCandidate)
        advanceUntilIdle()

        val emptyState = viewModel.uiState.value as RegionalGuideUiState.Empty

        assertEquals("대전광역시 > 중구", emptyState.query)
        assertFalse(viewModel.restoreCandidatesFromDetail())
        assertTrue(viewModel.uiState.value is RegionalGuideUiState.Empty)
    }

    @Test
    fun `후보에서 파생된 조회 후보가 선택 지역과 맞지 않으면 후보 복원 스택을 비운다`() = runTest {
        val viewModel = createViewModel(
            regionOptionsRepository = FakeRegionOptionsRepository(
                sigunguOptionsBySido = mapOf(
                    "대전광역시" to listOf("중구")
                ),
                keywordRegions = listOf(
                    Region(sido = "대전광역시", sigungu = "중구"),
                    Region(sido = "서울특별시", sigungu = "중구")
                )
            ),
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(
                candidates = listOf(
                    sampleGuide(
                        sido = "서울특별시",
                        sigungu = "중구",
                        targetRegionName = "명동"
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("중구")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        val searchCandidate = (viewModel.uiState.value as RegionalGuideUiState.Ambiguous)
            .candidates
            .first { candidate -> candidate.sido == "대전광역시" }

        viewModel.onRegionCandidateSelected(searchCandidate)
        advanceUntilIdle()

        val emptyState = viewModel.uiState.value as RegionalGuideUiState.Empty

        assertEquals("대전광역시 > 중구", emptyState.query)
        assertFalse(viewModel.restoreCandidatesFromDetail())
        assertTrue(viewModel.uiState.value is RegionalGuideUiState.Empty)
    }

    @Test
    fun `대전광역시 중구 전체 기준 문전수거 후보는 오류가 아닌 상세로 진입한다`() = runTest {
        val viewModel = createViewModel(
            regionOptionsRepository = FakeRegionOptionsRepository(
                sigunguOptionsBySido = mapOf(
                    "대전광역시" to listOf("중구")
                ),
                keywordRegions = listOf(
                    Region(sido = "대전광역시", sigungu = "중구"),
                    Region(sido = "서울특별시", sigungu = "중구")
                )
            ),
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(
                candidates = listOf(
                    daejeonJungguOverallGuide("일, 월, 화, 수, 목"),
                    daejeonJungguOverallGuide("일, 월, 화, 수, 목, 금")
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("중구")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        val searchCandidate = (viewModel.uiState.value as RegionalGuideUiState.Ambiguous)
            .candidates
            .first { candidate -> candidate.sido == "대전광역시" }

        viewModel.onRegionCandidateSelected(searchCandidate)
        advanceUntilIdle()

        val state = viewModel.uiState.value as RegionalGuideUiState.Success

        assertEquals("대전광역시 > 중구", state.query)
        assertEquals("대전광역시 중구", state.guide.regionName)
        assertEquals("없음", state.guide.managementZoneName)
        assertEquals("없음", state.guide.targetRegionName)
        assertTrue(state.guide.schedules.isNotEmpty())
    }

    @Test
    fun `후보 선택 후 재시도는 검색어 대신 선택 지역을 사용한다`() = runTest {
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
    fun `후보 선택은 지역 선택 상태의 행정동을 정규화한다`() = runTest {
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

    @Test
    fun `과거 안양8동 키워드 검색은 표시 지역의 만안구를 유지하고 조회 키만 안양시로 축약한다`() = runTest {
        val regionalGuideRepository = FakeRegionalDisposalGuideRepository(
            candidates = listOf(
                RegionalDisposalGuide(
                    region = Region(
                        sido = "경기도",
                        sigungu = "안양시 만안구",
                        eupmyeondong = "명학동"
                    ),
                    targetRegionName = "명학동",
                    managementZoneName = "명학동",
                    schedules = emptyList()
                )
            )
        )
        val viewModel = createViewModel(
            regionRepository = FakeRegionRepository(
                resolvedRegion = Region(
                    sido = "경기도",
                    sigungu = "안양시 만안구",
                    eupmyeondong = "안양8동"
                )
            ),
            regionOptionsRepository = FakeRegionOptionsRepository(
                sigunguOptionsBySido = mapOf(
                    "경기도" to listOf("안양시 만안구")
                ),
                eupmyeondongOptionsByRegion = mapOf(
                    "경기도" to mapOf(
                        "안양시 만안구" to listOf("명학동")
                    )
                )
            ),
            regionalGuideRepository = regionalGuideRepository
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("경기도 안양시 만안구 안양8동")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        val state = viewModel.uiState.value as RegionalGuideUiState.Success

        assertEquals("안양시", regionalGuideRepository.queries.single().sigunguQuery)
        assertEquals("경기도 안양시 만안구 명학동", state.guide.regionName)

        with(viewModel.regionSelectorUiState.value) {
            assertEquals("경기도", selectedSido)
            assertEquals("안양시 만안구", selectedSigungu)
            assertEquals("명학동", selectedEupmyeondong)
        }
    }

}

private fun daejeonJungguOverallGuide(foodDisposalDays: String): RegionalDisposalGuide =
    RegionalDisposalGuide(
        region = Region(sido = "대전광역시", sigungu = "중구"),
        managementZoneName = "없음",
        targetRegionName = "없음",
        disposalPlaceType = "문전수거",
        schedules = listOf(
            RegionalWasteSchedule(
                wasteType = RegionalWasteType.FOOD,
                disposalDays = foodDisposalDays,
                disposalStartTime = null,
                disposalEndTime = null,
                disposalMethod = null
            )
        ),
        uncollectedDays = "추석명절, 설명절",
    )


