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
import com.team.yeogibeoryeo.presentation.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
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
    fun `같은 가이드를 다시 로드하면 기존 상태를 유지하고 재조회하지 않는다`() =
        runTest {
            val guide = sampleGuide("유리병")
            val itemRepository = FakeItemRepository(guide = guide)
            val viewModel =
                createViewModel(
                    itemRepository = itemRepository,
                    favoriteRepository = FakeFavoriteRepository(),
                )

            viewModel.loadGuide(guide.id)
            advanceUntilIdle()
            val loadedState = viewModel.uiState.value

            viewModel.loadGuide(guide.id)

            assertSame(loadedState, viewModel.uiState.value)
            assertEquals(listOf(guide.id), itemRepository.requestedGuideIds)
        }

    @Test
    fun `다른 가이드 로드가 예약된 상태에서 현재 가이드를 다시 요청하면 예약 작업을 취소한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val currentGuide = sampleGuide("유리병")
            val pendingGuide = sampleGuide("종이팩")
            val itemRepository =
                FakeItemRepository { guideId ->
                    when (guideId) {
                        currentGuide.id -> currentGuide
                        pendingGuide.id -> pendingGuide
                        else -> null
                    }
                }
            val viewModel =
                createViewModel(
                    itemRepository = itemRepository,
                    favoriteRepository = FakeFavoriteRepository(),
                )

            viewModel.loadGuide(currentGuide.id)
            advanceUntilIdle()
            val loadedState = viewModel.uiState.value

            viewModel.loadGuide(pendingGuide.id)
            viewModel.loadGuide(currentGuide.id)
            advanceUntilIdle()

            assertSame(loadedState, viewModel.uiState.value)
            assertEquals(listOf(currentGuide.id), itemRepository.requestedGuideIds)
        }

    @Test
    fun `즐겨찾기를 추가하면 추가 메시지 이벤트를 보낸다`() =
        runTest {
            val guide = sampleGuide("유리병")
            val viewModel =
                createViewModel(
                    itemRepository = FakeItemRepository(guide = guide),
                    favoriteRepository = FakeFavoriteRepository(),
                )

            viewModel.loadGuide(guide.id)
            advanceUntilIdle()
            val event = async(start = CoroutineStart.UNDISPATCHED) { viewModel.events.first() }
            viewModel.toggleFavorite()
            advanceUntilIdle()

            val state = viewModel.uiState.value as ItemGuideDetailUiState.Success
            assertTrue(state.isFavorite)
            assertEquals(
                R.string.item_guide_detail_favorite_added_message,
                (event.await() as ItemGuideDetailEvent.ShowMessage).messageResId,
            )
        }

    @Test
    fun `즐겨찾기를 해제하면 해제 메시지 이벤트를 보낸다`() =
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
            val event = async(start = CoroutineStart.UNDISPATCHED) { viewModel.events.first() }
            viewModel.toggleFavorite()
            advanceUntilIdle()

            val state = viewModel.uiState.value as ItemGuideDetailUiState.Success
            assertFalse(state.isFavorite)
            assertEquals(
                R.string.item_guide_detail_favorite_removed_message,
                (event.await() as ItemGuideDetailEvent.ShowMessage).messageResId,
            )
        }

    @Test
    fun `즐겨찾기 변경에 실패하면 실패 메시지 이벤트를 보낸다`() =
        runTest {
            val guide = sampleGuide("유리병")
            val viewModel =
                createViewModel(
                    itemRepository = FakeItemRepository(guide = guide),
                    favoriteRepository =
                        FakeFavoriteRepository(toggleError = IllegalStateException("저장 실패")),
                )

            viewModel.loadGuide(guide.id)
            advanceUntilIdle()
            val event = async(start = CoroutineStart.UNDISPATCHED) { viewModel.events.first() }
            viewModel.toggleFavorite()
            advanceUntilIdle()

            val state = viewModel.uiState.value as ItemGuideDetailUiState.Success
            val messageEvent = event.await() as ItemGuideDetailEvent.ShowMessage
            assertFalse(state.isFavorite)
            assertEquals(
                R.string.item_guide_detail_favorite_update_failed_message,
                messageEvent.messageResId,
            )
            assertEquals(ItemGuideDetailMessageIcon.Warning, messageEvent.icon)
        }

    @Test
    fun `공식 안내 실행 실패 메시지 이벤트를 보낸다`() =
        runTest {
            val viewModel =
                createViewModel(
                    itemRepository = FakeItemRepository(guide = sampleGuide("전기밥솥")),
                    favoriteRepository = FakeFavoriteRepository(),
                )

            val event = async(start = CoroutineStart.UNDISPATCHED) { viewModel.events.first() }
            viewModel.showOfficialGuideOpenFailedMessage()
            advanceUntilIdle()

            val messageEvent = event.await() as ItemGuideDetailEvent.ShowMessage
            assertEquals(
                R.string.item_guide_action_open_failed_message,
                messageEvent.messageResId,
            )
            assertEquals(ItemGuideDetailMessageIcon.Warning, messageEvent.icon)
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
        val requestedGuideIds = mutableListOf<String>()

        constructor(guide: DisposalItemGuide?) : this({ guideId -> guide?.takeIf { it.id == guideId } })

        override suspend fun searchItemGuides(query: String): List<DisposalItemGuide> = emptyList()

        override suspend fun getItemGuide(guideId: String): DisposalItemGuide? {
            requestedGuideIds += guideId
            return onGetItemGuide(guideId)
        }

        override suspend fun getCategoryGuides(category: DisposalCategory): List<DisposalItemGuide> = emptyList()

        override fun getCategories(): List<DisposalCategory> = emptyList()
    }

    private class FakeFavoriteRepository(
        initialFavorites: List<Favorite> = emptyList(),
        private val toggleError: Throwable? = null,
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
            toggleError?.let { throw it }
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
