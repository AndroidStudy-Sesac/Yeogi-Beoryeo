package com.team.yeogibeoryeo.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoriteUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ToggleFavoriteUseCase
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.usecase.GetDisposalItemGuideUseCase
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
class ItemGuideDetailViewModel
@Inject
constructor(
    private val getDisposalItemGuideUseCase: GetDisposalItemGuideUseCase,
    private val observeFavoriteUseCase: ObserveFavoriteUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ItemGuideDetailUiState>(ItemGuideDetailUiState.Loading)
    val uiState: StateFlow<ItemGuideDetailUiState> = _uiState.asStateFlow()
    private var loadGuideJob: Job? = null
    private var favoriteJob: Job? = null

    fun loadGuide(guideId: String) {
        loadGuideJob?.cancel()
        loadGuideJob =
            viewModelScope.launch {
                favoriteJob?.cancel()
                _uiState.value = ItemGuideDetailUiState.Loading

                try {
                    val guide = getDisposalItemGuideUseCase(guideId)

                    if (guide != null) {
                        _uiState.value = ItemGuideDetailUiState.Success(guide = guide)
                        observeFavorite(guide)
                    } else {
                        _uiState.value = ItemGuideDetailUiState.Error("다시 검색해서 품목을 선택해주세요.")
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Throwable) {
                    _uiState.value = ItemGuideDetailUiState.Error("품목 정보를 불러오는 중 오류가 발생했습니다.")
                }
            }
    }

    fun toggleFavorite() {
        val currentState = _uiState.value as? ItemGuideDetailUiState.Success ?: return
        val guide = currentState.guide

        viewModelScope.launch {
            val isFavorite =
                toggleFavoriteUseCase(
                    Favorite(
                        type = FavoriteTargetType.ITEM_GUIDE,
                        targetId = guide.id,
                        savedAtMillis = System.currentTimeMillis(),
                    ),
                )
            _uiState.update { state ->
                if (state is ItemGuideDetailUiState.Success && state.guide.id == guide.id) {
                    state.copy(
                        favoriteMessage =
                            if (isFavorite) {
                                "즐겨찾기에 추가되었습니다"
                            } else {
                                "즐겨찾기에서 제외되었습니다"
                            },
                    )
                } else {
                    state
                }
            }
        }
    }

    fun clearFavoriteMessage() {
        _uiState.update { state ->
            if (state is ItemGuideDetailUiState.Success) {
                state.copy(favoriteMessage = null)
            } else {
                state
            }
        }
    }

    private fun observeFavorite(guide: DisposalItemGuide) {
        favoriteJob =
            viewModelScope.launch {
                observeFavoriteUseCase(FavoriteTargetType.ITEM_GUIDE, guide.id)
                    .collect { isFavorite ->
                        _uiState.update { state ->
                            if (state is ItemGuideDetailUiState.Success && state.guide.id == guide.id) {
                                state.copy(isFavorite = isFavorite)
                            } else {
                                state
                            }
                        }
                    }
            }
    }
}

sealed interface ItemGuideDetailUiState {
    data object Loading : ItemGuideDetailUiState

    data class Success(
        val guide: DisposalItemGuide,
        val isFavorite: Boolean = false,
        val favoriteMessage: String? = null,
    ) : ItemGuideDetailUiState

    data class Error(
        val message: String,
    ) : ItemGuideDetailUiState
}
