package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.model.toFavoriteSnapshot
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.presentation.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class RegionalGuideFavoriteViewModelTest {

    @get:Rule
    val mainDispatcherRule = RegionalGuideMainDispatcherRule()

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

    @Test
    fun `favorite target id restores candidate with same target region and different management zone`() = runTest {
        val region = Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "반석동")
        val firstGuide =
            RegionalDisposalGuide(
                region = region,
                targetRegionName = "반석동 일부지역",
                managementZoneName = "노은2동",
                schedules = emptyList(),
            )
        val savedGuide =
            RegionalDisposalGuide(
                region = region,
                targetRegionName = "반석동 일부지역",
                managementZoneName = "노은3동",
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
        assertEquals("반석동 일부지역", state.guide.targetRegionName)
        assertEquals("노은3동", state.guide.managementZoneName)
        assertEquals(true, state.isFavorite)
    }

    @Test
    fun `저장 후보 스냅샷이 없으면 복원 실패와 지역 다시 선택 action을 보여준다`() = runTest {
        val viewModel = createViewModel(
            regionalGuideSnapshotRepository = FakeRegionalGuideFavoriteSnapshotRepository()
        )
        advanceUntilIdle()

        viewModel.loadByFavoriteTargetId("missing-target-id")
        advanceUntilIdle()

        val state = viewModel.uiState.value as RegionalGuideUiState.Empty

        assertEquals(R.string.regional_guide_empty_favorite_restore_failed_title, state.titleResId)
        assertEquals(R.string.regional_guide_empty_favorite_restore_failed_message, state.messageResId)
        assertEquals(RegionalGuideEmptyActionType.SELECT_REGION, state.action?.type)
        assertEquals(R.string.regional_guide_empty_action_select_region, state.action?.labelResId)
    }

    @Test
    fun `저장 후보가 info 후보와 더 이상 일치하지 않으면 복원 실패 action을 보여준다`() = runTest {
        val savedGuide =
            RegionalDisposalGuide(
                region = Region(sido = "대전광역시", sigungu = "유성구"),
                targetRegionName = "반석동 일부지역",
                managementZoneName = "노은3동",
                schedules = emptyList(),
            )
        val snapshot = savedGuide.toFavoriteSnapshot()
        val viewModel =
            createViewModel(
                regionalGuideRepository =
                    FakeRegionalDisposalGuideRepository(
                        candidates = listOf(
                            savedGuide.copy(managementZoneName = "노은2동")
                        )
                    ),
                regionalGuideSnapshotRepository =
                    FakeRegionalGuideFavoriteSnapshotRepository(snapshots = listOf(snapshot)),
            )
        advanceUntilIdle()

        viewModel.loadByFavoriteTargetId(snapshot.targetId)
        advanceUntilIdle()

        val state = viewModel.uiState.value as RegionalGuideUiState.Empty

        assertEquals(R.string.regional_guide_empty_favorite_restore_failed_title, state.titleResId)
        assertEquals(R.string.regional_guide_empty_favorite_restore_failed_message, state.messageResId)
        assertEquals(RegionalGuideEmptyActionType.SELECT_REGION, state.action?.type)
    }

    @Test
    fun `favorite click after guide candidate selection keeps original none zone values in snapshot`() = runTest {
        val region = Region(sido = "경기도", sigungu = "성남시")
        val doorToDoorGuide =
            RegionalDisposalGuide(
                region = region,
                managementZoneName = "없음",
                targetRegionName = "없음",
                disposalPlaceType = "문전수거",
                schedules = emptyList(),
            )
        val basePointGuide =
            RegionalDisposalGuide(
                region = region,
                managementZoneName = "없음",
                targetRegionName = "없음",
                disposalPlaceType = "거점수거",
                schedules = emptyList(),
            )
        val favoriteRepository = FakeFavoriteRepository()
        val snapshotRepository = FakeRegionalGuideFavoriteSnapshotRepository()
        val viewModel =
            createViewModel(
                regionRepository = FakeRegionRepository(resolvedRegion = region),
                regionalGuideRepository =
                    FakeRegionalDisposalGuideRepository(candidates = listOf(doorToDoorGuide, basePointGuide)),
                favoriteRepository = favoriteRepository,
                regionalGuideSnapshotRepository = snapshotRepository,
                regionalGuideFavoriteRepository =
                    FakeRegionalGuideFavoriteRepository(
                        favoriteRepository = favoriteRepository,
                        snapshotRepository = snapshotRepository,
                    ),
            )
        advanceUntilIdle()

        viewModel.onSearchKeywordChanged("성남시")
        viewModel.searchCurrentKeyword()
        advanceUntilIdle()

        val candidate = (viewModel.uiState.value as RegionalGuideUiState.GuideCandidates)
            .candidates
            .first { candidate -> candidate.displayText.contains("문전수거") }

        viewModel.onRegionalGuideCandidateSelected(candidate)
        viewModel.onFavoriteClick()
        advanceUntilIdle()

        val expectedSnapshot = doorToDoorGuide.toFavoriteSnapshot()
        assertEquals(true, favoriteRepository.isFavorite(FavoriteTargetType.REGIONAL_GUIDE, expectedSnapshot.targetId))
        assertEquals(expectedSnapshot, snapshotRepository.getSnapshot(expectedSnapshot.targetId))
    }

    @Test
    fun `legacy regional guide favorite key is observed and removed with current snapshot`() = runTest {
        val region = Region(sido = "Sido", sigungu = "Sigungu", eupmyeondong = "Dong")
        val guide =
            RegionalDisposalGuide(
                region = region,
                targetRegionName = "Target",
                managementZoneName = "Management",
                schedules = emptyList(),
            )
        val currentSnapshot = guide.toFavoriteSnapshot()
        val legacyTargetId = currentSnapshot.legacyTargetId.orEmpty()
        val legacySnapshot = currentSnapshot.copy(targetId = legacyTargetId, managementZoneName = null)
        val favoriteRepository =
            FakeFavoriteRepository(
                initialFavorites =
                    listOf(
                        Favorite(
                            type = FavoriteTargetType.REGIONAL_GUIDE,
                            targetId = legacyTargetId,
                            savedAtMillis = 1L,
                        ),
                    ),
            )
        val snapshotRepository =
            FakeRegionalGuideFavoriteSnapshotRepository(snapshots = listOf(legacySnapshot))
        val viewModel =
            createViewModel(
                regionRepository = FakeRegionRepository(
                    resolvedRegion = region.copy(eupmyeondong = null)
                ),
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

        viewModel.loadByFavoriteTargetId(legacyTargetId)
        advanceUntilIdle()

        assertEquals(true, (viewModel.uiState.value as RegionalGuideUiState.Success).isFavorite)

        viewModel.onFavoriteClick()
        advanceUntilIdle()

        assertEquals(false, (viewModel.uiState.value as RegionalGuideUiState.Success).isFavorite)
        assertEquals(false, favoriteRepository.isFavorite(FavoriteTargetType.REGIONAL_GUIDE, legacyTargetId))
        assertEquals(false, favoriteRepository.isFavorite(FavoriteTargetType.REGIONAL_GUIDE, currentSnapshot.targetId))
        assertNull(snapshotRepository.getSnapshot(legacyTargetId))
        assertNull(snapshotRepository.getSnapshot(currentSnapshot.targetId))
    }

}


