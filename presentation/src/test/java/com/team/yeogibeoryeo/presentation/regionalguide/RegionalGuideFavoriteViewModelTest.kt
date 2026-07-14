package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteKey
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteRepository
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.presentation.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class RegionalGuideFavoriteViewModelTest {

    @get:Rule
    val mainDispatcherRule = RegionalGuideMainDispatcherRule()

    @Test
    fun `즐겨찾기 클릭은 지역 가이드 즐겨찾기를 저장하고 상태를 관찰한다`() = runTest {
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
    fun `지역 가이드 즐겨찾기 변경 실패 시 상태를 유지하고 실패 이벤트를 보낸다`() = runTest {
        val guide = sampleGuide(sido = "서울특별시", sigungu = "중구", targetRegionName = "금호2.3가동")
        val snapshot = guide.toFavoriteSnapshot()
        val viewModel = createViewModel(
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(candidates = listOf(guide)),
            regionalGuideSnapshotRepository =
                FakeRegionalGuideFavoriteSnapshotRepository(snapshots = listOf(snapshot)),
            regionalGuideFavoriteRepository = FailingRegionalGuideFavoriteRepository(),
        )
        advanceUntilIdle()
        viewModel.loadByFavoriteTargetId(snapshot.targetId)
        advanceUntilIdle()
        val event = async(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.first()
        }

        viewModel.onFavoriteClick()
        advanceUntilIdle()

        assertEquals(RegionalGuideEvent.FavoriteUpdateFailed, event.await())
        assertEquals(false, (viewModel.uiState.value as RegionalGuideUiState.Success).isFavorite)
    }

    @Test
    fun `지역 가이드 즐겨찾기를 빠르게 두 번 누르면 처리 중 중복 요청을 무시한다`() = runTest {
        val guide = sampleGuide(sido = "서울특별시", sigungu = "중구", targetRegionName = "금호2.3가동")
        val favoriteRepository = FakeFavoriteRepository()
        val snapshot = guide.toFavoriteSnapshot()
        val snapshotRepository = FakeRegionalGuideFavoriteSnapshotRepository(snapshots = listOf(snapshot))
        val firstToggleStarted = CompletableDeferred<Unit>()
        val continueFirstToggle = CompletableDeferred<Unit>()
        val favoriteToggleRepository = PausingRegionalGuideFavoriteRepository(
            delegate = FakeRegionalGuideFavoriteRepository(
                favoriteRepository = favoriteRepository,
                snapshotRepository = snapshotRepository,
            ),
            firstToggleStarted = firstToggleStarted,
            continueFirstToggle = continueFirstToggle,
        )
        val viewModel = createViewModel(
            regionalGuideRepository = FakeRegionalDisposalGuideRepository(candidates = listOf(guide)),
            favoriteRepository = favoriteRepository,
            regionalGuideSnapshotRepository = snapshotRepository,
            regionalGuideFavoriteRepository = favoriteToggleRepository,
        )
        advanceUntilIdle()
        viewModel.loadByFavoriteTargetId(snapshot.targetId)
        advanceUntilIdle()

        viewModel.onFavoriteClick()
        runCurrent()
        firstToggleStarted.await()
        viewModel.onFavoriteClick()
        continueFirstToggle.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, favoriteToggleRepository.toggleCallCount)
        assertEquals(true, favoriteRepository.isFavorite(FavoriteTargetType.REGIONAL_GUIDE, snapshot.targetId))
        assertEquals(true, (viewModel.uiState.value as RegionalGuideUiState.Success).isFavorite)
    }

    @Test
    fun `즐겨찾기 대상 아이디로 저장된 지역 가이드 후보를 복원한다`() = runTest {
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
    fun `즐겨찾기 대상 아이디는 대상지역이 같고 관리구역이 다른 후보를 복원한다`() = runTest {
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
    fun `즐겨찾기 재진입은 저장된 통합 시도명 키와 현재 시도명 후보를 호환되게 복원한다`() = runTest {
        val snapshotRegion = Region(
            sido = "전남광주통합특별시",
            sigungu = "나주시",
            eupmyeondong = "노안면",
        )
        val currentGuide =
            RegionalDisposalGuide(
                region = Region(
                    sido = "전라남도",
                    sigungu = "나주시",
                    eupmyeondong = "노안면",
                ),
                targetRegionName = "노안면",
                managementZoneName = "노안면",
                schedules = emptyList(),
            )
        val targetId = RegionalGuideFavoriteKey(
            sido = snapshotRegion.sido,
            sigungu = snapshotRegion.sigungu,
            eupmyeondong = snapshotRegion.eupmyeondong,
            targetRegionName = "노안면",
            managementZoneName = "노안면",
        ).encode()
        val snapshot = RegionalGuideFavoriteSnapshot(
            targetId = targetId,
            region = snapshotRegion,
            targetRegionName = "노안면",
            managementZoneName = "노안면",
        )
        val favoriteRepository =
            FakeFavoriteRepository(
                initialFavorites =
                    listOf(
                        Favorite(
                            type = FavoriteTargetType.REGIONAL_GUIDE,
                            targetId = targetId,
                            savedAtMillis = 1L,
                        ),
                    ),
            )
        val viewModel =
            createViewModel(
                regionalGuideRepository =
                    FakeRegionalDisposalGuideRepository(candidates = listOf(currentGuide)),
                favoriteRepository = favoriteRepository,
                regionalGuideSnapshotRepository =
                    FakeRegionalGuideFavoriteSnapshotRepository(snapshots = listOf(snapshot)),
            )
        advanceUntilIdle()

        viewModel.loadByFavoriteTargetId(targetId)
        advanceUntilIdle()

        val state = viewModel.uiState.value as RegionalGuideUiState.Success
        assertEquals("전남광주통합특별시 나주시 노안면", state.guide.regionName)
        assertEquals("노안면", state.guide.targetRegionName)
        assertEquals(true, state.isFavorite)
    }

    @Test
    fun `저장 후보 스냅샷이 없으면 복원 실패와 지역 다시 선택 동작을 보여준다`() = runTest {
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
    fun `저장 후보가 안내 후보와 더 이상 일치하지 않으면 복원 실패 동작을 보여준다`() = runTest {
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
    fun `즐겨찾기 복원 후보가 모호하면 즐겨찾기 복원 모호 사유를 보여준다`() = runTest {
        val region = Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "반석동")
        val firstGuide =
            RegionalDisposalGuide(
                region = region,
                targetRegionName = "반석동 일부지역",
                managementZoneName = "노은2동",
                schedules = emptyList(),
            )
        val secondGuide =
            RegionalDisposalGuide(
                region = region,
                targetRegionName = "반석동 일부지역",
                managementZoneName = "노은3동",
                schedules = emptyList(),
            )
        val targetId = RegionalGuideFavoriteKey(
            sido = region.sido,
            sigungu = region.sigungu,
            eupmyeondong = region.eupmyeondong,
            targetRegionName = secondGuide.targetRegionName,
            managementZoneName = null,
        ).encode()
        val snapshot = RegionalGuideFavoriteSnapshot(
            targetId = targetId,
            region = region,
            targetRegionName = secondGuide.targetRegionName,
            managementZoneName = null,
        )
        val viewModel =
            createViewModel(
                regionalGuideRepository =
                    FakeRegionalDisposalGuideRepository(candidates = listOf(firstGuide, secondGuide)),
                regionalGuideSnapshotRepository =
                    FakeRegionalGuideFavoriteSnapshotRepository(snapshots = listOf(snapshot)),
            )
        advanceUntilIdle()

        viewModel.loadByFavoriteTargetId(targetId)
        advanceUntilIdle()

        val state = viewModel.uiState.value as RegionalGuideUiState.GuideCandidates

        assertEquals(RegionalGuideCandidateReason.FAVORITE_RESTORE_AMBIGUOUS, state.reason)
        assertEquals(2, state.candidates.size)
    }

    @Test
    fun `즐겨찾기 복원 모호 후보 상세에서 뒤로가기를 요청하면 이전 후보 목록을 복원한다`() = runTest {
        val region = Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "반석동")
        val firstGuide =
            RegionalDisposalGuide(
                region = region,
                targetRegionName = "반석동 일부지역",
                managementZoneName = "노은2동",
                schedules = emptyList(),
            )
        val secondGuide =
            RegionalDisposalGuide(
                region = region,
                targetRegionName = "반석동 일부지역",
                managementZoneName = "노은3동",
                schedules = emptyList(),
            )
        val targetId = RegionalGuideFavoriteKey(
            sido = region.sido,
            sigungu = region.sigungu,
            eupmyeondong = region.eupmyeondong,
            targetRegionName = secondGuide.targetRegionName,
            managementZoneName = null,
        ).encode()
        val snapshot = RegionalGuideFavoriteSnapshot(
            targetId = targetId,
            region = region,
            targetRegionName = secondGuide.targetRegionName,
            managementZoneName = null,
        )
        val viewModel =
            createViewModel(
                regionalGuideRepository =
                    FakeRegionalDisposalGuideRepository(candidates = listOf(firstGuide, secondGuide)),
                regionalGuideSnapshotRepository =
                    FakeRegionalGuideFavoriteSnapshotRepository(snapshots = listOf(snapshot)),
            )
        advanceUntilIdle()

        viewModel.loadByFavoriteTargetId(targetId)
        advanceUntilIdle()

        val candidate = (viewModel.uiState.value as RegionalGuideUiState.GuideCandidates)
            .candidates
            .first()

        viewModel.onRegionalGuideCandidateSelected(candidate)

        val successState = viewModel.uiState.value as RegionalGuideUiState.Success
        assertTrue(successState.canRestoreCandidates)

        assertTrue(viewModel.restoreCandidatesFromDetail())

        val restoredState = viewModel.uiState.value as RegionalGuideUiState.GuideCandidates
        assertEquals(RegionalGuideCandidateReason.FAVORITE_RESTORE_AMBIGUOUS, restoredState.reason)
        assertEquals(2, restoredState.candidates.size)
    }

    @Test
    fun `가이드 후보 선택 후 즐겨찾기 클릭은 원본 없음 권역 값을 스냅샷에 유지한다`() = runTest {
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
    fun `이전 형식 지역 가이드 즐겨찾기 키는 현재 스냅샷과 함께 관찰되고 삭제된다`() = runTest {
        val region = Region(sido = "서울특별시", sigungu = "노원구", eupmyeondong = "하계동")
        val guide =
            RegionalDisposalGuide(
                region = region,
                targetRegionName = "하계1동",
                managementZoneName = "6권역",
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

private fun RegionalDisposalGuide.toFavoriteSnapshot(): RegionalGuideFavoriteSnapshot {
    val key = RegionalGuideFavoriteKey(
        sido = region.sido,
        sigungu = region.sigungu,
        eupmyeondong = region.eupmyeondong,
        targetRegionName = targetRegionName,
        managementZoneName = managementZoneName,
    )

    return RegionalGuideFavoriteSnapshot(
        targetId = key.encode(),
        region = region,
        targetRegionName = targetRegionName?.trim()?.takeIf { it.isNotBlank() },
        managementZoneName = managementZoneName?.trim()?.takeIf { it.isNotBlank() },
    )
}

private class FailingRegionalGuideFavoriteRepository : RegionalGuideFavoriteRepository {
    override suspend fun toggleFavorite(snapshot: RegionalGuideFavoriteSnapshot): Boolean {
        throw IllegalStateException("저장 실패")
    }

    override suspend fun removeFavorite(targetId: String) = Unit
}

private class PausingRegionalGuideFavoriteRepository(
    private val delegate: RegionalGuideFavoriteRepository,
    private val firstToggleStarted: CompletableDeferred<Unit>,
    private val continueFirstToggle: CompletableDeferred<Unit>,
) : RegionalGuideFavoriteRepository {
    var toggleCallCount = 0
        private set

    override suspend fun toggleFavorite(snapshot: RegionalGuideFavoriteSnapshot): Boolean {
        toggleCallCount += 1
        if (toggleCallCount == 1) {
            firstToggleStarted.complete(Unit)
            continueFirstToggle.await()
        }
        return delegate.toggleFavorite(snapshot)
    }

    override suspend fun removeFavorite(targetId: String) {
        delegate.removeFavorite(targetId)
    }
}
