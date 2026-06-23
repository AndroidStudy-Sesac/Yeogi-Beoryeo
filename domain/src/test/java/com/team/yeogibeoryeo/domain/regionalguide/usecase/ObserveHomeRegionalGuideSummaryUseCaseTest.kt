package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteSnapshotRepository
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoritesUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveRegionalGuideFavoriteSnapshotsUseCase
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.HomeRegionalGuideSummaryResult
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupException
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideFailureReason
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupResult
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideQuery
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteSchedule
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteType
import com.team.yeogibeoryeo.domain.regionalguide.repository.RegionalDisposalGuideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveHomeRegionalGuideSummaryUseCaseTest {
    @Test
    fun `no regional guide favorite returns no favorite`() =
        runBlocking {
            val favoriteRepository =
                FakeFavoriteRepository(
                    initialFavorites =
                        listOf(
                            Favorite(
                                type = FavoriteTargetType.ITEM_GUIDE,
                                targetId = "item-guide",
                                savedAtMillis = 1L,
                            ),
                        ),
                )
            val useCase = createUseCase(favoriteRepository = favoriteRepository)

            assertEquals(HomeRegionalGuideSummaryResult.NoFavorite, useCase().first())
        }

    @Test
    fun `latest regional guide favorite is selected`() =
        runBlocking {
            val oldSnapshot = sampleSnapshot(targetId = "old", sigungu = "중구")
            val latestSnapshot = sampleSnapshot(targetId = "latest", sigungu = "노원구")
            val favoriteRepository =
                FakeFavoriteRepository(
                    initialFavorites =
                        listOf(
                            Favorite(
                                type = FavoriteTargetType.REGIONAL_GUIDE,
                                targetId = oldSnapshot.targetId,
                                savedAtMillis = 1L,
                            ),
                            Favorite(
                                type = FavoriteTargetType.REGIONAL_GUIDE,
                                targetId = latestSnapshot.targetId,
                                savedAtMillis = 2L,
                            ),
                        ),
                )
            val snapshotRepository =
                FakeRegionalGuideFavoriteSnapshotRepository(
                    initialSnapshots = listOf(oldSnapshot, latestSnapshot),
                )
            val regionalRepository =
                FakeRegionalDisposalGuideRepository(
                    candidates =
                        listOf(
                            sampleGuide(region = latestSnapshot.region),
                        ),
                )
            val useCase =
                createUseCase(
                    favoriteRepository = favoriteRepository,
                    snapshotRepository = snapshotRepository,
                    regionalRepository = regionalRepository,
                )

            val result = useCase().drop(1).first()

            assertTrue(result is HomeRegionalGuideSummaryResult.Success)
            assertEquals("latest", (result as HomeRegionalGuideSummaryResult.Success).summary.targetId)
            assertEquals(listOf("노원구"), regionalRepository.requestedSigungu)
        }

    @Test
    fun `missing snapshot returns favorite restore failed`() =
        runBlocking {
            val favoriteRepository =
                FakeFavoriteRepository(
                    initialFavorites =
                        listOf(
                            Favorite(
                                type = FavoriteTargetType.REGIONAL_GUIDE,
                                targetId = "missing",
                                savedAtMillis = 1L,
                            ),
                        ),
                )
            val useCase = createUseCase(favoriteRepository = favoriteRepository)

            val result = useCase().first()

            assertEquals(HomeRegionalGuideSummaryResult.FavoriteRestoreFailed("missing"), result)
        }

    @Test
    fun `multiple restored candidates are not selected arbitrarily`() =
        runBlocking {
            val snapshot = sampleSnapshot(targetId = "regional")
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
            val useCase =
                createUseCase(
                    favoriteRepository = favoriteRepository,
                    snapshotRepository = FakeRegionalGuideFavoriteSnapshotRepository(listOf(snapshot)),
                    regionalRepository =
                        FakeRegionalDisposalGuideRepository(
                            candidates =
                                listOf(
                                    sampleGuide(region = snapshot.region, managementZoneName = "노은2동"),
                                    sampleGuide(region = snapshot.region, managementZoneName = "노은3동"),
                                ),
                        ),
                )

            val result = useCase().drop(1).first()

            assertEquals(HomeRegionalGuideSummaryResult.FavoriteRestoreFailed("regional"), result)
        }

    @Test
    fun `no today schedule returns no today schedule`() =
        runBlocking {
            val snapshot = sampleSnapshot(targetId = "regional")
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
            val useCase =
                createUseCase(
                    favoriteRepository = favoriteRepository,
                    snapshotRepository = FakeRegionalGuideFavoriteSnapshotRepository(listOf(snapshot)),
                    regionalRepository =
                        FakeRegionalDisposalGuideRepository(
                            candidates =
                                listOf(
                                    sampleGuide(
                                        region = snapshot.region,
                                        schedules =
                                            listOf(
                                                sampleSchedule(
                                                    type = RegionalWasteType.GENERAL,
                                                    days = "해당없음",
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                )

            val result = useCase().drop(1).first()

            assertEquals(
                HomeRegionalGuideSummaryResult.NoTodaySchedule(
                    targetId = "regional",
                    regionName = "서울특별시 > 노원구 > 하계동",
                ),
                result,
            )
        }

    @Test
    fun `unknown weekday returns schedule needs confirmation`() =
        runBlocking {
            val snapshot = sampleSnapshot(targetId = "regional")
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
            val useCase =
                createUseCase(
                    favoriteRepository = favoriteRepository,
                    snapshotRepository = FakeRegionalGuideFavoriteSnapshotRepository(listOf(snapshot)),
                    regionalRepository =
                        FakeRegionalDisposalGuideRepository(
                            candidates =
                                listOf(
                                    sampleGuide(
                                        region = snapshot.region,
                                        schedules =
                                            listOf(
                                                sampleSchedule(
                                                    type = RegionalWasteType.GENERAL,
                                                    days = "기타",
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                )

            val result = useCase().drop(1).first()

            assertEquals(
                HomeRegionalGuideSummaryResult.ScheduleNeedsConfirmation(
                    targetId = "regional",
                    regionName = "서울특별시 > 노원구 > 하계동",
                ),
                result,
            )
        }

    @Test
    fun `repository failure returns failure state`() =
        runBlocking {
            val snapshot = sampleSnapshot(targetId = "regional")
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
            val useCase =
                createUseCase(
                    favoriteRepository = favoriteRepository,
                    snapshotRepository = FakeRegionalGuideFavoriteSnapshotRepository(listOf(snapshot)),
                    regionalRepository =
                        FakeRegionalDisposalGuideRepository(
                            result =
                                Result.failure(
                                    RegionalGuideLookupException(
                                        reason = RegionalGuideFailureReason.NETWORK,
                                    ),
                                ),
                        ),
                )

            val result = useCase().drop(1).first()

            assertEquals(
                HomeRegionalGuideSummaryResult.Failure(
                    targetId = "regional",
                    regionName = "서울특별시 > 노원구 > 하계동",
                    reason = RegionalGuideFailureReason.NETWORK,
                ),
                result,
            )
        }

    @Test
    fun `favorite changes refresh summary`() =
        runBlocking {
            val firstSnapshot = sampleSnapshot(targetId = "first", sigungu = "중구")
            val secondSnapshot = sampleSnapshot(targetId = "second", sigungu = "노원구")
            val favoriteRepository = FakeFavoriteRepository()
            val snapshotRepository =
                FakeRegionalGuideFavoriteSnapshotRepository(
                    initialSnapshots = listOf(firstSnapshot, secondSnapshot),
                )
            val useCase =
                createUseCase(
                    favoriteRepository = favoriteRepository,
                    snapshotRepository = snapshotRepository,
                    regionalRepository =
                        FakeRegionalDisposalGuideRepository(
                            candidates =
                                listOf(
                                    sampleGuide(region = firstSnapshot.region),
                                    sampleGuide(region = secondSnapshot.region),
                                ),
                        ),
                )

            favoriteRepository.addFavorite(
                Favorite(
                    type = FavoriteTargetType.REGIONAL_GUIDE,
                    targetId = firstSnapshot.targetId,
                    savedAtMillis = 1L,
                ),
            )
            val firstResult = useCase().drop(1).first() as HomeRegionalGuideSummaryResult.Success

            favoriteRepository.addFavorite(
                Favorite(
                    type = FavoriteTargetType.REGIONAL_GUIDE,
                    targetId = secondSnapshot.targetId,
                    savedAtMillis = 2L,
                ),
            )
            val secondResult = useCase().drop(1).first() as HomeRegionalGuideSummaryResult.Success

            assertEquals("first", firstResult.summary.targetId)
            assertEquals("second", secondResult.summary.targetId)
        }

    private fun createUseCase(
        favoriteRepository: FakeFavoriteRepository = FakeFavoriteRepository(),
        snapshotRepository: FakeRegionalGuideFavoriteSnapshotRepository =
            FakeRegionalGuideFavoriteSnapshotRepository(),
        regionalRepository: FakeRegionalDisposalGuideRepository = FakeRegionalDisposalGuideRepository(),
    ): ObserveHomeRegionalGuideSummaryUseCase =
        ObserveHomeRegionalGuideSummaryUseCase(
            observeFavoritesUseCase = ObserveFavoritesUseCase(favoriteRepository),
            observeRegionalGuideFavoriteSnapshotsUseCase =
                ObserveRegionalGuideFavoriteSnapshotsUseCase(snapshotRepository),
            getRegionalDisposalGuideUseCase =
                GetRegionalDisposalGuideUseCase(
                    repository = regionalRepository,
                    normalizeRegionalGuideQueryUseCase = NormalizeRegionalGuideQueryUseCase(),
                    selectRegionalGuideCandidateUseCase = SelectRegionalGuideCandidateUseCase(),
                ),
            getTodayRegionalWasteSummaryUseCase = GetTodayRegionalWasteSummaryUseCase(),
        )

    private fun sampleSnapshot(
        targetId: String,
        sigungu: String = "노원구",
    ): RegionalGuideFavoriteSnapshot =
        RegionalGuideFavoriteSnapshot(
            targetId = targetId,
            region = Region(sido = "서울특별시", sigungu = sigungu, eupmyeondong = "하계동"),
            targetRegionName = null,
            managementZoneName = null,
        )

    private fun sampleGuide(
        region: Region = Region(sido = "서울특별시", sigungu = "노원구", eupmyeondong = "하계동"),
        managementZoneName: String? = null,
        targetRegionName: String? = region.eupmyeondong,
        schedules: List<RegionalWasteSchedule> =
            listOf(
                sampleSchedule(
                    type = RegionalWasteType.GENERAL,
                    days = "월, 화, 수, 목, 금, 토, 일",
                    start = "18:00",
                ),
            ),
    ): RegionalDisposalGuide =
        RegionalDisposalGuide(
            region = region,
            managementZoneName = managementZoneName,
            targetRegionName = targetRegionName,
            schedules = schedules,
        )

    private fun sampleSchedule(
        type: RegionalWasteType,
        days: String?,
        start: String? = null,
    ): RegionalWasteSchedule =
        RegionalWasteSchedule(
            wasteType = type,
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
        private val candidates: List<RegionalDisposalGuide> = emptyList(),
        private val result: Result<List<RegionalDisposalGuide>>? = null,
    ) : RegionalDisposalGuideRepository {
        val requestedSigungu = mutableListOf<String?>()

        override suspend fun getRegionalDisposalGuideCandidates(
            query: RegionalGuideQuery,
        ): Result<List<RegionalDisposalGuide>> {
            requestedSigungu += query.sigunguQuery
            return result ?: Result.success(candidates.filter { it.region.sigungu == query.sigunguQuery })
        }
    }
}
