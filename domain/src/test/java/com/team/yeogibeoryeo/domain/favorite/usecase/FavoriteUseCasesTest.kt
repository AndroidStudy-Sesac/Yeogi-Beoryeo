package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteUseCasesTest {
    @Test
    fun `ObserveFavoritesUseCase는 저장소의 즐겨찾기 목록을 반환한다`() =
        runBlocking {
            val favorite = sampleFavorite("종이")
            val repository = FakeFavoriteRepository(initialFavorites = listOf(favorite))

            val result = ObserveFavoritesUseCase(repository).invoke().first()

            assertEquals(listOf(favorite), result)
        }

    @Test
    fun `ObserveFavoritesUseCase는 대상 타입으로 즐겨찾기 목록을 필터링한다`() =
        runBlocking {
            val itemGuideFavorite = sampleFavorite("종이")
            val collectionSpotFavorite =
                sampleFavorite(
                    targetId = "spot-1",
                    type = FavoriteTargetType.COLLECTION_SPOT,
                )
            val repository =
                FakeFavoriteRepository(
                    initialFavorites = listOf(itemGuideFavorite, collectionSpotFavorite),
                )

            val result =
                ObserveFavoritesUseCase(repository)
                    .invoke(FavoriteTargetType.COLLECTION_SPOT)
                    .first()

            assertEquals(listOf(collectionSpotFavorite), result)
        }

    @Test
    fun `ObserveFavoriteUseCase는 특정 대상의 즐겨찾기 여부를 반환한다`() =
        runBlocking {
            val favorite = sampleFavorite("종이")
            val repository = FakeFavoriteRepository(initialFavorites = listOf(favorite))

            val result =
                ObserveFavoriteUseCase(repository)
                    .invoke(FavoriteTargetType.ITEM_GUIDE, "종이")
                    .first()

            assertTrue(result)
        }

    @Test
    fun `ToggleFavoriteUseCase는 즐겨찾기가 아니면 추가한다`() =
        runBlocking {
            val favorite = sampleFavorite("종이")
            val repository = FakeFavoriteRepository()

            val result = ToggleFavoriteUseCase(repository).invoke(favorite)

            assertTrue(result)
            assertEquals(listOf(favorite), repository.observeFavorites().first())
        }

    @Test
    fun `ToggleFavoriteUseCase는 이미 즐겨찾기면 제거한다`() =
        runBlocking {
            val favorite = sampleFavorite("종이")
            val repository = FakeFavoriteRepository(initialFavorites = listOf(favorite))

            val result = ToggleFavoriteUseCase(repository).invoke(favorite)

            assertFalse(result)
            assertFalse(repository.isFavorite(FavoriteTargetType.ITEM_GUIDE, "종이"))
        }

    @Test
    fun `RemoveFavoriteUseCase는 대상 타입과 id에 맞는 즐겨찾기를 제거한다`() =
        runBlocking {
            val favorite = sampleFavorite("종이")
            val otherFavorite =
                sampleFavorite(
                    targetId = "spot-1",
                    type = FavoriteTargetType.COLLECTION_SPOT,
                )
            val repository =
                FakeFavoriteRepository(initialFavorites = listOf(favorite, otherFavorite))

            RemoveFavoriteUseCase(repository).invoke(FavoriteTargetType.ITEM_GUIDE, "종이")

            assertEquals(listOf(otherFavorite), repository.observeFavorites().first())
        }

    private fun sampleFavorite(
        targetId: String,
        type: FavoriteTargetType = FavoriteTargetType.ITEM_GUIDE,
    ): Favorite =
        Favorite(
            type = type,
            targetId = targetId,
            savedAtMillis = 1L,
        )

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
}
