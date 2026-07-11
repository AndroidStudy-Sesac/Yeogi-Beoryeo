package com.team.yeogibeoryeo.presentation.search

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteSnapshotRepository
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoritesUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveRegionalGuideFavoriteSnapshotsUseCase
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import com.team.yeogibeoryeo.domain.region.usecase.FindAdminDongCandidatesForLegalDongUseCase
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideQuery
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteSchedule
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteType
import com.team.yeogibeoryeo.domain.regionalguide.repository.RegionalDisposalGuideRepository
import com.team.yeogibeoryeo.domain.regionalguide.usecase.BuildHomeRegionalGuideSummaryUseCase
import com.team.yeogibeoryeo.domain.regionalguide.usecase.GetRegionalDisposalGuideUseCase
import com.team.yeogibeoryeo.domain.regionalguide.usecase.NormalizeRegionalGuideQueryUseCase
import com.team.yeogibeoryeo.domain.regionalguide.usecase.ObserveHomeRegionalGuideSummaryUseCase
import com.team.yeogibeoryeo.domain.regionalguide.usecase.SelectRegionalGuideCandidateUseCase
import com.team.yeogibeoryeo.presentation.search.model.HomeRegionalGuideSummaryUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeRegionalGuideSummaryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `지역 가이드 즐겨찾기가 없으면 즐겨찾기 없음 상태를 보여준다`() =
        runTest {
            val viewModel = createViewModel()
            collectState(viewModel)
            advanceUntilIdle()

            assertEquals(
                HomeRegionalGuideSummaryUiState.NoFavorite,
                viewModel.uiState.value,
            )
        }

    @Test
    fun `가장 최근 지역 가이드 즐겨찾기 요약을 보여준다`() =
        runTest {
            val snapshot =
                sampleSnapshot(
                    targetId = "regional-target",
                    region = Region(sido = "Sido", sigungu = "Sigungu", eupmyeondong = "Dong"),
                )
            val viewModel =
                createViewModel(
                    favoriteRepository =
                        FakeFavoriteRepository(
                            initialFavorites =
                                listOf(
                                    Favorite(
                                        type = FavoriteTargetType.REGIONAL_GUIDE,
                                        targetId = snapshot.targetId,
                                        savedAtMillis = 1L,
                                    ),
                                ),
                        ),
                    snapshotRepository =
                        FakeRegionalGuideFavoriteSnapshotRepository(
                            initialSnapshots = listOf(snapshot),
                        ),
                    regionalRepository =
                        FakeRegionalDisposalGuideRepository(
                            guides = listOf(sampleGuide(region = snapshot.region)),
                        ),
                )
            collectState(viewModel)
            advanceUntilIdle()

            val summary = viewModel.uiState.value as HomeRegionalGuideSummaryUiState.Summary
            assertEquals("regional-target", summary.targetId)
            assertEquals("Sido > Sigungu > Dong", summary.regionName)
            assertEquals("월, 화, 수, 목, 금, 토, 일", summary.disposalDays)
            assertEquals("18:00 이후", summary.disposalTime)
        }

    @Test
    fun `재시도하면 마지막 지역 가이드 조회를 다시 실행한다`() =
        runTest {
            val snapshot = sampleSnapshot(targetId = "regional-target")
            val regionalRepository =
                FakeRegionalDisposalGuideRepository(
                    guides = listOf(sampleGuide(region = snapshot.region)),
                )
            val viewModel =
                createViewModel(
                    favoriteRepository =
                        FakeFavoriteRepository(
                            initialFavorites =
                                listOf(
                                    Favorite(
                                        type = FavoriteTargetType.REGIONAL_GUIDE,
                                        targetId = snapshot.targetId,
                                        savedAtMillis = 1L,
                                    ),
                                ),
                        ),
                    snapshotRepository =
                        FakeRegionalGuideFavoriteSnapshotRepository(
                            initialSnapshots = listOf(snapshot),
                        ),
                    regionalRepository = regionalRepository,
                )
            collectState(viewModel)
            advanceUntilIdle()

            viewModel.retry()
            advanceUntilIdle()

            assertEquals(2, regionalRepository.requestCount)
        }

    @Test
    fun `같은 즐겨찾기 갱신 중에는 이전 요약을 유지한다`() =
        runTest {
            val snapshot = sampleSnapshot(targetId = "regional-target")
            val regionalRepository =
                FakeRegionalDisposalGuideRepository(
                    guides = listOf(sampleGuide(region = snapshot.region)),
                )
            val viewModel =
                createViewModel(
                    favoriteRepository =
                        FakeFavoriteRepository(
                            initialFavorites =
                                listOf(
                                    Favorite(
                                        type = FavoriteTargetType.REGIONAL_GUIDE,
                                        targetId = snapshot.targetId,
                                        savedAtMillis = 1L,
                                    ),
                                ),
                        ),
                    snapshotRepository =
                        FakeRegionalGuideFavoriteSnapshotRepository(
                            initialSnapshots = listOf(snapshot),
                        ),
                    regionalRepository = regionalRepository,
                )
            val states = mutableListOf<HomeRegionalGuideSummaryUiState>()
            collectState(viewModel, states)
            advanceUntilIdle()
            states.clear()

            viewModel.retry()
            advanceUntilIdle()

            assertFalse(states.any { state -> state is HomeRegionalGuideSummaryUiState.Loading })
            assertEquals(2, regionalRepository.requestCount)
        }

    @Test
    fun `일반쓰레기 요일이 미지정이면 대체 요일이 적용된 요약을 보여준다`() =
        runTest {
            val snapshot = sampleSnapshot(targetId = "regional-target")
            val viewModel =
                createViewModel(
                    favoriteRepository =
                        FakeFavoriteRepository(
                            initialFavorites =
                                listOf(
                                    Favorite(
                                        type = FavoriteTargetType.REGIONAL_GUIDE,
                                        targetId = snapshot.targetId,
                                        savedAtMillis = 1L,
                                    ),
                                ),
                        ),
                    snapshotRepository =
                        FakeRegionalGuideFavoriteSnapshotRepository(
                            initialSnapshots = listOf(snapshot),
                        ),
                    regionalRepository =
                        FakeRegionalDisposalGuideRepository(
                            guides = listOf(
                                sampleGuide(
                                    region = snapshot.region,
                                    schedules = listOf(sampleSchedule(days = "기타")),
                                ),
                            ),
                        ),
                )
            collectState(viewModel)
            advanceUntilIdle()

            assertEquals(
                HomeRegionalGuideSummaryUiState.Summary(
                    targetId = "regional-target",
                    regionName = "Sido > Sigungu > Dong",
                    disposalDays = null,
                    disposalTime = null,
                    hasDifferentDisposalDays = false,
                    hasDifferentDisposalTime = false,
                ),
                viewModel.uiState.value,
            )
        }

    private fun TestScope.collectState(viewModel: HomeRegionalGuideSummaryViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
    }

    private fun TestScope.collectState(
        viewModel: HomeRegionalGuideSummaryViewModel,
        states: MutableList<HomeRegionalGuideSummaryUiState>,
    ) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { state -> states += state }
        }
    }

    private fun createViewModel(
        favoriteRepository: FakeFavoriteRepository = FakeFavoriteRepository(),
        snapshotRepository: FakeRegionalGuideFavoriteSnapshotRepository =
            FakeRegionalGuideFavoriteSnapshotRepository(),
        regionalRepository: FakeRegionalDisposalGuideRepository =
            FakeRegionalDisposalGuideRepository(),
    ): HomeRegionalGuideSummaryViewModel =
        HomeRegionalGuideSummaryViewModel(
            observeHomeRegionalGuideSummaryUseCase =
                ObserveHomeRegionalGuideSummaryUseCase(
                    observeFavoritesUseCase = ObserveFavoritesUseCase(favoriteRepository),
                    observeRegionalGuideFavoriteSnapshotsUseCase =
                        ObserveRegionalGuideFavoriteSnapshotsUseCase(snapshotRepository),
                    getRegionalDisposalGuideUseCase =
                        GetRegionalDisposalGuideUseCase(
                            repository = regionalRepository,
                            normalizeRegionalGuideQueryUseCase = NormalizeRegionalGuideQueryUseCase(),
                            selectRegionalGuideCandidateUseCase = SelectRegionalGuideCandidateUseCase(),
                            findAdminDongCandidatesForLegalDongUseCase =
                                FindAdminDongCandidatesForLegalDongUseCase(FakeRegionOptionsRepository()),
                        ),
                    buildHomeRegionalGuideSummaryUseCase = BuildHomeRegionalGuideSummaryUseCase(),
                ),
        )

    private fun sampleSnapshot(
        targetId: String,
        region: Region = Region(sido = "Sido", sigungu = "Sigungu", eupmyeondong = "Dong"),
    ): RegionalGuideFavoriteSnapshot =
        RegionalGuideFavoriteSnapshot(
            targetId = targetId,
            region = region,
            targetRegionName = null,
            managementZoneName = null,
        )

    private fun sampleGuide(
        region: Region,
        schedules: List<RegionalWasteSchedule> =
            listOf(
                sampleSchedule(
                    days = "월, 화, 수, 목, 금, 토, 일",
                    start = "18:00",
                ),
            ),
    ): RegionalDisposalGuide =
        RegionalDisposalGuide(
            region = region,
            targetRegionName = region.eupmyeondong,
            schedules = schedules,
        )

    private fun sampleSchedule(
        days: String?,
        start: String? = null,
    ): RegionalWasteSchedule =
        RegionalWasteSchedule(
            wasteType = RegionalWasteType.GENERAL,
            disposalDays = days,
            disposalStartTime = start,
        )

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
                items.any { it.type == type && it.targetId == targetId }
            }

        override suspend fun isFavorite(
            type: FavoriteTargetType,
            targetId: String,
        ): Boolean =
            favorites.value.any { it.type == type && it.targetId == targetId }

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
                favorites.value.filterNot { it.type == type && it.targetId == targetId }
        }
    }

    private class FakeRegionalGuideFavoriteSnapshotRepository(
        initialSnapshots: List<RegionalGuideFavoriteSnapshot> = emptyList(),
    ) : RegionalGuideFavoriteSnapshotRepository {
        private val snapshots = MutableStateFlow(initialSnapshots)

        override fun observeSnapshots(): Flow<List<RegionalGuideFavoriteSnapshot>> = snapshots

        override suspend fun getSnapshot(targetId: String): RegionalGuideFavoriteSnapshot? =
            snapshots.value.firstOrNull { it.targetId == targetId }

        override suspend fun upsertSnapshot(snapshot: RegionalGuideFavoriteSnapshot) {
            snapshots.value = snapshots.value.filterNot { it.targetId == snapshot.targetId } + snapshot
        }

        override suspend fun deleteSnapshot(targetId: String) {
            snapshots.value = snapshots.value.filterNot { it.targetId == targetId }
        }
    }

    private class FakeRegionalDisposalGuideRepository(
        private val guides: List<RegionalDisposalGuide> = emptyList(),
    ) : RegionalDisposalGuideRepository {
        var requestCount = 0
            private set

        override suspend fun getRegionalDisposalGuideCandidates(
            query: RegionalGuideQuery,
        ): Result<List<RegionalDisposalGuide>> {
            requestCount += 1
            return Result.success(guides.filter { guide -> guide.region.sigungu == query.sigunguQuery })
        }
    }

    private class FakeRegionOptionsRepository : RegionOptionsRepository {
        override suspend fun getSidoOptions(): List<String> = emptyList()

        override suspend fun getSigunguOptions(sido: String): List<String> = emptyList()

        override suspend fun getEupmyeondongOptions(
            sido: String,
            sigungu: String,
        ): List<String> = emptyList()

        override suspend fun findRegionsByEupmyeondongKeyword(keyword: String): List<Region> =
            emptyList()

        override suspend fun findRegionsBySigunguKeyword(keyword: String): List<Region> =
            emptyList()

        override suspend fun normalizeRegionForRegionalGuide(region: Region): Region = region

        override suspend fun findAdminDongCandidatesForLegalDong(region: Region): List<Region> =
            emptyList()
    }
}
