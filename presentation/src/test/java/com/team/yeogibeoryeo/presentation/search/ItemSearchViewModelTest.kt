package com.team.yeogibeoryeo.presentation.search

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoritesUseCase
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalInstruction
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.repository.DisposalItemGuideRepository
import com.team.yeogibeoryeo.domain.item.repository.HomeQuickCategoryRepository
import com.team.yeogibeoryeo.domain.item.usecase.GetDisposalCategoryGuidesUseCase
import com.team.yeogibeoryeo.domain.item.usecase.LimitHomeQuickCategoriesUseCase
import com.team.yeogibeoryeo.domain.item.usecase.ObserveHomeQuickCategoriesUseCase
import com.team.yeogibeoryeo.domain.item.usecase.SearchDisposalItemGuidesUseCase
import com.team.yeogibeoryeo.domain.item.usecase.ToggleHomeQuickCategoryUseCase
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.search.components.quickCategoryOrder
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ItemSearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `빈 검색어는 검색하지 않고 초기 상태를 유지한다`() =
        runTest {
            val repository = FakeRepository()
            val viewModel = createViewModel(repository)

            viewModel.onQueryChange("   ")
            viewModel.search()

            assertEquals(0, repository.searchCallCount)
            assertFalse(viewModel.uiState.value.hasSearched)
            assertEquals(emptyList<DisposalItemGuide>(), viewModel.uiState.value.guides)
            assertNull(viewModel.uiState.value.errorMessageResId)
        }

    @Test
    fun `검색 성공 시 결과 목록을 노출한다`() =
        runTest {
            val expected = listOf(sampleGuide("유리병"))
            val repository = FakeRepository(onSearch = { expected })
            val viewModel = createViewModel(repository)

            viewModel.onQueryChange("유리")
            viewModel.search()

            assertEquals(listOf("유리"), repository.queries)
            assertEquals(expected, viewModel.uiState.value.guides)
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.errorMessageResId)
        }

    @Test
    fun `검색 성공 시 검색 결과 버전을 증가시킨다`() =
        runTest {
            val repository = FakeRepository(onSearch = { listOf(sampleGuide(it)) })
            val viewModel = createViewModel(repository)

            viewModel.search("유리")
            val firstVersion = viewModel.uiState.value.searchResultVersion
            viewModel.search("비닐")

            assertEquals(1, firstVersion)
            assertEquals(2, viewModel.uiState.value.searchResultVersion)
        }

    @Test
    fun `검색 실패 시 에러 리소스 ID를 저장한다`() =
        runTest {
            val repository = FakeRepository(onSearch = { error("network") })
            val viewModel = createViewModel(repository)

            viewModel.onQueryChange("건전지")
            viewModel.search()

            assertEquals(
                R.string.search_load_failed_message,
                viewModel.uiState.value.errorMessageResId
            )
            assertEquals(emptyList<DisposalItemGuide>(), viewModel.uiState.value.guides)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `검색어 변경 시 기존 에러를 초기화한다`() =
        runTest {
            val repository = FakeRepository(onSearch = { error("network") })
            val viewModel = createViewModel(repository)

            viewModel.onQueryChange("건전지")
            viewModel.search()
            viewModel.onQueryChange("비닐")

            assertNull(viewModel.uiState.value.errorMessageResId)
            assertEquals("비닐", viewModel.uiState.value.query)
        }

    @Test
    fun `검색어 변경 시 이전 결과를 초기화하고 초기 상태로 돌아간다`() =
        runTest {
            val repository = FakeRepository(onSearch = { listOf(sampleGuide("유리병")) })
            val viewModel = createViewModel(repository)

            viewModel.onQueryChange("유리")
            viewModel.search()
            viewModel.onQueryChange("비닐")

            assertEquals("비닐", viewModel.uiState.value.query)
            assertEquals(emptyList<DisposalItemGuide>(), viewModel.uiState.value.guides)
            assertFalse(viewModel.uiState.value.hasSearched)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `나중 검색이 시작되면 이전 검색 결과는 반영되지 않는다`() =
        runTest {
            val firstResult = CompletableDeferred<List<DisposalItemGuide>>()
            val repository =
                FakeRepository(
                    onSearch = { query ->
                        when (query) {
                            "유리" -> firstResult.await()
                            "비닐" -> listOf(sampleGuide("비닐"))
                            else -> emptyList()
                        }
                    },
                )
            val viewModel = createViewModel(repository)

            viewModel.onQueryChange("유리")
            viewModel.search()
            viewModel.onQueryChange("비닐")
            viewModel.search()
            advanceUntilIdle()

            assertEquals(listOf("비닐"), viewModel.uiState.value.guides.map { it.name })

            firstResult.complete(listOf(sampleGuide("유리병")))
            advanceUntilIdle()

            assertEquals(listOf("비닐"), viewModel.uiState.value.guides.map { it.name })
        }

    @Test
    fun `검색 취소는 에러 상태로 표시하지 않는다`() =
        runTest {
            val firstResult = CompletableDeferred<List<DisposalItemGuide>>()
            val repository =
                FakeRepository(
                    onSearch = { query ->
                        when (query) {
                            "유리" -> firstResult.await()
                            "비닐" -> listOf(sampleGuide("비닐"))
                            else -> emptyList()
                        }
                    },
                )
            val viewModel = createViewModel(repository)

            viewModel.onQueryChange("유리")
            viewModel.search()
            viewModel.onQueryChange("비닐")
            viewModel.search()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.errorMessageResId)
            assertEquals(listOf("비닐"), viewModel.uiState.value.guides.map { it.name })
        }

    @Test
    fun `카테고리 조회 취소는 에러 상태로 표시하지 않는다`() =
        runTest {
            val firstResult = CompletableDeferred<List<DisposalItemGuide>>()
            val repository =
                FakeRepository(
                    onCategory = { category ->
                        when (category) {
                            DisposalCategory.VINYL -> firstResult.await()
                            DisposalCategory.PAPER -> listOf(sampleGuide("종이"))
                            else -> emptyList()
                        }
                    },
                )
            val viewModel = createViewModel(repository)
            val event = async(start = CoroutineStart.UNDISPATCHED) { viewModel.events.first() }

            viewModel.openCategoryGuide(RepresentativeGuideCategory.VINYL)
            viewModel.openCategoryGuide(RepresentativeGuideCategory.PAPER)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.errorMessageResId)
            assertEquals(
                "종이",
                (event.await() as ItemSearchEvent.NavigateToGuide).guide.name,
            )
        }

    @Test
    fun `카테고리 선택 시 대표 가이드 이동 이벤트를 발행한다`() =
        runTest {
            val expected = listOf(
                sampleGuide("비닐봉투"),
                sampleGuide(RepresentativeGuideCategory.VINYL.representativeGuideName),
            )
            val repository = FakeRepository(onCategory = { expected })
            val viewModel = createViewModel(repository)
            val event = async(start = CoroutineStart.UNDISPATCHED) { viewModel.events.first() }

            viewModel.openCategoryGuide(RepresentativeGuideCategory.VINYL)
            advanceUntilIdle()

            assertEquals(listOf(DisposalCategory.VINYL), repository.requestedCategories)
            assertEquals(
                RepresentativeGuideCategory.VINYL.representativeGuideName,
                (event.await() as ItemSearchEvent.NavigateToGuide).guide.name,
            )
            assertEquals("", viewModel.uiState.value.query)
            assertEquals(emptyList<DisposalItemGuide>(), viewModel.uiState.value.guides)
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.errorMessageResId)
        }

    @Test
    fun `빠른 분류 펼침 상태와 접힌 슬롯 수와 스크롤 복원 위치를 상태에 저장한다`() =
        runTest {
            val viewModel = createViewModel(FakeRepository())

            viewModel.expandQuickCategory(
                collapsedItemCount = 8,
                firstVisibleItemIndex = 3,
                firstVisibleItemScrollOffset = 40,
            )

            assertEquals(8, viewModel.uiState.value.quickCategoryFixedCollapsedItemCount)
            assertEquals(3, viewModel.uiState.value.quickCategoryScrollRestoreIndex)
            assertEquals(40, viewModel.uiState.value.quickCategoryScrollRestoreOffset)
            assertEquals(true, viewModel.uiState.value.isQuickCategoryExpanded)

            viewModel.resetQuickCategoryFixedCollapsedItemCountIfCollapsed()

            assertEquals(8, viewModel.uiState.value.quickCategoryFixedCollapsedItemCount)

            viewModel.collapseQuickCategory()

            assertEquals(false, viewModel.uiState.value.isQuickCategoryExpanded)
            assertEquals(1, viewModel.uiState.value.quickCategoryScrollRestoreVersion)

            viewModel.resetQuickCategoryFixedCollapsedItemCountIfCollapsed()

            assertEquals(0, viewModel.uiState.value.quickCategoryFixedCollapsedItemCount)
        }

    @Test
    fun `카테고리 대표 가이드가 없으면 첫 가이드 이동 이벤트를 발행한다`() =
        runTest {
            val expected = sampleGuide("첫 번째 가이드")
            val repository =
                FakeRepository(onCategory = { listOf(expected, sampleGuide("두 번째 가이드")) })
            val viewModel = createViewModel(repository)
            val event = async(start = CoroutineStart.UNDISPATCHED) { viewModel.events.first() }

            viewModel.openCategoryGuide(RepresentativeGuideCategory.VINYL)
            advanceUntilIdle()

            assertEquals(expected, (event.await() as ItemSearchEvent.NavigateToGuide).guide)
        }

    @Test
    fun `검색 결과를 초기화하면 쿼리와 결과가 삭제되고 초기 상태로 돌아간다`() =
        runTest {
            val repository = FakeRepository(onSearch = { listOf(sampleGuide("유리병")) })
            val viewModel = createViewModel(repository)

            viewModel.onQueryChange("유리")
            viewModel.search()
            viewModel.clearSearch()

            assertEquals("", viewModel.uiState.value.query)
            assertEquals(emptyList<DisposalItemGuide>(), viewModel.uiState.value.guides)
            assertFalse(viewModel.uiState.value.hasSearched)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `카테고리 가이드 조회 실패 시 에러 리소스 ID를 저장한다`() =
        runTest {
            val repository = FakeRepository(onCategory = { error("category failure") })
            val viewModel = createViewModel(repository)

            viewModel.openCategoryGuide(RepresentativeGuideCategory.VINYL)
            advanceUntilIdle()

            assertEquals(
                R.string.search_load_failed_message,
                viewModel.uiState.value.errorMessageResId
            )
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `즐겨찾기한 검색 결과 id를 상태에 반영한다`() =
        runTest {
            val favoriteRepository =
                FakeFavoriteRepository(
                    initialFavorites =
                        listOf(
                            Favorite(
                                type = FavoriteTargetType.ITEM_GUIDE,
                                targetId = "유리병",
                                savedAtMillis = 1L,
                            ),
                        ),
                )
            val viewModel = createViewModel(FakeRepository(), favoriteRepository)
            advanceUntilIdle()

            assertEquals(setOf("유리병"), viewModel.uiState.value.favoriteGuideIds)
        }

    @Test
    fun `홈 표시 분류를 상태에 반영한다`() =
        runTest {
            val homeQuickCategoryRepository =
                FakeHomeQuickCategoryRepository(
                    initialCategories = listOf(DisposalCategory.ELECTRONICS),
                )
            val viewModel = createViewModel(
                repository = FakeRepository(),
                homeQuickCategoryRepository = homeQuickCategoryRepository,
            )
            advanceUntilIdle()

            assertEquals(
                listOf(RepresentativeGuideCategory.ELECTRONICS),
                viewModel.uiState.value.homeQuickCategories,
            )
        }

    @Test
    fun `선택한 분류가 없으면 홈 빠른 분류는 기본 순서를 노출한다`() {
        val uiState = ItemSearchUiState()

        assertEquals(quickCategoryOrder, uiState.quickCategories)
    }

    @Test
    fun `선택한 분류가 있으면 홈 빠른 분류는 선택한 분류를 앞에 노출한다`() {
        val selectedCategories =
            listOf(
                RepresentativeGuideCategory.ELECTRONICS,
                RepresentativeGuideCategory.BATTERY,
            )

        val uiState = ItemSearchUiState(homeQuickCategories = selectedCategories)

        assertEquals(selectedCategories, uiState.quickCategories.take(selectedCategories.size))
        assertEquals(quickCategoryOrder.size, uiState.quickCategories.size)
    }

    @Test
    fun `홈 표시 분류 토글 시 저장소에 분류를 전달한다`() =
        runTest {
            val homeQuickCategoryRepository = FakeHomeQuickCategoryRepository()
            val viewModel = createViewModel(
                repository = FakeRepository(),
                homeQuickCategoryRepository = homeQuickCategoryRepository,
            )

            viewModel.toggleHomeQuickCategory(
                category = RepresentativeGuideCategory.BATTERY,
                maxSelectedCount = 1,
            )
            advanceUntilIdle()

            assertEquals(listOf(DisposalCategory.BATTERY), homeQuickCategoryRepository.toggledHomeQuickCategories)
            assertEquals(
                listOf(RepresentativeGuideCategory.BATTERY),
                viewModel.uiState.value.homeQuickCategories,
            )
        }

    @Test
    fun `홈 표시 분류 토글 시 최대 개수 이상 추가하지 않는다`() =
        runTest {
            val homeQuickCategoryRepository =
                FakeHomeQuickCategoryRepository(
                    initialCategories = listOf(DisposalCategory.BATTERY),
                )
            val viewModel = createViewModel(
                repository = FakeRepository(),
                homeQuickCategoryRepository = homeQuickCategoryRepository,
            )
            advanceUntilIdle()

            viewModel.toggleHomeQuickCategory(
                category = RepresentativeGuideCategory.ELECTRONICS,
                maxSelectedCount = 1,
            )
            advanceUntilIdle()

            assertEquals(
                listOf(RepresentativeGuideCategory.BATTERY),
                viewModel.uiState.value.homeQuickCategories,
            )
        }

    @Test
    fun `홈 표시 분류 제한 시 앞에서부터 최대 개수만 유지한다`() =
        runTest {
            val homeQuickCategoryRepository =
                FakeHomeQuickCategoryRepository(
                    initialCategories = listOf(
                        DisposalCategory.BATTERY,
                        DisposalCategory.ELECTRONICS,
                    ),
                )
            val viewModel = createViewModel(
                repository = FakeRepository(),
                homeQuickCategoryRepository = homeQuickCategoryRepository,
            )
            advanceUntilIdle()

            viewModel.limitHomeQuickCategories(maxSelectedCount = 1)
            advanceUntilIdle()

            assertEquals(
                listOf(RepresentativeGuideCategory.BATTERY),
                viewModel.uiState.value.homeQuickCategories,
            )
        }

    private fun createViewModel(
        repository: FakeRepository,
        favoriteRepository: FakeFavoriteRepository = FakeFavoriteRepository(),
        homeQuickCategoryRepository: FakeHomeQuickCategoryRepository = FakeHomeQuickCategoryRepository(),
    ) =
        ItemSearchViewModel(
            SearchDisposalItemGuidesUseCase(repository),
            GetDisposalCategoryGuidesUseCase(repository),
            ToggleHomeQuickCategoryUseCase(homeQuickCategoryRepository),
            LimitHomeQuickCategoriesUseCase(homeQuickCategoryRepository),
            ObserveFavoritesUseCase(favoriteRepository),
            ObserveHomeQuickCategoriesUseCase(homeQuickCategoryRepository),
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

    private class FakeRepository(
        private val onSearch: suspend (String) -> List<DisposalItemGuide> = { emptyList() },
        private val onCategory: suspend (DisposalCategory) -> List<DisposalItemGuide> = { emptyList() },
    ) : DisposalItemGuideRepository {
        val queries = mutableListOf<String>()
        val requestedCategories = mutableListOf<DisposalCategory>()
        var searchCallCount = 0

        override suspend fun searchItemGuides(query: String): List<DisposalItemGuide> {
            searchCallCount += 1
            queries += query
            return onSearch(query)
        }

        override suspend fun getCategoryGuides(category: DisposalCategory): List<DisposalItemGuide> {
            requestedCategories += category
            return onCategory(category)
        }

        override suspend fun getItemGuide(guideId: String): DisposalItemGuide? = null

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

    private class FakeHomeQuickCategoryRepository(
        initialCategories: List<DisposalCategory> = emptyList(),
    ) : HomeQuickCategoryRepository {
        private val categories = MutableStateFlow(initialCategories)
        val toggledHomeQuickCategories = mutableListOf<DisposalCategory>()

        override fun observeHomeQuickCategories(): Flow<List<DisposalCategory>> = categories

        override suspend fun toggleHomeQuickCategory(
            category: DisposalCategory,
            maxSelectedCount: Int,
        ) {
            toggledHomeQuickCategories += category
            if (category in categories.value) {
                categories.value = categories.value - category
            } else if (categories.value.size >= maxSelectedCount.coerceAtLeast(0)) {
                return
            } else {
                categories.value = categories.value + category
            }
        }

        override suspend fun limitHomeQuickCategories(maxSelectedCount: Int) {
            categories.value = categories.value.take(maxSelectedCount.coerceAtLeast(0))
        }
    }
}
