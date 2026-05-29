package com.team.yeogibeoryeo.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoritesUseCase
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.usecase.GetDisposalCategoryGuidesUseCase
import com.team.yeogibeoryeo.domain.item.usecase.SearchDisposalItemGuidesUseCase
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ItemSearchViewModel
@Inject
constructor(
    private val searchDisposalItemGuidesUseCase: SearchDisposalItemGuidesUseCase,
    private val getDisposalCategoryGuidesUseCase: GetDisposalCategoryGuidesUseCase,
    observeFavoritesUseCase: ObserveFavoritesUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ItemSearchUiState())
    val uiState: StateFlow<ItemSearchUiState> = _uiState.asStateFlow()
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
    }

    fun onQueryChange(query: String) {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                query = query,
                guides = emptyList(),
                pendingGuideToOpen = null,
                isLoading = false,
                hasSearched = false,
                errorMessageResId = null,
            )
        }
    }

    fun clearPendingGuideToOpen() {
        _uiState.update { it.copy(pendingGuideToOpen = null) }
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
                    pendingGuideToOpen = null,
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
                        pendingGuideToOpen = null,
                        hasSearched = true,
                        errorMessageResId = null,
                    )
                }

                runCatchingCancellable { searchDisposalItemGuidesUseCase(trimmedQuery) }
                    .onSuccess { guides ->
                        _uiState.update {
                            it.copy(
                                guides = guides,
                                isLoading = false,
                            )
                        }
                    }
                    .onFailure {
                        _uiState.update {
                            it.copy(
                                guides = emptyList(),
                                pendingGuideToOpen = null,
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
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        pendingGuideToOpen = null,
                        errorMessageResId = null,
                    )
                }

                runCatchingCancellable { getDisposalCategoryGuidesUseCase(category.disposalCategory) }
                    .onSuccess { guides ->
                        val representativeGuide =
                            guides.firstOrNull { it.name == category.representativeGuideName }
                                ?: guides.firstOrNull()

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                pendingGuideToOpen = representativeGuide,
                            )
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

    private suspend fun <T> runCatchingCancellable(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            Result.failure(exception)
        }
}
