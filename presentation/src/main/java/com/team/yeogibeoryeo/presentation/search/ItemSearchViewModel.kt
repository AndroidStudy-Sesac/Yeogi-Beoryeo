package com.team.yeogibeoryeo.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {
    private val _uiState = MutableStateFlow(ItemSearchUiState())
    val uiState: StateFlow<ItemSearchUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                query = query,
                guides = emptyList(),
                selectedGuide = null,
                isLoading = false,
                hasSearched = false,
                errorMessageResId = null,
            )
        }
    }

    fun selectGuide(guide: DisposalItemGuide) {
        _uiState.update { it.copy(selectedGuide = guide) }
    }

    fun clearSelectedGuide() {
        _uiState.update { it.copy(selectedGuide = null) }
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
        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    guides = emptyList(),
                    selectedGuide = null,
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
                        selectedGuide = null,
                        hasSearched = true,
                        errorMessageResId = null,
                    )
                }

                runCatchingCancellable { searchDisposalItemGuidesUseCase(query) }
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
                                selectedGuide = null,
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
                        selectedGuide = null,
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
                                selectedGuide = representativeGuide,
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
