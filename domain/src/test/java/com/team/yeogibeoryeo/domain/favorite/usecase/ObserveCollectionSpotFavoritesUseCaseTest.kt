package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.model.CollectionSpotFavorite
import com.team.yeogibeoryeo.domain.favorite.model.CollectionSpotFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteSnapshotRepository
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveCollectionSpotFavoritesUseCaseTest {
    @Test
    fun `collection spot favorites are emitted in favorite order`() =
        runBlocking {
            val olderSnapshot = snapshot(targetId = "spot-older")
            val newerSnapshot = snapshot(targetId = "spot-newer")
            val useCase =
                ObserveCollectionSpotFavoritesUseCase(
                    favoriteRepository =
                        FakeFavoriteRepository(
                            favorites =
                                listOf(
                                    favorite(targetId = olderSnapshot.targetId, savedAtMillis = 1L),
                                    favorite(targetId = newerSnapshot.targetId, savedAtMillis = 2L),
                                ),
                        ),
                    snapshotRepository =
                        FakeCollectionSpotFavoriteSnapshotRepository(
                            snapshots = listOf(olderSnapshot, newerSnapshot),
                        ),
                )

            val result = useCase().first()

            assertEquals(
                listOf("spot-newer", "spot-older"),
                result.map { favorite -> favorite.targetId },
            )
            assertEquals(listOf(2L, 1L), result.map { favorite -> favorite.savedAtMillis })
        }

    @Test
    fun `favorite without snapshot is excluded`() =
        runBlocking {
            val useCase =
                ObserveCollectionSpotFavoritesUseCase(
                    favoriteRepository =
                        FakeFavoriteRepository(
                            favorites = listOf(favorite(targetId = "missing-snapshot")),
                        ),
                    snapshotRepository = FakeCollectionSpotFavoriteSnapshotRepository(),
                )

            assertEquals(emptyList<CollectionSpotFavorite>(), useCase().first())
        }

    @Test
    fun `snapshot without favorite is excluded`() =
        runBlocking {
            val useCase =
                ObserveCollectionSpotFavoritesUseCase(
                    favoriteRepository = FakeFavoriteRepository(),
                    snapshotRepository =
                        FakeCollectionSpotFavoriteSnapshotRepository(
                            snapshots = listOf(snapshot(targetId = "snapshot-only")),
                        ),
                )

            assertEquals(emptyList<CollectionSpotFavorite>(), useCase().first())
        }

    @Test
    fun `non collection spot favorites are excluded`() =
        runBlocking {
            val snapshot = snapshot(targetId = "spot-1")
            val useCase =
                ObserveCollectionSpotFavoritesUseCase(
                    favoriteRepository =
                        FakeFavoriteRepository(
                            favorites =
                                listOf(
                                    Favorite(
                                        type = FavoriteTargetType.ITEM_GUIDE,
                                        targetId = snapshot.targetId,
                                        savedAtMillis = 1L,
                                    ),
                                ),
                        ),
                    snapshotRepository =
                        FakeCollectionSpotFavoriteSnapshotRepository(snapshots = listOf(snapshot)),
                )

            assertEquals(emptyList<CollectionSpotFavorite>(), useCase().first())
        }

    @Test
    fun `combined result keeps favorite metadata and snapshot`() =
        runBlocking {
            val snapshot = snapshot(targetId = "spot-1")
            val useCase =
                ObserveCollectionSpotFavoritesUseCase(
                    favoriteRepository =
                        FakeFavoriteRepository(
                            favorites = listOf(favorite(targetId = snapshot.targetId, savedAtMillis = 3L)),
                        ),
                    snapshotRepository =
                        FakeCollectionSpotFavoriteSnapshotRepository(snapshots = listOf(snapshot)),
                )

            val result = useCase().first().single()

            assertEquals("spot-1", result.targetId)
            assertEquals(3L, result.savedAtMillis)
            assertEquals(snapshot, result.snapshot)
        }

    private fun favorite(
        targetId: String,
        savedAtMillis: Long = 1L,
    ): Favorite =
        Favorite(
            type = FavoriteTargetType.COLLECTION_SPOT,
            targetId = targetId,
            savedAtMillis = savedAtMillis,
        )

    private fun snapshot(targetId: String): CollectionSpotFavoriteSnapshot =
        CollectionSpotFavoriteSnapshot(
            targetId = targetId,
            name = "수거함",
            type = CollectionSpotType.BATTERY_BIN,
            address = "서울특별시 영등포구",
            detailLocation = null,
            coordinate = Coordinate(latitude = 37.5, longitude = 126.9),
        )

    private class FakeFavoriteRepository(
        favorites: List<Favorite> = emptyList(),
    ) : FavoriteRepository {
        private val favorites = MutableStateFlow(favorites)

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

        override suspend fun toggleFavorite(favorite: Favorite): Boolean = false

        override suspend fun addFavorite(favorite: Favorite) {
            favorites.value = favorites.value + favorite
        }

        override suspend fun removeFavorite(
            type: FavoriteTargetType,
            targetId: String,
        ) {
            favorites.value =
                favorites.value.filterNot { favorite -> favorite.type == type && favorite.targetId == targetId }
        }
    }

    private class FakeCollectionSpotFavoriteSnapshotRepository(
        snapshots: List<CollectionSpotFavoriteSnapshot> = emptyList(),
    ) : CollectionSpotFavoriteSnapshotRepository {
        private val snapshots = MutableStateFlow(snapshots)

        override fun observeSnapshots(): Flow<List<CollectionSpotFavoriteSnapshot>> = snapshots

        override suspend fun getSnapshot(targetId: String): CollectionSpotFavoriteSnapshot? =
            snapshots.value.firstOrNull { snapshot -> snapshot.targetId == targetId }

        override suspend fun upsertSnapshot(snapshot: CollectionSpotFavoriteSnapshot) {
            snapshots.value =
                snapshots.value
                    .filterNot { it.targetId == snapshot.targetId } + snapshot
        }

        override suspend fun deleteSnapshot(targetId: String) {
            snapshots.value = snapshots.value.filterNot { snapshot -> snapshot.targetId == targetId }
        }
    }
}
