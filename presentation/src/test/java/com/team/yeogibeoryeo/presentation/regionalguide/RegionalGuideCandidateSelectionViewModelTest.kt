package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class RegionalGuideCandidateSelectionViewModelTest {

    @get:Rule
    val mainDispatcherRule = RegionalGuideMainDispatcherRule()

    @Test
    fun `지역 가이드 조회 결과가 여러 후보이면 후보 목록을 보여준다`() = runTest {
        val viewModel = createViewModel(
            regionRepository = FakeRegionRepository(
                resolvedRegion = Region(sigungu = "울주군")
            ),
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(
                candidates = listOf(
                    sampleGuide(
                        sido = "울산광역시",
                        sigungu = "울주군",
                        targetRegionName = "범서, 온양, 웅촌, 언양, 삼남, 상북, 온산, 청량, 서생"
                    ),
                    sampleGuide(
                        sido = "울산광역시",
                        sigungu = "울주군",
                        targetRegionName = "두동, 두서, 삼동"
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("울주군")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        val state = viewModel.uiState.value as RegionalGuideUiState.GuideCandidates

        assertEquals("울주군", state.query)
        assertEquals(RegionalGuideCandidateReason.MULTIPLE_CANDIDATES, state.reason)
        assertEquals(2, state.candidates.size)
        assertEquals(
            listOf(
                "두동, 두서, 삼동",
                "범서, 온양, 웅촌, 언양, 삼남, 상북, 온산, 청량, 서생"
            ),
            state.candidates.map { candidate -> candidate.displayText }
        )
    }

    @Test
    fun `직접 매칭 실패 후 대체 후보 목록이 표시되면 대체 사유를 보여준다`() = runTest {
        val region = Region(
            sido = "강원특별자치도",
            sigungu = "강릉시",
            eupmyeondong = "사천면"
        )
        val viewModel = createViewModel(
            regionRepository = FakeRegionRepository(resolvedRegion = region),
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(
                candidates = listOf(
                    RegionalDisposalGuide(
                        region = region.copy(eupmyeondong = null),
                        managementZoneName = "없음",
                        targetRegionName = "없음",
                        disposalPlaceType = "문전수거",
                        schedules = emptyList()
                    ),
                    RegionalDisposalGuide(
                        region = region.copy(eupmyeondong = null),
                        managementZoneName = "없음",
                        targetRegionName = "없음",
                        disposalPlaceType = "거점수거",
                        schedules = emptyList()
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("사천면")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        val state = viewModel.uiState.value as RegionalGuideUiState.GuideCandidates

        assertEquals(
            RegionalGuideCandidateReason.FALLBACK_BECAUSE_DIRECT_MATCH_NOT_FOUND,
            state.reason
        )
        assertEquals(2, state.candidates.size)
    }

    @Test
    fun `정확 매칭 후보가 여러 개이면 복수 정확 매칭 사유를 보여준다`() = runTest {
        val region = Region(
            sido = "대전광역시",
            sigungu = "유성구",
            eupmyeondong = "반석동"
        )
        val viewModel = createViewModel(
            regionRepository = FakeRegionRepository(resolvedRegion = region),
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(
                candidates = listOf(
                    RegionalDisposalGuide(
                        region = region,
                        managementZoneName = "노은2동",
                        targetRegionName = "반석동",
                        schedules = emptyList()
                    ),
                    RegionalDisposalGuide(
                        region = region,
                        managementZoneName = "노은3동",
                        targetRegionName = "반석동",
                        schedules = emptyList()
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("반석동")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        val state = viewModel.uiState.value as RegionalGuideUiState.GuideCandidates

        assertEquals(RegionalGuideCandidateReason.MULTIPLE_EXACT_MATCHES, state.reason)
        assertEquals(2, state.candidates.size)
    }

    @Test
    fun `가이드 후보 검색 후 검색어가 바뀌면 후보 목록을 초기화한다`() = runTest {
        val viewModel = createViewModel(
            regionRepository = FakeRegionRepository(
                resolvedRegion = Region(sido = "SidoA", sigungu = "SigunguA")
            ),
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(
                candidates = listOf(
                    sampleGuide(
                        sido = "SidoA",
                        sigungu = "SigunguA",
                        targetRegionName = "zone1"
                    ),
                    sampleGuide(
                        sido = "SidoA",
                        sigungu = "SigunguA",
                        targetRegionName = "zone2"
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("SigunguA")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is RegionalGuideUiState.GuideCandidates)

        viewModel.onSearchKeywordChanged("Sigungu")

        assertEquals("Sigungu", viewModel.searchKeyword.value)
        assertEquals(RegionalGuideUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `지역 가이드 후보를 선택하면 선택한 가이드를 보여준다`() = runTest {
        val viewModel = createViewModel(
            regionRepository = FakeRegionRepository(
                resolvedRegion = Region(sigungu = "울주군")
            ),
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(
                candidates = listOf(
                    sampleGuide(
                        sido = "울산광역시",
                        sigungu = "울주군",
                        targetRegionName = "범서, 온양, 웅촌, 언양, 삼남, 상북, 온산, 청량, 서생"
                    ),
                    sampleGuide(
                        sido = "울산광역시",
                        sigungu = "울주군",
                        targetRegionName = "두동, 두서, 삼동"
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("울주군")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        val candidate = (viewModel.uiState.value as RegionalGuideUiState.GuideCandidates)
            .candidates
            .first()

        viewModel.onRegionalGuideCandidateSelected(candidate)

        val state = viewModel.uiState.value as RegionalGuideUiState.Success

        assertEquals("울주군", viewModel.searchKeyword.value)
        assertEquals("울주군", state.query)
        assertEquals(
            "두동, 두서, 삼동",
            state.guide.targetRegionName
        )

        with(viewModel.regionSelectorUiState.value) {
            assertEquals("울산광역시", selectedSido)
            assertEquals("울주군", selectedSigungu)
            assertNull(selectedEupmyeondong)
        }
    }

    @Test
    fun `일반 후보 상세에서 뒤로가기를 요청하면 이전 후보 목록을 복원한다`() = runTest {
        val viewModel = createViewModel(
            regionRepository = FakeRegionRepository(
                resolvedRegion = Region(sigungu = "울주군")
            ),
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(
                candidates = listOf(
                    sampleGuide(
                        sido = "울산광역시",
                        sigungu = "울주군",
                        targetRegionName = "범서, 온양, 웅촌, 언양, 삼남, 상북, 온산, 청량, 서생"
                    ),
                    sampleGuide(
                        sido = "울산광역시",
                        sigungu = "울주군",
                        targetRegionName = "두동, 두서, 삼동"
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("울주군")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        val candidate = (viewModel.uiState.value as RegionalGuideUiState.GuideCandidates)
            .candidates
            .first()
        val scrollPosition = RegionalGuideCandidateListScrollPosition(
            firstVisibleItemIndex = 1,
            firstVisibleItemScrollOffset = 24,
        )

        viewModel.onCandidateListScrollPositionChanged(scrollPosition)

        viewModel.onRegionalGuideCandidateSelected(candidate)

        val successState = viewModel.uiState.value as RegionalGuideUiState.Success
        assertTrue(successState.canRestoreCandidates)

        assertTrue(viewModel.restoreCandidatesFromDetail())

        val restoredState = viewModel.uiState.value as RegionalGuideUiState.GuideCandidates
        assertEquals("울주군", restoredState.query)
        assertEquals(RegionalGuideCandidateReason.MULTIPLE_CANDIDATES, restoredState.reason)
        assertEquals(2, restoredState.candidates.size)
        assertEquals(scrollPosition, restoredState.candidateListScrollPosition)
        assertEquals("울주군", viewModel.searchKeyword.value)
        assertEquals("울주군", viewModel.regionSelectorUiState.value.selectedSigungu)

        viewModel.onRegionalGuideCandidateSelected(restoredState.candidates.last())

        val reselectedState = viewModel.uiState.value as RegionalGuideUiState.Success
        assertEquals(
            "범서, 온양, 웅촌, 언양, 삼남, 상북, 온산, 청량, 서생",
            reselectedState.guide.targetRegionName
        )
    }

    @Test
    fun `새 후보 목록은 이전 후보 목록 스크롤 위치를 재사용하지 않는다`() = runTest {
        val viewModel = createViewModel(
            regionRepository = FakeRegionRepository(
                resolvedRegion = Region(sigungu = "울주군")
            ),
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(
                candidates = listOf(
                    sampleGuide(
                        sido = "울산광역시",
                        sigungu = "울주군",
                        targetRegionName = "범서, 온양, 웅촌"
                    ),
                    sampleGuide(
                        sido = "울산광역시",
                        sigungu = "울주군",
                        targetRegionName = "두동, 두서, 삼동"
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("울주군")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        viewModel.onCandidateListScrollPositionChanged(
            RegionalGuideCandidateListScrollPosition(
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 32,
            )
        )

        viewModel.searchByKeyword("울주군")
        advanceUntilIdle()

        val state = viewModel.uiState.value as RegionalGuideUiState.GuideCandidates
        assertEquals(
            RegionalGuideCandidateListScrollPosition.Initial,
            state.candidateListScrollPosition
        )
    }

    @Test
    fun `대체 수거 유형 후보 상세에서 뒤로가기를 요청하면 이전 후보 패널 상태를 복원한다`() = runTest {
        val region = Region(
            sido = "강원특별자치도",
            sigungu = "강릉시",
            eupmyeondong = "사천면"
        )
        val viewModel = createViewModel(
            regionRepository = FakeRegionRepository(resolvedRegion = region),
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(
                candidates = listOf(
                    RegionalDisposalGuide(
                        region = region.copy(eupmyeondong = null),
                        managementZoneName = "없음",
                        targetRegionName = "없음",
                        disposalPlaceType = "문전수거",
                        schedules = emptyList()
                    ),
                    RegionalDisposalGuide(
                        region = region.copy(eupmyeondong = null),
                        managementZoneName = "없음",
                        targetRegionName = "없음",
                        disposalPlaceType = "거점수거",
                        schedules = emptyList()
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("사천면")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        val candidate = (viewModel.uiState.value as RegionalGuideUiState.GuideCandidates)
            .candidates
            .first()

        viewModel.onRegionalGuideCandidateSelected(candidate)

        val successState = viewModel.uiState.value as RegionalGuideUiState.Success
        assertTrue(successState.canRestoreCandidates)

        assertTrue(viewModel.restoreCandidatesFromDetail())

        val restoredState = viewModel.uiState.value as RegionalGuideUiState.GuideCandidates
        assertEquals(
            RegionalGuideCandidateReason.FALLBACK_BECAUSE_DIRECT_MATCH_NOT_FOUND,
            restoredState.reason
        )
        assertEquals(2, restoredState.candidates.size)
        with(viewModel.regionSelectorUiState.value) {
            assertEquals("강원특별자치도", selectedSido)
            assertEquals("강릉시", selectedSigungu)
            assertEquals("사천면", selectedEupmyeondong)
        }
    }

    @Test
    fun `복수 정확 매칭 후보 상세에서 뒤로가기를 요청하면 이전 후보 목록을 복원한다`() = runTest {
        val region = Region(
            sido = "대전광역시",
            sigungu = "유성구",
            eupmyeondong = "반석동"
        )
        val viewModel = createViewModel(
            regionRepository = FakeRegionRepository(resolvedRegion = region),
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(
                candidates = listOf(
                    RegionalDisposalGuide(
                        region = region,
                        managementZoneName = "노은2동",
                        targetRegionName = "반석동",
                        schedules = emptyList()
                    ),
                    RegionalDisposalGuide(
                        region = region,
                        managementZoneName = "노은3동",
                        targetRegionName = "반석동",
                        schedules = emptyList()
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("반석동")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        val candidate = (viewModel.uiState.value as RegionalGuideUiState.GuideCandidates)
            .candidates
            .first()

        viewModel.onRegionalGuideCandidateSelected(candidate)

        assertTrue(viewModel.restoreCandidatesFromDetail())

        val restoredState = viewModel.uiState.value as RegionalGuideUiState.GuideCandidates
        assertEquals(RegionalGuideCandidateReason.MULTIPLE_EXACT_MATCHES, restoredState.reason)
        assertEquals(2, restoredState.candidates.size)
        assertEquals("반석동", viewModel.searchKeyword.value)
    }

    @Test
    fun `직접 조회 상세에서는 후보 목록 복원을 처리하지 않는다`() = runTest {
        val viewModel = createViewModel(
            regionRepository = FakeRegionRepository(
                resolvedRegion = Region(
                    sido = "서울특별시",
                    sigungu = "마포구",
                    eupmyeondong = "합정동"
                )
            ),
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(
                candidates = listOf(
                    RegionalDisposalGuide(
                        region = Region(
                            sido = "서울특별시",
                            sigungu = "마포구",
                            eupmyeondong = "합정동"
                        ),
                        targetRegionName = "합정동",
                        managementZoneName = "합정동",
                        schedules = emptyList()
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("합정동")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        val state = viewModel.uiState.value as RegionalGuideUiState.Success

        assertFalse(state.canRestoreCandidates)
        assertFalse(viewModel.restoreCandidatesFromDetail())
    }

}


