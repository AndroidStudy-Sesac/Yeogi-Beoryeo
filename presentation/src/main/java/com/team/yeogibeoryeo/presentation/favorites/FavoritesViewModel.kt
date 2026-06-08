package com.team.yeogibeoryeo.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoritesUseCase
import com.team.yeogibeoryeo.presentation.favorites.mapper.FavoriteCollectionSpotUiMapper
import com.team.yeogibeoryeo.presentation.favorites.mapper.FavoriteItemGuideUiMapper
import com.team.yeogibeoryeo.presentation.favorites.mapper.FavoriteRegionalGuideUiMapper
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteTab
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class FavoritesViewModel
    @Inject
    constructor(
        observeFavoritesUseCase: ObserveFavoritesUseCase,
        private val itemGuideUiMapper: FavoriteItemGuideUiMapper,
        private val collectionSpotUiMapper: FavoriteCollectionSpotUiMapper,
        private val regionalGuideUiMapper: FavoriteRegionalGuideUiMapper,
    ) : ViewModel() {
        private val selectedTab = MutableStateFlow(FavoriteTab.ITEM_GUIDE)

        val uiState: StateFlow<FavoritesUiState> =
            combine(
                selectedTab,
                observeFavoritesUseCase(),
            ) { selectedTab, favorites ->
                val itemGuideFavorites =
                    favorites
                        .filter { it.type == FavoriteTargetType.ITEM_GUIDE }
                        .mapNotNull { favorite -> itemGuideUiMapper.map(favorite) }
                val collectionSpotFavorites =
                    favorites
                        .filter { it.type == FavoriteTargetType.COLLECTION_SPOT }
                        .mapNotNull { favorite -> collectionSpotUiMapper.map(favorite) }
                val regionalGuideFavorites =
                    favorites
                        .filter { it.type == FavoriteTargetType.REGIONAL_GUIDE }
                        .mapNotNull { favorite -> regionalGuideUiMapper.map(favorite) }

                    FavoritesUiState(
                        selectedTab = selectedTab,
                        itemGuideFavorites = itemGuideFavorites,
                        collectionSpotFavorites = collectionSpotFavorites,
                        regionalGuideFavorites = regionalGuideFavorites,
                    )
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = FavoritesUiState(isLoading = true),
                )

        fun selectTab(tab: FavoriteTab) {
            selectedTab.value = tab
        }
    }
