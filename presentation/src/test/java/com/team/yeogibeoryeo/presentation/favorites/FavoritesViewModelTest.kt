package com.team.yeogibeoryeo.presentation.favorites

import com.team.yeogibeoryeo.domain.favorite.model.CollectionSpotFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteSnapshotRepository
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteSnapshotRepository
import com.team.yeogibeoryeo.domain.favorite.usecase.GetCollectionSpotFavoriteSnapshotUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveCollectionSpotFavoriteSnapshotsUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoritesUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveRegionalGuideFavoriteSnapshotsUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.RemoveCollectionSpotFavoriteUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.RemoveRegionalGuideFavoriteUseCase
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalInstruction
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.model.DisposalSubCategory
import com.team.yeogibeoryeo.domain.item.repository.DisposalItemGuideRepository
import com.team.yeogibeoryeo.domain.item.usecase.GetDisposalItemGuideUseCase
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.presentation.favorites.mapper.FavoriteCollectionSpotUiMapper
import com.team.yeogibeoryeo.presentation.favorites.mapper.FavoriteItemGuideUiMapper
import com.team.yeogibeoryeo.presentation.favorites.mapper.FavoriteRegionalGuideUiMapper
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteCollectionSpotMapMoveRequest
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteTab
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteUiModel
import com.team.yeogibeoryeo.presentation.search.MainDispatcherRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `즐겨찾기 원본 가이드를 조회해 UI 모델로 변환한다`() =
        runTest {
            val guide =
                sampleGuide(
                    id = "paper-pack",
                    name = "종이팩",
                    subCategory = DisposalSubCategory.MILK_CARTON,
                )
            val viewModel =
                createViewModel(
                    favoriteRepository =
                        FakeFavoriteRepository(
                            initialFavorites =
                                listOf(
                                    Favorite(
                                        type = FavoriteTargetType.ITEM_GUIDE,
                                        targetId = guide.id,
                                        savedAtMillis = 1L,
                                    ),
                                ),
                        ),
                    itemRepository = FakeItemRepository(guides = listOf(guide)),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            assertEquals(
                listOf(
                    FavoriteUiModel(
                        type = FavoriteTargetType.ITEM_GUIDE,
                        targetId = "paper-pack",
                        title = "종이팩",
                        subtitle = "우유팩",
                    ),
                ),
                viewModel.uiState.value.itemGuideFavorites,
            )
        }

    @Test
    fun `원본 가이드를 찾을 수 없는 즐겨찾기는 UI 목록에서 제외한다`() =
        runTest {
            val viewModel =
                createViewModel(
                    favoriteRepository =
                        FakeFavoriteRepository(
                            initialFavorites =
                                listOf(
                                    Favorite(
                                        type = FavoriteTargetType.ITEM_GUIDE,
                                        targetId = "missing-guide",
                                        savedAtMillis = 1L,
                                    ),
                                ),
                        ),
                    itemRepository = FakeItemRepository(guides = emptyList()),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            assertEquals(emptyList<FavoriteUiModel>(), viewModel.uiState.value.itemGuideFavorites)
        }

    @Test
    fun `수거 장소 스냅샷을 조회해 장소 즐겨찾기 UI 모델로 변환한다`() =
        runTest {
            val snapshot =
                CollectionSpotFavoriteSnapshot(
                    targetId = "spot-1",
                    name = "폐건전지 수거함",
                    type = CollectionSpotType.BATTERY_BIN,
                    address = "서울특별시 영등포구 문래동",
                    detailLocation = "주민센터 앞",
                    coordinate = Coordinate(latitude = 37.5, longitude = 126.9),
                )
            val viewModel =
                createViewModel(
                    favoriteRepository =
                        FakeFavoriteRepository(
                            initialFavorites =
                                listOf(
                                    Favorite(
                                        type = FavoriteTargetType.COLLECTION_SPOT,
                                        targetId = snapshot.targetId,
                                        savedAtMillis = 1L,
                                    ),
                                ),
                        ),
                    itemRepository = FakeItemRepository(guides = emptyList()),
                    collectionSpotSnapshotRepository =
                        FakeCollectionSpotFavoriteSnapshotRepository(
                            snapshots = listOf(snapshot),
                        ),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            assertEquals(
                listOf(
                    FavoriteUiModel(
                        type = FavoriteTargetType.COLLECTION_SPOT,
                        targetId = "spot-1",
                        title = "폐건전지 수거함",
                        subtitle = "폐건전지 · 서울특별시 영등포구 문래동 · 주민센터 앞",
                        collectionSpotMapMoveRequest =
                            FavoriteCollectionSpotMapMoveRequest(
                                targetId = "spot-1",
                                name = "폐건전지 수거함",
                                type = CollectionSpotType.BATTERY_BIN,
                                address = "서울특별시 영등포구 문래동",
                                detailLocation = "주민센터 앞",
                                latitude = 37.5,
                                longitude = 126.9,
                            ),
                    ),
                ),
                viewModel.uiState.value.collectionSpotFavorites,
            )
        }

    @Test
    fun `좌표가 없는 수거 장소 즐겨찾기는 지도 이동 요청 없이 UI 모델로 변환한다`() =
        runTest {
            val snapshot =
                CollectionSpotFavoriteSnapshot(
                    targetId = "spot-without-coordinate",
                    name = "좌표 없는 수거함",
                    type = CollectionSpotType.OTHER,
                    address = "서울특별시 영등포구",
                    detailLocation = null,
                    coordinate = null,
                )
            val viewModel =
                createViewModel(
                    favoriteRepository =
                        FakeFavoriteRepository(
                            initialFavorites =
                                listOf(
                                    Favorite(
                                        type = FavoriteTargetType.COLLECTION_SPOT,
                                        targetId = snapshot.targetId,
                                        savedAtMillis = 1L,
                                    ),
                                ),
                        ),
                    itemRepository = FakeItemRepository(guides = emptyList()),
                    collectionSpotSnapshotRepository =
                        FakeCollectionSpotFavoriteSnapshotRepository(
                            snapshots = listOf(snapshot),
                        ),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            assertEquals(
                null,
                viewModel.uiState.value.collectionSpotFavorites.single().collectionSpotMapMoveRequest,
            )
        }

    @Test
    fun `스냅샷이 없는 수거 장소 즐겨찾기는 장소 UI 목록에서 제외한다`() =
        runTest {
            val viewModel =
                createViewModel(
                    favoriteRepository =
                        FakeFavoriteRepository(
                            initialFavorites =
                                listOf(
                                    Favorite(
                                        type = FavoriteTargetType.COLLECTION_SPOT,
                                        targetId = "missing-snapshot",
                                        savedAtMillis = 1L,
                                    ),
                                ),
                        ),
                    itemRepository = FakeItemRepository(guides = emptyList()),
                    collectionSpotSnapshotRepository = FakeCollectionSpotFavoriteSnapshotRepository(),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            viewModel.selectTab(FavoriteTab.COLLECTION_SPOT)
            advanceUntilIdle()

            assertEquals(emptyList<FavoriteUiModel>(), viewModel.uiState.value.selectedFavorites)
        }

    @Test
    fun `수거 장소 즐겨찾기는 Favorite 저장 순서를 기준으로 표시한다`() =
        runTest {
            val olderSnapshot =
                CollectionSpotFavoriteSnapshot(
                    targetId = "spot-older",
                    name = "오래된 수거함",
                    type = CollectionSpotType.BATTERY_BIN,
                    address = "서울특별시 영등포구",
                    detailLocation = null,
                    coordinate = null,
                )
            val newerSnapshot =
                CollectionSpotFavoriteSnapshot(
                    targetId = "spot-newer",
                    name = "최근 수거함",
                    type = CollectionSpotType.PHONE_DROP_OFF,
                    address = "서울특별시 마포구",
                    detailLocation = null,
                    coordinate = null,
                )
            val viewModel =
                createViewModel(
                    favoriteRepository =
                        FakeFavoriteRepository(
                            initialFavorites =
                                listOf(
                                    Favorite(
                                        type = FavoriteTargetType.COLLECTION_SPOT,
                                        targetId = newerSnapshot.targetId,
                                        savedAtMillis = 2L,
                                    ),
                                    Favorite(
                                        type = FavoriteTargetType.COLLECTION_SPOT,
                                        targetId = olderSnapshot.targetId,
                                        savedAtMillis = 1L,
                                    ),
                                ),
                        ),
                    itemRepository = FakeItemRepository(guides = emptyList()),
                    collectionSpotSnapshotRepository =
                        FakeCollectionSpotFavoriteSnapshotRepository(
                            snapshots = listOf(olderSnapshot, newerSnapshot),
                        ),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            assertEquals(
                listOf("spot-newer", "spot-older"),
                viewModel.uiState.value.collectionSpotFavorites.map { it.targetId },
            )
        }

    @Test
    fun `장소 즐겨찾기 해제 시 공통 Favorite와 스냅샷을 삭제하고 UI 목록을 갱신한다`() =
        runTest {
            val snapshot =
                CollectionSpotFavoriteSnapshot(
                    targetId = "spot-1",
                    name = "폐건전지 수거함",
                    type = CollectionSpotType.BATTERY_BIN,
                    address = "서울특별시 영등포구 문래동",
                    detailLocation = "주민센터 앞",
                    coordinate = Coordinate(latitude = 37.5, longitude = 126.9),
                )
            val favoriteRepository =
                FakeFavoriteRepository(
                    initialFavorites =
                        listOf(
                            Favorite(
                                type = FavoriteTargetType.COLLECTION_SPOT,
                                targetId = snapshot.targetId,
                                savedAtMillis = 1L,
                            ),
                        ),
                )
            val snapshotRepository =
                FakeCollectionSpotFavoriteSnapshotRepository(
                    snapshots = listOf(snapshot),
                )
            val viewModel =
                createViewModel(
                    favoriteRepository = favoriteRepository,
                    itemRepository = FakeItemRepository(guides = emptyList()),
                    collectionSpotSnapshotRepository = snapshotRepository,
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            viewModel.selectTab(FavoriteTab.COLLECTION_SPOT)
            advanceUntilIdle()
            assertEquals(1, viewModel.uiState.value.selectedFavorites.size)

            viewModel.removeCollectionSpotFavorite(snapshot.targetId)
            advanceUntilIdle()

            assertEquals(emptyList<FavoriteUiModel>(), viewModel.uiState.value.selectedFavorites)
            assertEquals(false, favoriteRepository.isFavorite(FavoriteTargetType.COLLECTION_SPOT, snapshot.targetId))
            assertEquals(emptyList<CollectionSpotFavoriteSnapshot>(), snapshotRepository.snapshots.value)
        }

    @Test
    fun `탭을 변경하면 선택된 탭 상태를 반영한다`() =
        runTest {
            val viewModel =
                createViewModel(
                    favoriteRepository = FakeFavoriteRepository(),
                    itemRepository = FakeItemRepository(guides = emptyList()),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.selectTab(FavoriteTab.COLLECTION_SPOT)
            advanceUntilIdle()

            assertEquals(FavoriteTab.COLLECTION_SPOT, viewModel.uiState.value.selectedTab)
        }

    @Test
    fun `regional guide favorites are mapped from snapshots in favorite order`() =
        runTest {
            val olderSnapshot =
                RegionalGuideFavoriteSnapshot(
                    targetId = "regional-guide-v1|4:서울시2:중구-1:4:권역A",
                    region = Region(sido = "서울시", sigungu = "중구"),
                    targetRegionName = "권역A",
                    managementZoneName = "관리구역A",
                )
            val newerSnapshot =
                RegionalGuideFavoriteSnapshot(
                    targetId = "regional-guide-v1|4:서울시2:중구-1:4:권역B",
                    region = Region(sido = "서울시", sigungu = "중구"),
                    targetRegionName = "권역B",
                    managementZoneName = "관리구역B",
                )
            val viewModel =
                createViewModel(
                    favoriteRepository =
                        FakeFavoriteRepository(
                            initialFavorites =
                                listOf(
                                    Favorite(
                                        type = FavoriteTargetType.REGIONAL_GUIDE,
                                        targetId = newerSnapshot.targetId,
                                        savedAtMillis = 2L,
                                    ),
                                    Favorite(
                                        type = FavoriteTargetType.REGIONAL_GUIDE,
                                        targetId = olderSnapshot.targetId,
                                        savedAtMillis = 1L,
                                    ),
                                ),
                        ),
                    itemRepository = FakeItemRepository(guides = emptyList()),
                    regionalGuideSnapshotRepository =
                        FakeRegionalGuideFavoriteSnapshotRepository(
                            snapshots = listOf(olderSnapshot, newerSnapshot),
                        ),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            viewModel.selectTab(FavoriteTab.REGIONAL_GUIDE)
            advanceUntilIdle()

            assertEquals(
                listOf(newerSnapshot.targetId, olderSnapshot.targetId),
                viewModel.uiState.value.selectedFavorites.map { it.targetId },
            )
            assertEquals("서울시 > 중구", viewModel.uiState.value.selectedFavorites.first().title)
            assertEquals("권역B · 관리구역B", viewModel.uiState.value.selectedFavorites.first().subtitle)
        }

    @Test
    fun `regional guide favorite remove deletes favorite and snapshot`() =
        runTest {
            val snapshot =
                RegionalGuideFavoriteSnapshot(
                    targetId = "regional-guide-v1|4:서울시2:중구-1:4:권역A",
                    region = Region(sido = "서울시", sigungu = "중구"),
                    targetRegionName = "권역A",
                    managementZoneName = "관리구역A",
                )
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
            val snapshotRepository =
                FakeRegionalGuideFavoriteSnapshotRepository(snapshots = listOf(snapshot))
            val viewModel =
                createViewModel(
                    favoriteRepository = favoriteRepository,
                    itemRepository = FakeItemRepository(guides = emptyList()),
                    regionalGuideSnapshotRepository = snapshotRepository,
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            viewModel.selectTab(FavoriteTab.REGIONAL_GUIDE)
            advanceUntilIdle()
            assertEquals(1, viewModel.uiState.value.selectedFavorites.size)

            viewModel.removeRegionalGuideFavorite(snapshot.targetId)
            advanceUntilIdle()

            assertEquals(emptyList<FavoriteUiModel>(), viewModel.uiState.value.selectedFavorites)
            assertEquals(false, favoriteRepository.isFavorite(FavoriteTargetType.REGIONAL_GUIDE, snapshot.targetId))
            assertEquals(emptyList<RegionalGuideFavoriteSnapshot>(), snapshotRepository.snapshots.value)
        }

    private fun createViewModel(
        favoriteRepository: FakeFavoriteRepository,
        itemRepository: FakeItemRepository,
        collectionSpotSnapshotRepository: FakeCollectionSpotFavoriteSnapshotRepository =
            FakeCollectionSpotFavoriteSnapshotRepository(),
        regionalGuideSnapshotRepository: FakeRegionalGuideFavoriteSnapshotRepository =
            FakeRegionalGuideFavoriteSnapshotRepository(),
    ): FavoritesViewModel =
        FavoritesViewModel(
            observeFavoritesUseCase = ObserveFavoritesUseCase(favoriteRepository),
            observeCollectionSpotFavoriteSnapshotsUseCase =
                ObserveCollectionSpotFavoriteSnapshotsUseCase(collectionSpotSnapshotRepository),
            observeRegionalGuideFavoriteSnapshotsUseCase =
                ObserveRegionalGuideFavoriteSnapshotsUseCase(regionalGuideSnapshotRepository),
            removeCollectionSpotFavoriteUseCase =
                RemoveCollectionSpotFavoriteUseCase(
                    collectionSpotFavoriteRepository =
                        FakeCollectionSpotFavoriteRepository(
                            favoriteRepository = favoriteRepository,
                            snapshotRepository = collectionSpotSnapshotRepository,
                        ),
                ),
            removeRegionalGuideFavoriteUseCase =
                RemoveRegionalGuideFavoriteUseCase(
                    repository =
                        FakeRegionalGuideFavoriteRepository(
                            favoriteRepository = favoriteRepository,
                            snapshotRepository = regionalGuideSnapshotRepository,
                        ),
                ),
            itemGuideUiMapper = FavoriteItemGuideUiMapper(GetDisposalItemGuideUseCase(itemRepository)),
            collectionSpotUiMapper =
                FavoriteCollectionSpotUiMapper(
                    GetCollectionSpotFavoriteSnapshotUseCase(collectionSpotSnapshotRepository),
                ),
            regionalGuideUiMapper = FavoriteRegionalGuideUiMapper(),
        )

    private fun sampleGuide(
        id: String,
        name: String,
        subCategory: DisposalSubCategory? = null,
    ): DisposalItemGuide =
        DisposalItemGuide(
            id = id,
            name = name,
            category = DisposalCategory.PAPER_PACK,
            subCategory = subCategory,
            instructions = listOf(DisposalInstruction(method = "재활용폐기물")),
            steps = emptyList(),
            cautions = emptyList(),
            tip = null,
            isRecyclable = true,
            relatedSpotTypes = emptyList(),
        )

    private class FakeItemRepository(
        guides: List<DisposalItemGuide>,
    ) : DisposalItemGuideRepository {
        private val guidesById = guides.associateBy { it.id }

        override suspend fun searchItemGuides(query: String): List<DisposalItemGuide> = emptyList()

        override suspend fun getItemGuide(guideId: String): DisposalItemGuide? = guidesById[guideId]

        override suspend fun getCategoryGuides(category: DisposalCategory): List<DisposalItemGuide> = emptyList()

        override fun getCategories(): List<DisposalCategory> = emptyList()
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

    private class FakeCollectionSpotFavoriteSnapshotRepository(
        snapshots: List<CollectionSpotFavoriteSnapshot> = emptyList(),
    ) : CollectionSpotFavoriteSnapshotRepository {
        val snapshots = MutableStateFlow(snapshots)

        override fun observeSnapshots(): Flow<List<CollectionSpotFavoriteSnapshot>> = snapshots

        override suspend fun getSnapshot(targetId: String): CollectionSpotFavoriteSnapshot? =
            snapshots.value.firstOrNull { snapshot -> snapshot.targetId == targetId }

        override suspend fun upsertSnapshot(snapshot: CollectionSpotFavoriteSnapshot) {
            snapshots.value =
                snapshots.value
                    .filterNot { it.targetId == snapshot.targetId } + snapshot
        }

        override suspend fun deleteSnapshot(targetId: String) {
            snapshots.value = snapshots.value.filterNot { it.targetId == targetId }
        }
    }

    private class FakeCollectionSpotFavoriteRepository(
        private val favoriteRepository: FakeFavoriteRepository,
        private val snapshotRepository: FakeCollectionSpotFavoriteSnapshotRepository,
    ) : CollectionSpotFavoriteRepository {
        override suspend fun toggleFavorite(spot: CollectionSpot): Boolean = false

        override suspend fun removeFavorite(targetId: String) {
            favoriteRepository.removeFavorite(FavoriteTargetType.COLLECTION_SPOT, targetId)
            snapshotRepository.deleteSnapshot(targetId)
        }
    }

    private class FakeRegionalGuideFavoriteSnapshotRepository(
        snapshots: List<RegionalGuideFavoriteSnapshot> = emptyList(),
    ) : RegionalGuideFavoriteSnapshotRepository {
        val snapshots = MutableStateFlow(snapshots)

        override fun observeSnapshots(): Flow<List<RegionalGuideFavoriteSnapshot>> = snapshots

        override suspend fun getSnapshot(targetId: String): RegionalGuideFavoriteSnapshot? =
            snapshots.value.firstOrNull { snapshot -> snapshot.targetId == targetId }

        override suspend fun upsertSnapshot(snapshot: RegionalGuideFavoriteSnapshot) {
            snapshots.value =
                snapshots.value
                    .filterNot { it.targetId == snapshot.targetId } + snapshot
        }

        override suspend fun deleteSnapshot(targetId: String) {
            snapshots.value = snapshots.value.filterNot { it.targetId == targetId }
        }
    }

    private class FakeRegionalGuideFavoriteRepository(
        private val favoriteRepository: FakeFavoriteRepository,
        private val snapshotRepository: FakeRegionalGuideFavoriteSnapshotRepository,
    ) : RegionalGuideFavoriteRepository {
        override suspend fun toggleFavorite(snapshot: RegionalGuideFavoriteSnapshot): Boolean = false

        override suspend fun removeFavorite(targetId: String) {
            favoriteRepository.removeFavorite(FavoriteTargetType.REGIONAL_GUIDE, targetId)
            snapshotRepository.deleteSnapshot(targetId)
        }
    }
}
