package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteKey
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
import com.team.yeogibeoryeo.domain.region.usecase.ClassifyRegionSearchInputUseCase
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
class RegionalGuideAddressSearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = RegionalGuideMainDispatcherRule()

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
    fun `keyword search with road address uses address lookup and keeps original input`() = runTest {
        val regionRepository = FakeRegionRepository(
            extractedRegion = Region(
                sido = "서울특별시",
                sigungu = "중구",
                eupmyeondong = null
            )
        )
        val regionalGuideRepository = FakeRegionalDisposalGuideRepository(
            candidates = listOf(
                sampleGuide(
                    sido = "서울특별시",
                    sigungu = "중구",
                    targetRegionName = "중구 전체"
                )
            )
        )
        val viewModel = createViewModel(
            regionRepository = regionRepository,
            regionOptionsRepository = FakeRegionOptionsRepository(
                sigunguOptionsBySido = mapOf(
                    "서울특별시" to listOf("중구")
                )
            ),
            regionalGuideRepository = regionalGuideRepository
        )
        advanceUntilIdle()

        val address = "서울시 중구 남대문로 63"
        viewModel.onSearchKeywordChanged(address)
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        assertEquals(listOf(address), regionRepository.extractedAddresses)
        assertEquals(emptyList<String>(), regionRepository.resolvedKeywords)
        assertEquals(address, viewModel.searchKeyword.value)
        assertEquals("중구", regionalGuideRepository.queries.single().sigunguQuery)

        with(viewModel.regionSelectorUiState.value) {
            assertEquals("서울특별시", selectedSido)
            assertEquals("중구", selectedSigungu)
            assertNull(selectedEupmyeondong)
        }
    }

    @Test
    fun `keyword search with parenthesized road address uses address lookup`() = runTest {
        val regionRepository = FakeRegionRepository(
            extractedRegion = Region(
                sido = "서울특별시",
                sigungu = "중구",
                eupmyeondong = null
            )
        )
        val viewModel = createViewModel(
            regionRepository = regionRepository,
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(
                candidates = listOf(
                    sampleGuide(
                        sido = "서울특별시",
                        sigungu = "중구",
                        targetRegionName = "중구 전체"
                    )
                )
            )
        )
        advanceUntilIdle()

        val address = "서울시 중구 남대문로 63 (남대문로2가한진빌딩)"
        viewModel.onSearchKeywordChanged(address)
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        assertEquals(listOf(address), regionRepository.extractedAddresses)
        assertTrue(viewModel.uiState.value is RegionalGuideUiState.Success)
    }

    @Test
    fun `retry after address keyword search uses last address lookup`() = runTest {
        val regionRepository = FakeRegionRepository(
            extractedRegion = Region(
                sido = "서울특별시",
                sigungu = "중구"
            )
        )
        val regionalGuideRepository = FakeRegionalDisposalGuideRepository(
            candidates = listOf(
                sampleGuide(
                    sido = "서울특별시",
                    sigungu = "중구",
                    targetRegionName = "중구 전체"
                )
            )
        )
        val viewModel = createViewModel(
            regionRepository = regionRepository,
            regionalGuideRepository = regionalGuideRepository
        )
        advanceUntilIdle()

        val address = "서울시 중구 남대문로 63"
        viewModel.onSearchKeywordChanged(address)
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("새검색")
        viewModel.retryLastRequest()
        advanceUntilIdle()

        assertEquals(listOf(address, address), regionRepository.extractedAddresses)
        assertEquals(2, regionalGuideRepository.queries.size)
        assertEquals("새검색", viewModel.searchKeyword.value)
    }

    @Test
    fun `address-like keyword search without parsed region fails safely`() = runTest {
        val regionRepository = FakeRegionRepository(extractedRegion = null)
        val viewModel = createViewModel(
            regionRepository = regionRepository,
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(
                candidates = listOf(
                    sampleGuide(
                        sido = "경기도",
                        sigungu = "수원시",
                        targetRegionName = "수원시 전체"
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.onRegionCandidateSelected(
            RegionSearchCandidateUiModel(
                sido = "경기도",
                sigungu = "수원시",
                eupmyeondong = null
            )
        )
        advanceUntilIdle()

        assertEquals("수원시", viewModel.regionSelectorUiState.value.selectedSigungu)

        val address = "서울시 중구 남대문로 63"
        viewModel.onSearchKeywordChanged(address)
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        assertEquals(listOf(address), regionRepository.extractedAddresses)
        assertTrue(viewModel.uiState.value is RegionalGuideUiState.Empty)
        assertEquals(address, viewModel.searchKeyword.value)

        with(viewModel.regionSelectorUiState.value) {
            assertNull(selectedSido)
            assertNull(selectedSigungu)
            assertNull(selectedEupmyeondong)
        }
    }

    @Test
    fun `region keyword with sigungu only keeps keyword search flow`() = runTest {
        val regionRepository = FakeRegionRepository(
            resolvedRegion = Region(
                sigungu = "중구"
            )
        )
        val viewModel = createViewModel(
            regionRepository = regionRepository,
            regionOptionsRepository = FakeRegionOptionsRepository(
                keywordRegions = listOf(
                    Region(sido = "서울특별시", sigungu = "중구"),
                    Region(sido = "대구광역시", sigungu = "중구")
                )
            )
        )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("중구")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        assertEquals(listOf("중구"), regionRepository.resolvedKeywords)
        assertEquals(emptyList<String>(), regionRepository.extractedAddresses)
        assertTrue(viewModel.uiState.value is RegionalGuideUiState.Ambiguous)
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

}


