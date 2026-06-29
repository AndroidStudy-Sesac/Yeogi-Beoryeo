package com.team.yeogibeoryeo.presentation.search

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoriteUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ToggleFavoriteUseCase
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.usecase.GetDisposalItemGuideUseCase
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.search.model.ItemGuideDetailAction
import com.team.yeogibeoryeo.presentation.search.model.toDetailActions
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
class ItemGuideDetailViewModel
@Inject
constructor(
    private val getDisposalItemGuideUseCase: GetDisposalItemGuideUseCase,
    private val observeFavoriteUseCase: ObserveFavoriteUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ItemGuideDetailUiState>(ItemGuideDetailUiState.Loading)
    val uiState: StateFlow<ItemGuideDetailUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<ItemGuideDetailEvent>()
    val events: SharedFlow<ItemGuideDetailEvent> = _events.asSharedFlow()
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
                        _uiState.value = ItemGuideDetailUiState.Success(
                            guide = guide,
                            actions = guide.toDetailActions(),
                        )
                        observeFavorite(guide)
                    } else {
                        _uiState.value =
                            ItemGuideDetailUiState.Error(
                                R.string.item_guide_detail_select_again_message,
                            )
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Throwable) {
                    _uiState.value =
                        ItemGuideDetailUiState.Error(
                            R.string.item_guide_detail_load_failed_message,
                        )
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
            val messageResId =
                if (isFavorite) {
                    R.string.item_guide_detail_favorite_added_message
                } else {
                    R.string.item_guide_detail_favorite_removed_message
                }
            val latestState = uiState.value
            if (latestState is ItemGuideDetailUiState.Success && latestState.guide.id == guide.id) {
                _events.emit(
                    ItemGuideDetailEvent.ShowMessage(
                        messageResId = messageResId,
                        icon = ItemGuideDetailMessageIcon.Favorite,
                    ),
                )
            }
        }
    }

    fun showOfficialGuideOpenFailedMessage() {
        viewModelScope.launch {
            _events.emit(
                ItemGuideDetailEvent.ShowMessage(
                    messageResId = R.string.item_guide_action_open_failed_message,
                    icon = ItemGuideDetailMessageIcon.Warning,
                ),
            )
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
        val actions: List<ItemGuideDetailAction> = emptyList(),
    ) : ItemGuideDetailUiState

    data class Error(
        @param:StringRes val messageResId: Int,
    ) : ItemGuideDetailUiState
}

sealed interface ItemGuideDetailEvent {
    data class ShowMessage(
        @param:StringRes val messageResId: Int,
        val icon: ItemGuideDetailMessageIcon,
    ) : ItemGuideDetailEvent
}

enum class ItemGuideDetailMessageIcon {
    Favorite,
    Warning,
}
