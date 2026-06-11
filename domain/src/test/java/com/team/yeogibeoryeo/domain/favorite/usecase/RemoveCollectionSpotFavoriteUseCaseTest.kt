package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.model.CollectionSpotFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteRepository
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RemoveCollectionSpotFavoriteUseCaseTest {
    @Test
    fun `targetId로 공통 Favorite와 수거 장소 스냅샷을 함께 삭제한다`() =
        runBlocking {
            val targetId = "spot-1"
            val collectionSpotFavoriteRepository =
                FakeCollectionSpotFavoriteRepository(
                    favoriteTargetIds = setOf(targetId),
                    initialSnapshots =
                        listOf(
                            CollectionSpotFavoriteSnapshot(
                                targetId = targetId,
                                name = "폐건전지 수거함",
                                type = CollectionSpotType.BATTERY_BIN,
                                address = "서울특별시 영등포구 문래동",
                                detailLocation = "주민센터 앞",
                                coordinate = Coordinate(latitude = 37.5, longitude = 126.9),
                            ),
                        ),
                )
            val useCase =
                RemoveCollectionSpotFavoriteUseCase(
                    collectionSpotFavoriteRepository = collectionSpotFavoriteRepository,
                )

            useCase(targetId)

            assertFalse(collectionSpotFavoriteRepository.isFavorite(FavoriteTargetType.COLLECTION_SPOT, targetId))
            assertEquals(emptyList<CollectionSpotFavoriteSnapshot>(), collectionSpotFavoriteRepository.snapshots.value)
        }

    private class FakeCollectionSpotFavoriteRepository(
        favoriteTargetIds: Set<String> = emptySet(),
        initialSnapshots: List<CollectionSpotFavoriteSnapshot> = emptyList(),
    ) : CollectionSpotFavoriteRepository {
        private val favoriteIds = MutableStateFlow(favoriteTargetIds)
        val snapshots = MutableStateFlow(initialSnapshots)

        override suspend fun toggleFavorite(spot: CollectionSpot): Boolean = false

        override suspend fun removeFavorite(targetId: String) {
            favoriteIds.value = favoriteIds.value - targetId
            snapshots.value = snapshots.value.filterNot { it.targetId == targetId }
        }

        fun isFavorite(
            type: FavoriteTargetType,
            targetId: String,
        ): Boolean = type == FavoriteTargetType.COLLECTION_SPOT && targetId in favoriteIds.value
    }
}
