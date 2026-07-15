package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteRepository
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.repository.HomeRegionalGuidePrimaryFavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ToggleRegionalGuideFavoriteUseCaseTest {
    @Test
    fun `상세 화면에서 고정된 지역 가이드를 해제하면 대표 저장값을 정리한다`() =
        runBlocking {
            val snapshot = sampleSnapshot()
            val favoriteRepository =
                FakeRegionalGuideFavoriteRepository(
                    initialFavorites =
                        listOf(
                            Favorite(
                                type = FavoriteTargetType.REGIONAL_GUIDE,
                                targetId = snapshot.targetId,
                                savedAtMillis = 1L,
                            ),
                        ),
                )
            val primaryFavoriteRepository =
                FakeHomeRegionalGuidePrimaryFavoriteRepository(
                    initialPrimaryTargetId = snapshot.targetId,
                    initialLastSelectedTargetId = snapshot.targetId,
                )
            val useCase =
                ToggleRegionalGuideFavoriteUseCase(
                    repository = favoriteRepository,
                    homeRegionalGuidePrimaryFavoriteRepository = primaryFavoriteRepository,
                )

            val isFavorite = useCase(snapshot)

            assertEquals(false, isFavorite)
            assertEquals(null, primaryFavoriteRepository.primaryTargetId.value)
            assertEquals(null, primaryFavoriteRepository.lastSelectedTargetId.value)
        }

    @Test
    fun `상세 화면 해제 후 대표 저장값 정리가 실패하면 재시도로 남은 값을 정리한다`() =
        runBlocking {
            val snapshot = sampleSnapshot()
            val favoriteRepository =
                FakeRegionalGuideFavoriteRepository(
                    initialFavorites =
                        listOf(
                            Favorite(
                                type = FavoriteTargetType.REGIONAL_GUIDE,
                                targetId = snapshot.targetId,
                                savedAtMillis = 1L,
                            ),
                        ),
                )
            val primaryFavoriteRepository =
                FakeHomeRegionalGuidePrimaryFavoriteRepository(
                    initialPrimaryTargetId = snapshot.targetId,
                    initialLastSelectedTargetId = snapshot.targetId,
                    clearFailureCount = 1,
                )
            val useCase =
                ToggleRegionalGuideFavoriteUseCase(
                    repository = favoriteRepository,
                    homeRegionalGuidePrimaryFavoriteRepository = primaryFavoriteRepository,
                )

            val firstResult = runCatching { useCase(snapshot) }
            primaryFavoriteRepository.clearPrimaryAndLastSelectedFavoriteTargetIdsIfMatches(
                snapshot.compatibleTargetIds,
            )

            assertEquals(true, firstResult.isFailure)
            assertEquals(null, primaryFavoriteRepository.primaryTargetId.value)
            assertEquals(null, primaryFavoriteRepository.lastSelectedTargetId.value)
        }

    @Test
    fun `상세 화면에서 해제한 지역을 다시 추가해도 이전 대표 고정값은 복원되지 않는다`() =
        runBlocking {
            val snapshot = sampleSnapshot()
            val favoriteRepository =
                FakeRegionalGuideFavoriteRepository(
                    initialFavorites =
                        listOf(
                            Favorite(
                                type = FavoriteTargetType.REGIONAL_GUIDE,
                                targetId = snapshot.targetId,
                                savedAtMillis = 1L,
                            ),
                        ),
                )
            val primaryFavoriteRepository =
                FakeHomeRegionalGuidePrimaryFavoriteRepository(
                    initialPrimaryTargetId = snapshot.targetId,
                    initialLastSelectedTargetId = snapshot.targetId,
                )
            val useCase =
                ToggleRegionalGuideFavoriteUseCase(
                    repository = favoriteRepository,
                    homeRegionalGuidePrimaryFavoriteRepository = primaryFavoriteRepository,
                )

            useCase(snapshot)
            val isFavorite = useCase(snapshot)

            assertEquals(true, isFavorite)
            assertEquals(null, primaryFavoriteRepository.primaryTargetId.value)
            assertEquals(null, primaryFavoriteRepository.lastSelectedTargetId.value)
        }

    private fun sampleSnapshot(): RegionalGuideFavoriteSnapshot =
        RegionalGuideFavoriteSnapshot(
            targetId = "regional-guide-primary",
            region = Region(sido = "서울특별시", sigungu = "노원구"),
            targetRegionName = null,
            managementZoneName = null,
        )

    private class FakeRegionalGuideFavoriteRepository(
        initialFavorites: List<Favorite> = emptyList(),
    ) : RegionalGuideFavoriteRepository {
        private val favorites = MutableStateFlow(initialFavorites)

        override suspend fun toggleFavorite(snapshot: RegionalGuideFavoriteSnapshot): Boolean {
            return if (
                favorites.value.any { favorite ->
                    favorite.type == FavoriteTargetType.REGIONAL_GUIDE &&
                        favorite.targetId == snapshot.targetId
                }
            ) {
                favorites.value =
                    favorites.value.filterNot { favorite ->
                        favorite.type == FavoriteTargetType.REGIONAL_GUIDE &&
                            favorite.targetId == snapshot.targetId
                    }
                false
            } else {
                favorites.value =
                    favorites.value +
                    Favorite(
                        type = FavoriteTargetType.REGIONAL_GUIDE,
                        targetId = snapshot.targetId,
                        savedAtMillis = 2L,
                    )
                true
            }
        }

        override suspend fun removeFavorite(targetId: String) = Unit
    }

    private class FakeHomeRegionalGuidePrimaryFavoriteRepository(
        initialPrimaryTargetId: String? = null,
        initialLastSelectedTargetId: String? = null,
        private var clearFailureCount: Int = 0,
    ) : HomeRegionalGuidePrimaryFavoriteRepository {
        val primaryTargetId = MutableStateFlow(initialPrimaryTargetId)
        val lastSelectedTargetId = MutableStateFlow(initialLastSelectedTargetId)

        override fun observePrimaryFavoriteTargetId(): Flow<String?> = primaryTargetId

        override fun observeLastSelectedFavoriteTargetId(): Flow<String?> = lastSelectedTargetId

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

        override suspend fun setLastSelectedFavoriteTargetId(targetId: String) {
            lastSelectedTargetId.value = targetId
        }

        override suspend fun clearLastSelectedFavoriteTargetId() {
            lastSelectedTargetId.value = null
        }

        override suspend fun clearLastSelectedFavoriteTargetIdIfMatches(targetId: String) {
            if (lastSelectedTargetId.value == targetId) {
                lastSelectedTargetId.value = null
            }
        }

        override suspend fun clearPrimaryAndLastSelectedFavoriteTargetIdsIfMatches(
            targetIds: Collection<String>,
        ) {
            if (clearFailureCount > 0) {
                clearFailureCount -= 1
                throw IllegalStateException("대표 저장값 정리 실패")
            }
            val targetIdSet = targetIds.toSet()
            if (primaryTargetId.value in targetIdSet) {
                primaryTargetId.value = null
            }
            if (lastSelectedTargetId.value in targetIdSet) {
                lastSelectedTargetId.value = null
            }
        }
    }
}
