package com.team.yeogibeoryeo.presentation.favorites

import androidx.lifecycle.SavedStateHandle
import com.team.yeogibeoryeo.domain.favorite.model.CollectionSpotFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteSnapshotRepository
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteSnapshotRepository
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveCollectionSpotFavoritesUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoritesUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveRegionalGuideFavoriteSnapshotsUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.RemoveCollectionSpotFavoriteUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.RemoveFavoriteUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.RemoveRegionalGuideFavoriteUseCase
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalInstruction
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.model.DisposalSubCategory
import com.team.yeogibeoryeo.domain.item.repository.DisposalItemGuideRepository
import com.team.yeogibeoryeo.domain.item.usecase.GetDisposalItemGuideUseCase
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.repository.HomeRegionalGuidePrimaryFavoriteRepository
import com.team.yeogibeoryeo.domain.regionalguide.usecase.ClearHomeRegionalGuidePrimaryFavoriteUseCase
import com.team.yeogibeoryeo.domain.regionalguide.usecase.ObserveHomeRegionalGuidePrimaryFavoriteTargetIdUseCase
import com.team.yeogibeoryeo.domain.regionalguide.usecase.SetHomeRegionalGuidePrimaryFavoriteUseCase
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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
                sampleItemGuide()
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
    fun `품목 즐겨찾기 해제 시 공통 Favorite를 삭제하고 UI 목록을 갱신한다`() =
        runTest {
            val guide =
                sampleItemGuide()
            val favoriteRepository =
                FakeFavoriteRepository(
                    initialFavorites =
                        listOf(
                            Favorite(
                                type = FavoriteTargetType.ITEM_GUIDE,
                                targetId = guide.id,
                                savedAtMillis = 1L,
                            ),
                        ),
                )
            val viewModel =
                createViewModel(
                    favoriteRepository = favoriteRepository,
                    itemRepository = FakeItemRepository(guides = listOf(guide)),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.selectedFavorites.size)

            viewModel.removeItemGuideFavorite(guide.id)
            advanceUntilIdle()

            assertEquals(emptyList<FavoriteUiModel>(), viewModel.uiState.value.selectedFavorites)
            assertEquals(false, favoriteRepository.isFavorite(FavoriteTargetType.ITEM_GUIDE, guide.id))
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
    fun `저장된 탭이 없으면 품목 탭을 기본값으로 사용한다`() =
        runTest {
            val viewModel =
                createViewModel(
                    favoriteRepository = FakeFavoriteRepository(),
                    itemRepository = FakeItemRepository(guides = emptyList()),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            assertEquals(FavoriteTab.ITEM_GUIDE, viewModel.uiState.value.selectedTab)
        }

    @Test
    fun `ViewModel을 다시 만들면 저장된 탭을 복원한다`() =
        runTest {
            val savedStateHandle = SavedStateHandle()
            val favoriteRepository = FakeFavoriteRepository()
            val itemRepository = FakeItemRepository(guides = emptyList())
            val firstViewModel =
                createViewModel(
                    favoriteRepository = favoriteRepository,
                    itemRepository = itemRepository,
                    savedStateHandle = savedStateHandle,
                )

            firstViewModel.selectTab(FavoriteTab.REGIONAL_GUIDE)

            val restoredSavedStateHandle =
                SavedStateHandle(
                    savedStateHandle.keys().associateWith { key ->
                        savedStateHandle.get<Any?>(key)
                    },
                )

            val restoredViewModel =
                createViewModel(
                    favoriteRepository = favoriteRepository,
                    itemRepository = itemRepository,
                    savedStateHandle = restoredSavedStateHandle,
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                restoredViewModel.uiState.collect()
            }
            advanceUntilIdle()

            assertEquals(FavoriteTab.REGIONAL_GUIDE, restoredViewModel.uiState.value.selectedTab)
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

    @Test
    fun `지역 가이드 홈 고정 대상이면 UI 모델에 고정 상태를 표시한다`() =
        runTest {
            val snapshot =
                RegionalGuideFavoriteSnapshot(
                    targetId = "regional-guide-primary",
                    region = Region(sido = "Sido", sigungu = "Sigungu"),
                    targetRegionName = null,
                    managementZoneName = null,
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
                    itemRepository = FakeItemRepository(guides = emptyList()),
                    regionalGuideSnapshotRepository =
                        FakeRegionalGuideFavoriteSnapshotRepository(snapshots = listOf(snapshot)),
                    primaryFavoriteRepository =
                        FakeHomeRegionalGuidePrimaryFavoriteRepository(
                            initialTargetId = snapshot.targetId,
                        ),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            val regionalGuide = viewModel.uiState.value.regionalGuideFavorites.single()

            assertEquals(true, regionalGuide.isHomeRegionalGuidePrimary)
        }

    @Test
    fun `지역 가이드 홈 고정 버튼을 누르면 대표 지역을 저장한다`() =
        runTest {
            val snapshot =
                RegionalGuideFavoriteSnapshot(
                    targetId = "regional-guide-primary",
                    region = Region(sido = "Sido", sigungu = "Sigungu"),
                    targetRegionName = null,
                    managementZoneName = null,
                )
            val primaryFavoriteRepository = FakeHomeRegionalGuidePrimaryFavoriteRepository()
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
                    itemRepository = FakeItemRepository(guides = emptyList()),
                    regionalGuideSnapshotRepository =
                        FakeRegionalGuideFavoriteSnapshotRepository(snapshots = listOf(snapshot)),
                    primaryFavoriteRepository = primaryFavoriteRepository,
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            viewModel.toggleHomeRegionalGuidePrimaryFavorite(snapshot.targetId)
            advanceUntilIdle()

            assertEquals(snapshot.targetId, primaryFavoriteRepository.primaryTargetId.value)
            assertEquals(true, viewModel.uiState.value.regionalGuideFavorites.single().isHomeRegionalGuidePrimary)
        }

    @Test
    fun `홈 고정된 지역 가이드를 다시 누르면 고정을 해제한다`() =
        runTest {
            val snapshot =
                RegionalGuideFavoriteSnapshot(
                    targetId = "regional-guide-primary",
                    region = Region(sido = "Sido", sigungu = "Sigungu"),
                    targetRegionName = null,
                    managementZoneName = null,
                )
            val primaryFavoriteRepository =
                FakeHomeRegionalGuidePrimaryFavoriteRepository(initialTargetId = snapshot.targetId)
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
                    itemRepository = FakeItemRepository(guides = emptyList()),
                    regionalGuideSnapshotRepository =
                        FakeRegionalGuideFavoriteSnapshotRepository(snapshots = listOf(snapshot)),
                    primaryFavoriteRepository = primaryFavoriteRepository,
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            viewModel.toggleHomeRegionalGuidePrimaryFavorite(snapshot.targetId)
            advanceUntilIdle()

            assertEquals(null, primaryFavoriteRepository.primaryTargetId.value)
            assertEquals(false, viewModel.uiState.value.regionalGuideFavorites.single().isHomeRegionalGuidePrimary)
        }

    @Test
    fun `홈 고정된 지역 가이드를 삭제하면 고정 상태도 초기화한다`() =
        runTest {
            val snapshot =
                RegionalGuideFavoriteSnapshot(
                    targetId = "regional-guide-primary",
                    region = Region(sido = "Sido", sigungu = "Sigungu"),
                    targetRegionName = null,
                    managementZoneName = null,
                )
            val primaryFavoriteRepository =
                FakeHomeRegionalGuidePrimaryFavoriteRepository(initialTargetId = snapshot.targetId)
            val snapshotRepository =
                FakeRegionalGuideFavoriteSnapshotRepository(snapshots = listOf(snapshot))
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
                    favoriteRepository = favoriteRepository,
                    itemRepository = FakeItemRepository(guides = emptyList()),
                    regionalGuideSnapshotRepository = snapshotRepository,
                    primaryFavoriteRepository = primaryFavoriteRepository,
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            viewModel.removeRegionalGuideFavorite(snapshot.targetId)
            advanceUntilIdle()

            assertEquals(null, primaryFavoriteRepository.primaryTargetId.value)
            assertEquals(false, favoriteRepository.isFavorite(FavoriteTargetType.REGIONAL_GUIDE, snapshot.targetId))
        }

    @Test
    fun `품목 즐겨찾기 삭제 실패 시 목록을 유지하고 실패 이벤트를 보낸다`() =
        runTest {
            val guide = sampleItemGuide()
            val favoriteRepository =
                FakeFavoriteRepository(
                    initialFavorites =
                        listOf(
                            Favorite(
                                type = FavoriteTargetType.ITEM_GUIDE,
                                targetId = guide.id,
                                savedAtMillis = 1L,
                            ),
                        ),
                    removeFailure = IllegalStateException("삭제 실패"),
                )
            val viewModel =
                createViewModel(
                    favoriteRepository = favoriteRepository,
                    itemRepository = FakeItemRepository(guides = listOf(guide)),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()
            val event =
                async(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.events.first()
                }

            viewModel.removeItemGuideFavorite(guide.id)
            advanceUntilIdle()

            assertEquals(FavoritesEvent.FavoriteUpdateFailed, event.await())
            assertEquals(listOf(guide.id), viewModel.uiState.value.selectedFavorites.map { it.targetId })
            assertEquals(true, favoriteRepository.isFavorite(FavoriteTargetType.ITEM_GUIDE, guide.id))
        }

    @Test
    fun `수거 장소 즐겨찾기 삭제 실패 시 목록과 스냅샷을 유지한다`() =
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
                FakeCollectionSpotFavoriteSnapshotRepository(snapshots = listOf(snapshot))
            val viewModel =
                createViewModel(
                    favoriteRepository = favoriteRepository,
                    itemRepository = FakeItemRepository(guides = emptyList()),
                    collectionSpotSnapshotRepository = snapshotRepository,
                    collectionSpotRemoveFailure = IllegalStateException("삭제 실패"),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()
            viewModel.selectTab(FavoriteTab.COLLECTION_SPOT)
            val event =
                async(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.events.first()
                }

            viewModel.removeCollectionSpotFavorite(snapshot.targetId)
            advanceUntilIdle()

            assertEquals(FavoritesEvent.FavoriteUpdateFailed, event.await())
            assertEquals(listOf(snapshot.targetId), viewModel.uiState.value.selectedFavorites.map { it.targetId })
            assertEquals(true, favoriteRepository.isFavorite(FavoriteTargetType.COLLECTION_SPOT, snapshot.targetId))
            assertEquals(listOf(snapshot), snapshotRepository.snapshots.value)
        }

    @Test
    fun `지역 안내 즐겨찾기 삭제 실패 시 목록과 스냅샷을 유지한다`() =
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
                    regionalGuideRemoveFailure = IllegalStateException("삭제 실패"),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()
            viewModel.selectTab(FavoriteTab.REGIONAL_GUIDE)
            val event =
                async(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.events.first()
                }

            viewModel.removeRegionalGuideFavorite(snapshot.targetId)
            advanceUntilIdle()

            assertEquals(FavoritesEvent.FavoriteUpdateFailed, event.await())
            assertEquals(listOf(snapshot.targetId), viewModel.uiState.value.selectedFavorites.map { it.targetId })
            assertEquals(true, favoriteRepository.isFavorite(FavoriteTargetType.REGIONAL_GUIDE, snapshot.targetId))
            assertEquals(listOf(snapshot), snapshotRepository.snapshots.value)
        }

    @Test
    fun `같은 품목을 빠르게 두 번 삭제하면 한 번만 요청한다`() =
        runTest {
            val guide = sampleItemGuide()
            val removeStarted = CompletableDeferred<Unit>()
            val continueRemove = CompletableDeferred<Unit>()
            val favoriteRepository =
                FakeFavoriteRepository(
                    initialFavorites =
                        listOf(
                            Favorite(
                                type = FavoriteTargetType.ITEM_GUIDE,
                                targetId = guide.id,
                                savedAtMillis = 1L,
                            ),
                        ),
                    removeStarted = removeStarted,
                    continueRemove = continueRemove,
                )
            val viewModel =
                createViewModel(
                    favoriteRepository = favoriteRepository,
                    itemRepository = FakeItemRepository(guides = listOf(guide)),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            viewModel.removeItemGuideFavorite(guide.id)
            runCurrent()
            removeStarted.await()
            viewModel.removeItemGuideFavorite(guide.id)
            continueRemove.complete(Unit)
            advanceUntilIdle()

            assertEquals(1, favoriteRepository.removeCallCount)
            assertEquals(emptyList<FavoriteUiModel>(), viewModel.uiState.value.selectedFavorites)
            assertEquals(false, favoriteRepository.isFavorite(FavoriteTargetType.ITEM_GUIDE, guide.id))
        }

    private fun createViewModel(
        favoriteRepository: FakeFavoriteRepository,
        itemRepository: FakeItemRepository,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        collectionSpotSnapshotRepository: FakeCollectionSpotFavoriteSnapshotRepository =
            FakeCollectionSpotFavoriteSnapshotRepository(),
        regionalGuideSnapshotRepository: FakeRegionalGuideFavoriteSnapshotRepository =
            FakeRegionalGuideFavoriteSnapshotRepository(),
        primaryFavoriteRepository: FakeHomeRegionalGuidePrimaryFavoriteRepository =
            FakeHomeRegionalGuidePrimaryFavoriteRepository(),
        collectionSpotRemoveFailure: Exception? = null,
        regionalGuideRemoveFailure: Exception? = null,
    ): FavoritesViewModel =
        FavoritesViewModel(
            savedStateHandle = savedStateHandle,
            observeFavoritesUseCase = ObserveFavoritesUseCase(favoriteRepository),
            observeCollectionSpotFavoritesUseCase =
                ObserveCollectionSpotFavoritesUseCase(
                    favoriteRepository = favoriteRepository,
                    snapshotRepository = collectionSpotSnapshotRepository,
                ),
            observeRegionalGuideFavoriteSnapshotsUseCase =
                ObserveRegionalGuideFavoriteSnapshotsUseCase(regionalGuideSnapshotRepository),
            observeHomeRegionalGuidePrimaryFavoriteTargetIdUseCase =
                ObserveHomeRegionalGuidePrimaryFavoriteTargetIdUseCase(primaryFavoriteRepository),
            removeFavoriteUseCase = RemoveFavoriteUseCase(favoriteRepository),
            removeCollectionSpotFavoriteUseCase =
                RemoveCollectionSpotFavoriteUseCase(
                    collectionSpotFavoriteRepository =
                        FakeCollectionSpotFavoriteRepository(
                            favoriteRepository = favoriteRepository,
                            snapshotRepository = collectionSpotSnapshotRepository,
                            removeFailure = collectionSpotRemoveFailure,
                        ),
                ),
            removeRegionalGuideFavoriteUseCase =
                RemoveRegionalGuideFavoriteUseCase(
                    repository =
                        FakeRegionalGuideFavoriteRepository(
                            favoriteRepository = favoriteRepository,
                            snapshotRepository = regionalGuideSnapshotRepository,
                            removeFailure = regionalGuideRemoveFailure,
                        ),
                    homeRegionalGuidePrimaryFavoriteRepository = primaryFavoriteRepository,
                ),
            setHomeRegionalGuidePrimaryFavoriteUseCase =
                SetHomeRegionalGuidePrimaryFavoriteUseCase(primaryFavoriteRepository),
            clearHomeRegionalGuidePrimaryFavoriteUseCase =
                ClearHomeRegionalGuidePrimaryFavoriteUseCase(primaryFavoriteRepository),
            itemGuideUiMapper = FavoriteItemGuideUiMapper(GetDisposalItemGuideUseCase(itemRepository)),
            collectionSpotUiMapper = FavoriteCollectionSpotUiMapper(),
            regionalGuideUiMapper = FavoriteRegionalGuideUiMapper(),
        )

    private fun sampleItemGuide(): DisposalItemGuide =
        DisposalItemGuide(
            id = "paper-pack",
            name = "종이팩",
            category = DisposalCategory.PAPER_PACK,
            subCategory = DisposalSubCategory.MILK_CARTON,
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
        private val removeFailure: Exception? = null,
        private val removeStarted: CompletableDeferred<Unit>? = null,
        private val continueRemove: CompletableDeferred<Unit>? = null,
    ) : FavoriteRepository {
        private val favorites = MutableStateFlow(initialFavorites)
        var removeCallCount: Int = 0
            private set

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
            removeCallCount += 1
            removeStarted?.complete(Unit)
            continueRemove?.await()
            removeFailure?.let { throw it }
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
        private val removeFailure: Exception? = null,
    ) : CollectionSpotFavoriteRepository {
        override suspend fun toggleFavorite(spot: CollectionSpot): Boolean = false

        override suspend fun removeFavorite(targetId: String) {
            removeFailure?.let { throw it }
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

    private class FakeHomeRegionalGuidePrimaryFavoriteRepository(
        initialTargetId: String? = null,
    ) : HomeRegionalGuidePrimaryFavoriteRepository {
        val primaryTargetId = MutableStateFlow(initialTargetId)

        override fun observePrimaryFavoriteTargetId(): Flow<String?> = primaryTargetId

        override suspend fun setPrimaryFavoriteTargetId(targetId: String) {
            primaryTargetId.value = targetId
        }

        override suspend fun clearPrimaryFavoriteTargetId() {
            primaryTargetId.value = null
        }

        override suspend fun clearPrimaryFavoriteTargetIdIfMatches(targetId: String) {
            if (primaryTargetId.value == targetId) {
                primaryTargetId.value = null
            }
        }
    }

    private class FakeRegionalGuideFavoriteRepository(
        private val favoriteRepository: FakeFavoriteRepository,
        private val snapshotRepository: FakeRegionalGuideFavoriteSnapshotRepository,
        private val removeFailure: Exception? = null,
    ) : RegionalGuideFavoriteRepository {
        override suspend fun toggleFavorite(snapshot: RegionalGuideFavoriteSnapshot): Boolean = false

        override suspend fun removeFavorite(targetId: String) {
            removeFailure?.let { throw it }
            favoriteRepository.removeFavorite(FavoriteTargetType.REGIONAL_GUIDE, targetId)
            snapshotRepository.deleteSnapshot(targetId)
        }
    }
}
