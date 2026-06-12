package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.model.toFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteSnapshotRepository
import com.team.yeogibeoryeo.domain.favorite.usecase.GetRegionalGuideFavoriteSnapshotUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoriteUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ToggleRegionalGuideFavoriteUseCase
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import com.team.yeogibeoryeo.domain.region.repository.RegionRepository
import com.team.yeogibeoryeo.domain.region.usecase.ExtractRegionFromAddressUseCase
import com.team.yeogibeoryeo.domain.region.usecase.GetEupmyeondongOptionsUseCase
import com.team.yeogibeoryeo.domain.region.usecase.GetSidoOptionsUseCase
import com.team.yeogibeoryeo.domain.region.usecase.GetSigunguOptionsUseCase
import com.team.yeogibeoryeo.domain.region.usecase.NormalizeRegionForRegionalGuideUseCase
import com.team.yeogibeoryeo.domain.region.usecase.ResolveRegionFromKeywordUseCase
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideQuery
import com.team.yeogibeoryeo.domain.regionalguide.repository.RegionalDisposalGuideRepository
import com.team.yeogibeoryeo.domain.regionalguide.usecase.GetRegionalDisposalGuideUseCase
import com.team.yeogibeoryeo.domain.regionalguide.usecase.NormalizeRegionalGuideQueryUseCase
import com.team.yeogibeoryeo.domain.regionalguide.usecase.SelectRegionalGuideCandidateUseCase
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionSearchCandidateUiModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class RegionalGuideViewModelTest {

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
        assertTrue(viewModel.uiState.value is RegionalGuideUiState.Empty)
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
    fun `candidate selection runs selected region lookup and updates selector state`() = runTest {
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

        assertEquals(candidate.displayText, viewModel.searchKeyword.value)
        assertTrue(viewModel.uiState.value is RegionalGuideUiState.Success)
        assertEquals("울주군", regionalGuideRepository.queries.single().sigunguQuery)

        with(viewModel.regionSelectorUiState.value) {
            assertEquals("울산광역시", selectedSido)
            assertEquals("울주군", selectedSigungu)
            assertEquals("온양읍", selectedEupmyeondong)
        }
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

    @Test
    fun `address lookup normalizes administrative district before selector and guide lookup`() = runTest {
        val regionalGuideRepository = FakeRegionalDisposalGuideRepository(
            candidates = listOf(
                sampleGuide(
                    sido = "경기도",
                    sigungu = "성남시",
                    targetRegionName = "없음"
                )
            )
        )
        val viewModel = createViewModel(
            regionRepository = FakeRegionRepository(
                extractedRegion = Region(
                    sido = "경기도",
                    sigungu = "성남시 중원구",
                    eupmyeondong = "중앙동"
                )
            ),
            regionOptionsRepository = FakeRegionOptionsRepository(
                sigunguOptionsBySido = mapOf(
                    "경기도" to listOf("성남시")
                ),
                eupmyeondongOptionsByRegion = mapOf(
                    "경기도" to mapOf(
                        "성남시" to listOf("중앙동")
                    )
                )
            ),
            regionalGuideRepository = regionalGuideRepository
        )
        advanceUntilIdle()

        viewModel.loadByAddress("경기도 성남시 중원구 중앙동")
        advanceUntilIdle()

        assertEquals("성남시", regionalGuideRepository.queries.single().sigunguQuery)

        with(viewModel.regionSelectorUiState.value) {
            assertEquals("경기도", selectedSido)
            assertEquals("성남시", selectedSigungu)
            assertEquals("중앙동", selectedEupmyeondong)
        }
    }

    @Test
    fun `keyword not found clears previous selected region`() = runTest {
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

        viewModel.onRegionCandidateSelected(
            RegionSearchCandidateUiModel(
                sido = "경기도",
                sigungu = "수원시 장안구",
                eupmyeondong = null
            )
        )
        advanceUntilIdle()

        assertEquals("수원시", viewModel.regionSelectorUiState.value.selectedSigungu)

        viewModel.onSearchKeywordChanged("중안구")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        assertEquals("중안구", viewModel.searchKeyword.value)
        assertTrue(viewModel.uiState.value is RegionalGuideUiState.Empty)

        with(viewModel.regionSelectorUiState.value) {
            assertNull(selectedSido)
            assertNull(selectedSigungu)
            assertNull(selectedEupmyeondong)
        }
    }

    @Test
    fun `regional guide lookup exposes multiple guide candidates`() = runTest {
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
        assertEquals(2, state.candidates.size)
        assertEquals(
            "범서, 온양, 웅촌, 언양, 삼남, 상북, 온산, 청량, 서생",
            state.candidates.first().displayText
        )
    }

    @Test
    fun `guide candidate list is cleared when keyword changes after guide candidate search`() = runTest {
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
    fun `regional guide candidate selection shows selected guide`() = runTest {
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

        assertEquals("울주군", state.query)
        assertEquals(
            "범서, 온양, 웅촌, 언양, 삼남, 상북, 온산, 청량, 서생",
            state.guide.targetRegionName
        )

        with(viewModel.regionSelectorUiState.value) {
            assertEquals("울산광역시", selectedSido)
            assertEquals("울주군", selectedSigungu)
            assertNull(selectedEupmyeondong)
        }
    }

    @Test
    fun `favorite click stores regional guide favorite and observes state`() = runTest {
        val guide =
            RegionalDisposalGuide(
                region = Region(sido = "Sido", sigungu = "Sigungu"),
                targetRegionName = "Zone A",
                managementZoneName = "Management A",
                schedules = emptyList(),
            )
        val favoriteRepository = FakeFavoriteRepository()
        val snapshotRepository = FakeRegionalGuideFavoriteSnapshotRepository()
        val viewModel =
            createViewModel(
                regionRepository = FakeRegionRepository(resolvedRegion = guide.region),
                regionalGuideRepository = FakeRegionalDisposalGuideRepository(candidates = listOf(guide)),
                favoriteRepository = favoriteRepository,
                regionalGuideSnapshotRepository = snapshotRepository,
                regionalGuideFavoriteRepository =
                    FakeRegionalGuideFavoriteRepository(
                        favoriteRepository = favoriteRepository,
                        snapshotRepository = snapshotRepository,
                    ),
            )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("Sigungu")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        assertEquals(false, (viewModel.uiState.value as RegionalGuideUiState.Success).isFavorite)

        viewModel.onFavoriteClick()
        advanceUntilIdle()

        val snapshot = guide.toFavoriteSnapshot()
        assertEquals(true, (viewModel.uiState.value as RegionalGuideUiState.Success).isFavorite)
        assertEquals(true, favoriteRepository.isFavorite(FavoriteTargetType.REGIONAL_GUIDE, snapshot.targetId))
        assertEquals(snapshot, snapshotRepository.getSnapshot(snapshot.targetId))
    }

    @Test
    fun `favorite target id restores saved regional guide candidate`() = runTest {
        val region = Region(sido = "Sido", sigungu = "Sigungu")
        val firstGuide =
            RegionalDisposalGuide(
                region = region,
                targetRegionName = "Zone A",
                schedules = emptyList(),
            )
        val savedGuide =
            RegionalDisposalGuide(
                region = region,
                targetRegionName = "Zone B",
                managementZoneName = "Management B",
                schedules = emptyList(),
            )
        val snapshot = savedGuide.toFavoriteSnapshot()
        val favoriteRepository =
            FakeFavoriteRepository(
                initialFavorites =
                    listOf(
                        Favorite(
                            type = FavoriteTargetType.REGIONAL_GUIDE,
                            targetId = snapshot.targetId,
                            savedAtMillis = 1L,
                        ),
                    ),
            )
        val viewModel =
            createViewModel(
                regionalGuideRepository =
                    FakeRegionalDisposalGuideRepository(candidates = listOf(firstGuide, savedGuide)),
                favoriteRepository = favoriteRepository,
                regionalGuideSnapshotRepository =
                    FakeRegionalGuideFavoriteSnapshotRepository(snapshots = listOf(snapshot)),
            )
        advanceUntilIdle()

        viewModel.loadByFavoriteTargetId(snapshot.targetId)
        advanceUntilIdle()

        val state = viewModel.uiState.value as RegionalGuideUiState.Success
        assertEquals("Zone B", state.guide.targetRegionName)
        assertEquals(true, state.isFavorite)
    }

    private fun createViewModel(
        regionRepository: RegionRepository = FakeRegionRepository(),
        regionOptionsRepository: RegionOptionsRepository = FakeRegionOptionsRepository(),
        regionalGuideRepository: RegionalDisposalGuideRepository = FakeRegionalDisposalGuideRepository(),
        favoriteRepository: FavoriteRepository = FakeFavoriteRepository(),
        regionalGuideSnapshotRepository: RegionalGuideFavoriteSnapshotRepository =
            FakeRegionalGuideFavoriteSnapshotRepository(),
        regionalGuideFavoriteRepository: RegionalGuideFavoriteRepository =
            FakeRegionalGuideFavoriteRepository(
                favoriteRepository = favoriteRepository,
                snapshotRepository = regionalGuideSnapshotRepository,
            ),
    ): RegionalGuideViewModel {
        return RegionalGuideViewModel(
            resolveRegionFromKeywordUseCase = ResolveRegionFromKeywordUseCase(
                repository = regionRepository,
                regionOptionsRepository = regionOptionsRepository
            ),
            extractRegionFromAddressUseCase = ExtractRegionFromAddressUseCase(regionRepository),
            getRegionalDisposalGuideUseCase = GetRegionalDisposalGuideUseCase(
                repository = regionalGuideRepository,
                normalizeRegionalGuideQueryUseCase = NormalizeRegionalGuideQueryUseCase(),
                selectRegionalGuideCandidateUseCase = SelectRegionalGuideCandidateUseCase()
            ),
            getSidoOptionsUseCase = GetSidoOptionsUseCase(regionOptionsRepository),
            getSigunguOptionsUseCase = GetSigunguOptionsUseCase(regionOptionsRepository),
            getEupmyeondongOptionsUseCase = GetEupmyeondongOptionsUseCase(regionOptionsRepository),
            normalizeRegionForRegionalGuideUseCase = NormalizeRegionForRegionalGuideUseCase(
                regionOptionsRepository
            ),
            observeFavoriteUseCase = ObserveFavoriteUseCase(favoriteRepository),
            toggleRegionalGuideFavoriteUseCase = ToggleRegionalGuideFavoriteUseCase(regionalGuideFavoriteRepository),
            getRegionalGuideFavoriteSnapshotUseCase =
                GetRegionalGuideFavoriteSnapshotUseCase(regionalGuideSnapshotRepository),
        )
    }

    private fun sampleGuide(
        sido: String,
        sigungu: String?,
        targetRegionName: String?
    ): RegionalDisposalGuide {
        return RegionalDisposalGuide(
            region = Region(
                sido = sido,
                sigungu = sigungu
            ),
            targetRegionName = targetRegionName,
            schedules = emptyList()
        )
    }

    private class FakeRegionRepository(
        private val resolvedRegion: Region? = null,
        private val extractedRegion: Region? = null
    ) : RegionRepository {
        override fun extractRegionFromAddress(address: String): Region? = extractedRegion

        override suspend fun resolveRegionFromKeyword(keyword: String): Region? = resolvedRegion

        override suspend fun resolveRegionFromCoordinate(
            latitude: Double,
            longitude: Double
        ): Region? = null
    }

    private class FakeRegionOptionsRepository(
        private val sidoOptions: List<String> = listOf("서울특별시", "경기도"),
        private val sigunguOptionsBySido: Map<String, List<String>> = emptyMap(),
        private val eupmyeondongOptionsByRegion: Map<String, Map<String, List<String>>> = emptyMap(),
        private val delayedSigunguOptionsBySido: Map<String, CompletableDeferred<List<String>>> = emptyMap(),
        private val keywordRegions: List<Region> = emptyList(),
    ) : RegionOptionsRepository {

        override suspend fun getSidoOptions(): List<String> = sidoOptions

        override suspend fun getSigunguOptions(
            sido: String
        ): List<String> {
            return delayedSigunguOptionsBySido[sido]?.await()
                ?: sigunguOptionsBySido[sido].orEmpty()
        }

        override suspend fun getEupmyeondongOptions(
            sido: String,
            sigungu: String
        ): List<String> {
            return eupmyeondongOptionsByRegion[sido]
                ?.get(sigungu)
                .orEmpty()
        }

        override suspend fun findRegionsByEupmyeondongKeyword(
            keyword: String
        ): List<Region> {
            val exactMatches = keywordRegions.filter { region -> region.eupmyeondong == keyword }

            return exactMatches.ifEmpty {
                keywordRegions.filter { region ->
                    region.eupmyeondong?.startsWith(keyword) == true
                }
            }
        }

        override suspend fun findRegionsBySigunguKeyword(
            keyword: String
        ): List<Region> {
            val exactMatches = keywordRegions.filter { region -> region.sigungu == keyword }

            val prefixMatches = exactMatches.ifEmpty {
                keywordRegions.filter { region ->
                    region.sigungu?.startsWith(keyword) == true
                }
            }

            return prefixMatches.ifEmpty {
                keywordRegions.filter { region ->
                    region.sigungu?.contains(keyword) == true
                }
            }
        }

        override suspend fun normalizeRegionForRegionalGuide(
            region: Region
        ): Region {
            val selectedSido = region.sido
            val selectedSigungu = region.sigungu
                ?.takeIf { sigungu -> sigungu.isNotBlank() }
                ?.substringBefore(" ")
                ?.let { sigungu ->
                    if (sigungu.contains("시") && sigungu.contains("구")) {
                        sigungu.substringBefore("시") + "시"
                    } else {
                        sigungu
                    }
                }

            return region.copy(
                sido = selectedSido,
                sigungu = selectedSigungu
            )
        }
    }

    private class FakeRegionalDisposalGuideRepository(
        private val candidates: List<RegionalDisposalGuide> = emptyList()
    ) : RegionalDisposalGuideRepository {
        val queries = mutableListOf<RegionalGuideQuery>()

        override suspend fun getRegionalDisposalGuideCandidates(
            query: RegionalGuideQuery
        ): Result<List<RegionalDisposalGuide>> {
            queries += query
            return Result.success(candidates)
        }
    }

    private class FakeFavoriteRepository(
        initialFavorites: List<Favorite> = emptyList(),
    ) : FavoriteRepository {
        private val favorites = MutableStateFlow(initialFavorites)

        override fun observeFavorites(): Flow<List<Favorite>> = favorites

        override fun observeFavorite(
            type: FavoriteTargetType,
            targetId: String,
        ): Flow<Boolean> =
            favorites.map { items ->
                items.any { favorite -> favorite.type == type && favorite.targetId == targetId }
            }

        override suspend fun isFavorite(
            type: FavoriteTargetType,
            targetId: String,
        ): Boolean =
            favorites.value.any { favorite -> favorite.type == type && favorite.targetId == targetId }

        override suspend fun toggleFavorite(favorite: Favorite): Boolean {
            return if (isFavorite(favorite.type, favorite.targetId)) {
                removeFavorite(favorite.type, favorite.targetId)
                false
            } else {
                addFavorite(favorite)
                true
            }
        }

        override suspend fun addFavorite(favorite: Favorite) {
            favorites.value =
                favorites.value
                    .filterNot { it.type == favorite.type && it.targetId == favorite.targetId } + favorite
        }

        override suspend fun removeFavorite(
            type: FavoriteTargetType,
            targetId: String,
        ) {
            favorites.value =
                favorites.value.filterNot { favorite ->
                    favorite.type == type && favorite.targetId == targetId
                }
        }
    }

    private class FakeRegionalGuideFavoriteSnapshotRepository(
        snapshots: List<RegionalGuideFavoriteSnapshot> = emptyList(),
    ) : RegionalGuideFavoriteSnapshotRepository {
        private val snapshots = MutableStateFlow(snapshots)

        override fun observeSnapshots(): Flow<List<RegionalGuideFavoriteSnapshot>> = snapshots

        override suspend fun getSnapshot(targetId: String): RegionalGuideFavoriteSnapshot? =
            snapshots.value.firstOrNull { snapshot -> snapshot.targetId == targetId }

        override suspend fun upsertSnapshot(snapshot: RegionalGuideFavoriteSnapshot) {
            snapshots.value =
                snapshots.value
                    .filterNot { it.targetId == snapshot.targetId } + snapshot
        }

        override suspend fun deleteSnapshot(targetId: String) {
            snapshots.value = snapshots.value.filterNot { snapshot -> snapshot.targetId == targetId }
        }
    }

    private class FakeRegionalGuideFavoriteRepository(
        private val favoriteRepository: FavoriteRepository,
        private val snapshotRepository: RegionalGuideFavoriteSnapshotRepository,
    ) : RegionalGuideFavoriteRepository {
        override suspend fun toggleFavorite(snapshot: RegionalGuideFavoriteSnapshot): Boolean {
            val favorite =
                Favorite(
                    type = FavoriteTargetType.REGIONAL_GUIDE,
                    targetId = snapshot.targetId,
                    savedAtMillis = 1L,
                )

            return if (favoriteRepository.isFavorite(favorite.type, favorite.targetId)) {
                favoriteRepository.removeFavorite(favorite.type, favorite.targetId)
                snapshotRepository.deleteSnapshot(snapshot.targetId)
                false
            } else {
                favoriteRepository.addFavorite(favorite)
                snapshotRepository.upsertSnapshot(snapshot)
                true
            }
        }

        override suspend fun removeFavorite(targetId: String) {
            favoriteRepository.removeFavorite(FavoriteTargetType.REGIONAL_GUIDE, targetId)
            snapshotRepository.deleteSnapshot(targetId)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RegionalGuideMainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
