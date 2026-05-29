package com.team.yeogibeoryeo.presentation.search

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoriteUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ToggleFavoriteUseCase
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalInstruction
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.repository.DisposalItemGuideRepository
import com.team.yeogibeoryeo.domain.item.usecase.GetDisposalItemGuideUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ItemGuideDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `가이드 로드 성공 시 상세 상태와 즐겨찾기 여부를 반영한다`() =
        runTest {
            val guide = sampleGuide("유리병")
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
                    itemRepository = FakeItemRepository(guide = guide),
                    favoriteRepository = favoriteRepository,
                )

            viewModel.loadGuide(guide.id)
            advanceUntilIdle()

            val state = viewModel.uiState.value as ItemGuideDetailUiState.Success
            assertEquals(guide, state.guide)
            assertTrue(state.isFavorite)
        }

    @Test
    fun `즐겨찾기를 추가하면 최종 상태 기준 메시지를 저장한다`() =
        runTest {
            val guide = sampleGuide("유리병")
            val viewModel =
                createViewModel(
                    itemRepository = FakeItemRepository(guide = guide),
                    favoriteRepository = FakeFavoriteRepository(),
                )

            viewModel.loadGuide(guide.id)
            advanceUntilIdle()
            viewModel.toggleFavorite()
            advanceUntilIdle()

            val state = viewModel.uiState.value as ItemGuideDetailUiState.Success
            assertTrue(state.isFavorite)
            assertEquals("즐겨찾기에 추가되었습니다", state.favoriteMessage)
        }

    @Test
    fun `즐겨찾기를 해제하면 최종 상태 기준 메시지를 저장한다`() =
        runTest {
            val guide = sampleGuide("유리병")
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
                    itemRepository = FakeItemRepository(guide = guide),
                    favoriteRepository = favoriteRepository,
                )

            viewModel.loadGuide(guide.id)
            advanceUntilIdle()
            viewModel.toggleFavorite()
            advanceUntilIdle()

            val state = viewModel.uiState.value as ItemGuideDetailUiState.Success
            assertFalse(state.isFavorite)
            assertEquals("즐겨찾기에서 제외되었습니다", state.favoriteMessage)
        }

    @Test
    fun `새 가이드를 로드하면 이전 로드 결과는 반영하지 않는다`() =
        runTest {
            val oldGuide = sampleGuide("유리병")
            val newGuide = sampleGuide("종이팩")
            val oldResult = CompletableDeferred<DisposalItemGuide?>()
            val viewModel =
                createViewModel(
                    itemRepository =
                        FakeItemRepository { guideId ->
                            when (guideId) {
                                oldGuide.id -> oldResult.await()
                                newGuide.id -> newGuide
                                else -> null
                            }
                        },
                    favoriteRepository = FakeFavoriteRepository(),
                )

            viewModel.loadGuide(oldGuide.id)
            viewModel.loadGuide(newGuide.id)
            advanceUntilIdle()

            var state = viewModel.uiState.value as ItemGuideDetailUiState.Success
            assertEquals(newGuide, state.guide)

            oldResult.complete(oldGuide)
            advanceUntilIdle()

            state = viewModel.uiState.value as ItemGuideDetailUiState.Success
            assertEquals(newGuide, state.guide)
        }

    private fun createViewModel(
        itemRepository: FakeItemRepository,
        favoriteRepository: FakeFavoriteRepository,
    ): ItemGuideDetailViewModel =
        ItemGuideDetailViewModel(
            getDisposalItemGuideUseCase = GetDisposalItemGuideUseCase(itemRepository),
            observeFavoriteUseCase = ObserveFavoriteUseCase(favoriteRepository),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(favoriteRepository),
        )

    private fun sampleGuide(name: String): DisposalItemGuide =
        DisposalItemGuide(
            id = name,
            name = name,
            category = DisposalCategory.GLASS,
            subCategory = null,
            instructions = listOf(DisposalInstruction(method = "재활용폐기물")),
            steps = emptyList(),
            cautions = emptyList(),
            tip = null,
            isRecyclable = true,
            relatedSpotTypes = emptyList(),
        )

    private class FakeItemRepository(
        private val onGetItemGuide: suspend (String) -> DisposalItemGuide?,
    ) : DisposalItemGuideRepository {
        constructor(guide: DisposalItemGuide?) : this({ guideId -> guide?.takeIf { it.id == guideId } })

        override suspend fun searchItemGuides(query: String): List<DisposalItemGuide> = emptyList()

        override suspend fun getItemGuide(guideId: String): DisposalItemGuide? = onGetItemGuide(guideId)

        override suspend fun getCategoryGuides(category: DisposalCategory): List<DisposalItemGuide> = emptyList()

        override fun getCategories(): List<DisposalCategory> = emptyList()
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
