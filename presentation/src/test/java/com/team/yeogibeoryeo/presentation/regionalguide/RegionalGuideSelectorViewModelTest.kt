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
}


