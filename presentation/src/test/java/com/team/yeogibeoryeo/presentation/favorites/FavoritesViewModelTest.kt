package com.team.yeogibeoryeo.presentation.favorites

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoritesUseCase
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalInstruction
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.model.DisposalSubCategory
import com.team.yeogibeoryeo.domain.item.repository.DisposalItemGuideRepository
import com.team.yeogibeoryeo.domain.item.usecase.GetDisposalItemGuideUseCase
import com.team.yeogibeoryeo.presentation.favorites.mapper.FavoriteCollectionSpotUiMapper
import com.team.yeogibeoryeo.presentation.favorites.mapper.FavoriteItemGuideUiMapper
import com.team.yeogibeoryeo.presentation.favorites.mapper.FavoriteRegionalGuideUiMapper
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteTab
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteUiModel
import com.team.yeogibeoryeo.presentation.search.MainDispatcherRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `즐겨찾기 원본 가이드를 조회해 UI 모델로 변환한다`() =
        runTest {
            val guide =
                sampleGuide(
                    id = "paper-pack",
                    name = "종이팩",
                    subCategory = DisposalSubCategory.MILK_CARTON,
                )
            val viewModel =
                createViewModel(
                    favoriteRepository =
                        FakeFavoriteRepository(
                            initialFavorites =
                                listOf(
                                    Favorite(
                                        type = FavoriteTargetType.ITEM_GUIDE,
                                        targetId = guide.id,
                                        savedAtMillis = 1L,
                                    ),
                                ),
                        ),
                    itemRepository = FakeItemRepository(guides = listOf(guide)),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            assertEquals(
                listOf(
                    FavoriteUiModel(
                        type = FavoriteTargetType.ITEM_GUIDE,
                        targetId = "paper-pack",
                        title = "종이팩",
                        subtitle = "우유팩",
                    ),
                ),
                viewModel.uiState.value.itemGuideFavorites,
            )
        }

    @Test
    fun `원본 가이드를 찾을 수 없는 즐겨찾기는 UI 목록에서 제외한다`() =
        runTest {
            val viewModel =
                createViewModel(
                    favoriteRepository =
                        FakeFavoriteRepository(
                            initialFavorites =
                                listOf(
                                    Favorite(
                                        type = FavoriteTargetType.ITEM_GUIDE,
                                        targetId = "missing-guide",
                                        savedAtMillis = 1L,
                                    ),
                                ),
                        ),
                    itemRepository = FakeItemRepository(guides = emptyList()),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            assertEquals(emptyList<FavoriteUiModel>(), viewModel.uiState.value.itemGuideFavorites)
        }

    @Test
    fun `탭을 변경하면 선택된 탭 상태를 반영한다`() =
        runTest {
            val viewModel =
                createViewModel(
                    favoriteRepository = FakeFavoriteRepository(),
                    itemRepository = FakeItemRepository(guides = emptyList()),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.selectTab(FavoriteTab.COLLECTION_SPOT)
            advanceUntilIdle()

            assertEquals(FavoriteTab.COLLECTION_SPOT, viewModel.uiState.value.selectedTab)
        }

    private fun createViewModel(
        favoriteRepository: FakeFavoriteRepository,
        itemRepository: FakeItemRepository,
    ): FavoritesViewModel =
        FavoritesViewModel(
            observeFavoritesUseCase = ObserveFavoritesUseCase(favoriteRepository),
            itemGuideUiMapper = FavoriteItemGuideUiMapper(GetDisposalItemGuideUseCase(itemRepository)),
            collectionSpotUiMapper = FavoriteCollectionSpotUiMapper(),
            regionalGuideUiMapper = FavoriteRegionalGuideUiMapper(),
        )

    private fun sampleGuide(
        id: String,
        name: String,
        subCategory: DisposalSubCategory? = null,
    ): DisposalItemGuide =
        DisposalItemGuide(
            id = id,
            name = name,
            category = DisposalCategory.PAPER_PACK,
            subCategory = subCategory,
            instructions = listOf(DisposalInstruction(method = "재활용폐기물")),
            steps = emptyList(),
            cautions = emptyList(),
            tip = null,
            isRecyclable = true,
            relatedSpotTypes = emptyList(),
        )

    private class FakeItemRepository(
        guides: List<DisposalItemGuide>,
    ) : DisposalItemGuideRepository {
        private val guidesById = guides.associateBy { it.id }

        override suspend fun searchItemGuides(query: String): List<DisposalItemGuide> = emptyList()

        override suspend fun getItemGuide(guideId: String): DisposalItemGuide? = guidesById[guideId]

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
