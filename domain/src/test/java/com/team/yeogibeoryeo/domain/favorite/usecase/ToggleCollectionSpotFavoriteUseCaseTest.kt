package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.model.CollectionSpotFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.model.toFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteRepository
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToggleCollectionSpotFavoriteUseCaseTest {
    @Test
    fun `즐겨찾기가 아니면 공통 Favorite와 수거 장소 스냅샷을 저장한다`() =
        runBlocking {
            val collectionSpotFavoriteRepository = FakeCollectionSpotFavoriteRepository()
            val useCase = ToggleCollectionSpotFavoriteUseCase(
                collectionSpotFavoriteRepository = collectionSpotFavoriteRepository,
            )
            val spot = sampleSpot()

            val isFavorite = useCase(spot)

            assertTrue(isFavorite)
            assertTrue(collectionSpotFavoriteRepository.isFavorite(FavoriteTargetType.COLLECTION_SPOT, spot.id))
            assertEquals(spot.id, collectionSpotFavoriteRepository.snapshots.value.single().targetId)
            assertEquals(spot.coordinate, collectionSpotFavoriteRepository.snapshots.value.single().coordinate)
        }

    @Test
    fun `이미 즐겨찾기면 공통 Favorite와 수거 장소 스냅샷을 삭제한다`() =
        runBlocking {
            val spot = sampleSpot()
            val collectionSpotFavoriteRepository = FakeCollectionSpotFavoriteRepository(
                favoriteTargetIds = setOf(spot.id),
                initialSnapshots = listOf(
                    CollectionSpotFavoriteSnapshot(
                        targetId = spot.id,
                        name = spot.name,
                        type = spot.type,
                        address = spot.address,
                        detailLocation = spot.detailLocation,
                        coordinate = spot.coordinate,
                    ),
                ),
            )
            val useCase = ToggleCollectionSpotFavoriteUseCase(
                collectionSpotFavoriteRepository = collectionSpotFavoriteRepository,
            )

            val isFavorite = useCase(spot)

            assertFalse(isFavorite)
            assertFalse(collectionSpotFavoriteRepository.isFavorite(FavoriteTargetType.COLLECTION_SPOT, spot.id))
            assertEquals(emptyList<CollectionSpotFavoriteSnapshot>(), collectionSpotFavoriteRepository.snapshots.value)
        }

    private fun sampleSpot(): CollectionSpot =
        CollectionSpot(
            id = "spot-1",
            name = "폐건전지 수거함",
            type = CollectionSpotType.BATTERY_BIN,
            address = "서울특별시 영등포구 문래동",
            detailLocation = "주민센터 앞",
            coordinate = Coordinate(latitude = 37.5, longitude = 126.9),
        )

    private class FakeCollectionSpotFavoriteRepository(
        favoriteTargetIds: Set<String> = emptySet(),
        initialSnapshots: List<CollectionSpotFavoriteSnapshot> = emptyList(),
    ) : CollectionSpotFavoriteRepository {
        private val favoriteIds = MutableStateFlow(favoriteTargetIds)
        val snapshots = MutableStateFlow(initialSnapshots)

        override suspend fun toggleFavorite(spot: CollectionSpot): Boolean {
            return if (isFavorite(FavoriteTargetType.COLLECTION_SPOT, spot.id)) {
                removeFavorite(spot.id)
                false
            } else {
                favoriteIds.value = favoriteIds.value + spot.id
                snapshots.value =
                    snapshots.value
                        .filterNot { it.targetId == spot.id } + spot.toFavoriteSnapshot()
                true
            }
        }

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
