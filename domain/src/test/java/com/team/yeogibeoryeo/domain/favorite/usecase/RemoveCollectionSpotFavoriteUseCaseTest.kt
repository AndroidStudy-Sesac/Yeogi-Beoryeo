package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.model.CollectionSpotFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteSnapshotRepository
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RemoveCollectionSpotFavoriteUseCaseTest {
    @Test
    fun `targetId로 공통 Favorite와 수거 장소 스냅샷을 함께 삭제한다`() =
        runBlocking {
            val targetId = "spot-1"
            val favoriteRepository =
                FakeFavoriteRepository(
                    initialFavorites =
                        listOf(
                            Favorite(
                                type = FavoriteTargetType.COLLECTION_SPOT,
                                targetId = targetId,
                                savedAtMillis = 1L,
                            ),
                        ),
                )
            val snapshotRepository =
                FakeCollectionSpotFavoriteSnapshotRepository(
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
                    favoriteRepository = favoriteRepository,
                    snapshotRepository = snapshotRepository,
                )

            useCase(targetId)

            assertFalse(favoriteRepository.isFavorite(FavoriteTargetType.COLLECTION_SPOT, targetId))
            assertEquals(emptyList<CollectionSpotFavoriteSnapshot>(), snapshotRepository.snapshots.value)
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
                items.any { favorite -> favorite.type == type && favorite.targetId == targetId }
            }

        override suspend fun isFavorite(
            type: FavoriteTargetType,
            targetId: String,
        ): Boolean =
            favorites.value.any { favorite -> favorite.type == type && favorite.targetId == targetId }

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
            favorites.value = favorites.value.filterNot { it.type == type && it.targetId == targetId }
        }
    }

    private class FakeCollectionSpotFavoriteSnapshotRepository(
        initialSnapshots: List<CollectionSpotFavoriteSnapshot> = emptyList(),
    ) : CollectionSpotFavoriteSnapshotRepository {
        val snapshots = MutableStateFlow(initialSnapshots)

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
}
