package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import com.team.yeogibeoryeo.domain.region.repository.RegionRepository
import com.team.yeogibeoryeo.domain.region.usecase.ExtractRegionFromAddressUseCase
import com.team.yeogibeoryeo.domain.region.usecase.GetEupmyeondongOptionsUseCase
import com.team.yeogibeoryeo.domain.region.usecase.GetSidoOptionsUseCase
import com.team.yeogibeoryeo.domain.region.usecase.GetSigunguOptionsUseCase
import com.team.yeogibeoryeo.domain.region.usecase.ResolveRegionFromKeywordUseCase
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideQuery
import com.team.yeogibeoryeo.domain.regionalguide.repository.RegionalDisposalGuideRepository
import com.team.yeogibeoryeo.domain.regionalguide.usecase.GetRegionalDisposalGuideUseCase
import com.team.yeogibeoryeo.domain.regionalguide.usecase.NormalizeRegionalGuideQueryUseCase
import com.team.yeogibeoryeo.domain.regionalguide.usecase.SelectRegionalGuideCandidateUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
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

    private fun createViewModel(
        regionRepository: RegionRepository = FakeRegionRepository(),
        regionOptionsRepository: RegionOptionsRepository = FakeRegionOptionsRepository(),
        regionalGuideRepository: RegionalDisposalGuideRepository = FakeRegionalDisposalGuideRepository()
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
            getEupmyeondongOptionsUseCase = GetEupmyeondongOptionsUseCase(regionOptionsRepository)
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

    private class FakeRegionRepository : RegionRepository {
        override fun extractRegionFromAddress(address: String): Region? = null

        override suspend fun resolveRegionFromKeyword(keyword: String): Region? = null

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
        ): List<Region> = emptyList()

        override suspend fun findRegionsBySigunguKeyword(
            keyword: String
        ): List<Region> = emptyList()
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
