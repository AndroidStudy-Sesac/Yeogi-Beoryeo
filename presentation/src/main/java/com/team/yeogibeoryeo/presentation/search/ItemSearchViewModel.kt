package com.team.yeogibeoryeo.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoritesUseCase
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.usecase.GetDisposalCategoryGuidesUseCase
import com.team.yeogibeoryeo.domain.item.usecase.LimitHomeQuickCategoriesUseCase
import com.team.yeogibeoryeo.domain.item.usecase.ObserveHomeQuickCategoriesUseCase
import com.team.yeogibeoryeo.domain.item.usecase.SearchDisposalItemGuidesUseCase
import com.team.yeogibeoryeo.domain.item.usecase.ToggleHomeQuickCategoryUseCase
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ItemSearchViewModel
@Inject
constructor(
    private val searchDisposalItemGuidesUseCase: SearchDisposalItemGuidesUseCase,
    private val getDisposalCategoryGuidesUseCase: GetDisposalCategoryGuidesUseCase,
    private val toggleHomeQuickCategoryUseCase: ToggleHomeQuickCategoryUseCase,
    private val limitHomeQuickCategoriesUseCase: LimitHomeQuickCategoriesUseCase,
    observeFavoritesUseCase: ObserveFavoritesUseCase,
    observeHomeQuickCategoriesUseCase: ObserveHomeQuickCategoriesUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ItemSearchUiState())
    val uiState: StateFlow<ItemSearchUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<ItemSearchEvent>()
    val events: SharedFlow<ItemSearchEvent> = _events.asSharedFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            observeFavoritesUseCase(FavoriteTargetType.ITEM_GUIDE)
                .collect { favorites ->
                    _uiState.update { state ->
                        state.copy(
                            favoriteGuideIds =
                                favorites
                                    .map { it.targetId }
                                    .toSet(),
                        )
                    }
                }
        }
        viewModelScope.launch {
            observeHomeQuickCategoriesUseCase()
                .collect { categories ->
                    _uiState.update { state ->
                        state.copy(
                            homeQuickCategories =
                                categories.map(RepresentativeGuideCategory::fromDisposalCategory),
                        )
                    }
                }
        }
    }

    fun onQueryChange(query: String) {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                query = query,
                guides = emptyList(),
                isLoading = false,
                hasSearched = false,
                errorMessageResId = null,
            )
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                query = "",
                guides = emptyList(),
                hasSearched = false,
                errorMessageResId = null,
            )
        }
    }

    fun search() {
        val query = _uiState.value.query.trim()
        search(query)
    }

    fun search(query: String) {
        _uiState.update { it.copy(query = query) }
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            _uiState.update {
                it.copy(
                    guides = emptyList(),
                    hasSearched = false,
                    errorMessageResId = null,
                )
            }
            return
        }

        searchJob?.cancel()
        searchJob =
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        hasSearched = true,
                        errorMessageResId = null,
                    )
                }

                runCatchingCancellable { searchDisposalItemGuidesUseCase(trimmedQuery) }
                    .onSuccess { guides ->
                        _uiState.update {
                            it.copy(
                                guides = guides,
                                searchResultVersion = it.searchResultVersion + 1,
                                isLoading = false,
                            )
                        }
                    }
                    .onFailure {
                        _uiState.update {
                            it.copy(
                                guides = emptyList(),
                                isLoading = false,
                                errorMessageResId = R.string.search_load_failed_message,
                            )
                        }
                    }
            }
    }

    fun openCategoryGuide(category: RepresentativeGuideCategory) {
        searchJob?.cancel()
        searchJob =
            viewModelScope.launch {
                _uiState.update { it.copy(errorMessageResId = null) }

                runCatchingCancellable { getDisposalCategoryGuidesUseCase(category.disposalCategory) }
                    .onSuccess { guides ->
                        val representativeGuide =
                            guides.firstOrNull { it.name == category.representativeGuideName }
                                ?: guides.firstOrNull()

                        if (representativeGuide != null) {
                            _events.emit(ItemSearchEvent.NavigateToGuide(representativeGuide))
                        }
                    }
                    .onFailure {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessageResId = R.string.search_load_failed_message,
                            )
                        }
                    }
            }
    }

    fun expandQuickCategory(
        collapsedItemCount: Int,
        firstVisibleItemIndex: Int,
        firstVisibleItemScrollOffset: Int,
    ) {
        _uiState.update {
            it.copy(
                isQuickCategoryExpanded = true,
                quickCategoryFixedCollapsedItemCount = collapsedItemCount,
                quickCategoryScrollRestoreIndex = firstVisibleItemIndex,
                quickCategoryScrollRestoreOffset = firstVisibleItemScrollOffset,
            )
        }
    }

    fun collapseQuickCategory() {
        _uiState.update {
            it.copy(
                isQuickCategoryExpanded = false,
                quickCategoryScrollRestoreVersion = it.quickCategoryScrollRestoreVersion + 1,
            )
        }
    }

    fun resetQuickCategoryFixedCollapsedItemCountIfCollapsed() {
        _uiState.update {
            if (it.isQuickCategoryExpanded) {
                it
            } else {
                it.copy(quickCategoryFixedCollapsedItemCount = 0)
            }
        }
    }

    fun toggleHomeQuickCategory(
        category: RepresentativeGuideCategory,
        maxSelectedCount: Int,
    ) {
        viewModelScope.launch {
            toggleHomeQuickCategoryUseCase(category.disposalCategory, maxSelectedCount)
        }
    }

    fun limitHomeQuickCategories(maxSelectedCount: Int) {
        viewModelScope.launch {
            limitHomeQuickCategoriesUseCase(maxSelectedCount)
        }
    }

    private suspend fun <T> runCatchingCancellable(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            Result.failure(exception)
        }
}
