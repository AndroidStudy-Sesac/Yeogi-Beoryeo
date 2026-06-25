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
class RegionalGuideCandidateSelectionViewModelTest {

    @get:Rule
    val mainDispatcherRule = RegionalGuideMainDispatcherRule()

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

        assertEquals("울주군", viewModel.searchKeyword.value)
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

}


