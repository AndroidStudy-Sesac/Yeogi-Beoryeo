package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteRepository
import com.team.yeogibeoryeo.domain.regionalguide.repository.HomeRegionalGuidePrimaryFavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoveRegionalGuideFavoriteUseCaseTest {
    @Test
    fun `대표 저장값 정리 실패 후 같은 대상으로 재시도하면 남은 값을 정리한다`() =
        runBlocking {
            val targetId = "regional-guide-primary"
            val favoriteRepository = FakeRegionalGuideFavoriteRepository()
            val primaryFavoriteRepository =
                FakeHomeRegionalGuidePrimaryFavoriteRepository(
                    initialPrimaryTargetId = targetId,
                    initialLastSelectedTargetId = targetId,
                    clearFailureCount = 1,
                )
            val useCase =
                RemoveRegionalGuideFavoriteUseCase(
                    repository = favoriteRepository,
                    homeRegionalGuidePrimaryFavoriteRepository = primaryFavoriteRepository,
                )

            val firstResult = runCatching { useCase(targetId) }
            useCase(targetId)

            assertEquals(true, firstResult.isFailure)
            assertEquals(2, favoriteRepository.removeCallCount)
            assertEquals(null, primaryFavoriteRepository.primaryTargetId.value)
            assertEquals(null, primaryFavoriteRepository.lastSelectedTargetId.value)
        }

    private class FakeRegionalGuideFavoriteRepository : RegionalGuideFavoriteRepository {
        var removeCallCount: Int = 0
            private set

        override suspend fun toggleFavorite(snapshot: RegionalGuideFavoriteSnapshot): Boolean = false

        override suspend fun removeFavorite(targetId: String) {
            removeCallCount += 1
        }
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
