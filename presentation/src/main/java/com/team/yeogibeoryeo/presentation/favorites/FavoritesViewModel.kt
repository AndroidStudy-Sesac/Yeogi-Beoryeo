package com.team.yeogibeoryeo.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoritesUseCase
import com.team.yeogibeoryeo.domain.item.usecase.GetDisposalItemGuideUseCase
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteItemUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class FavoritesViewModel
    @Inject
    constructor(
        observeFavoritesUseCase: ObserveFavoritesUseCase,
        private val getDisposalItemGuideUseCase: GetDisposalItemGuideUseCase,
    ) : ViewModel() {
        val uiState: StateFlow<FavoritesUiState> =
            observeFavoritesUseCase(FavoriteTargetType.ITEM_GUIDE)
                .map { favorites ->
                    FavoritesUiState(
                        favorites =
                            favorites.mapNotNull { favorite ->
                                val guide = getDisposalItemGuideUseCase(favorite.targetId)
                                guide?.let {
                                    FavoriteItemUiModel(
                                        targetId = favorite.targetId,
                                        title = it.name,
                                        subtitle = it.subCategory?.displayName ?: it.category.displayName,
                                    )
                                }
                            },
                    )
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = FavoritesUiState(isLoading = true),
                )
    }
