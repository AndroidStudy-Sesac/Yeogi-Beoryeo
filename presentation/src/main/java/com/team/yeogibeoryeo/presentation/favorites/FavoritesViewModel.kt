package com.team.yeogibeoryeo.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveCollectionSpotFavoritesUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoritesUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveRegionalGuideFavoriteSnapshotsUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.RemoveCollectionSpotFavoriteUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.RemoveRegionalGuideFavoriteUseCase
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
import kotlinx.coroutines.launch

@HiltViewModel
class FavoritesViewModel
    @Inject
    constructor(
        observeFavoritesUseCase: ObserveFavoritesUseCase,
        observeCollectionSpotFavoritesUseCase: ObserveCollectionSpotFavoritesUseCase,
        observeRegionalGuideFavoriteSnapshotsUseCase: ObserveRegionalGuideFavoriteSnapshotsUseCase,
        private val removeCollectionSpotFavoriteUseCase: RemoveCollectionSpotFavoriteUseCase,
        private val removeRegionalGuideFavoriteUseCase: RemoveRegionalGuideFavoriteUseCase,
        private val itemGuideUiMapper: FavoriteItemGuideUiMapper,
        private val collectionSpotUiMapper: FavoriteCollectionSpotUiMapper,
        private val regionalGuideUiMapper: FavoriteRegionalGuideUiMapper,
    ) : ViewModel() {
        private val selectedTab = MutableStateFlow(FavoriteTab.ITEM_GUIDE)

        val uiState: StateFlow<FavoritesUiState> =
            combine(
                selectedTab,
                observeFavoritesUseCase(),
                observeCollectionSpotFavoritesUseCase(),
                observeRegionalGuideFavoriteSnapshotsUseCase(),
            ) { selectedTab, favorites, collectionSpotFavorites, regionalGuideSnapshots ->
                val itemGuideFavorites =
                    favorites
                        .filter { it.type == FavoriteTargetType.ITEM_GUIDE }
                        .mapNotNull { favorite -> itemGuideUiMapper.map(favorite) }
                val collectionSpotFavoriteUiModels =
                    collectionSpotFavorites.map { favorite -> collectionSpotUiMapper.map(favorite) }
                val regionalGuideSnapshotsById =
                    regionalGuideSnapshots.associateBy { snapshot -> snapshot.targetId }
                val regionalGuideFavorites =
                    favorites
                        .filter { it.type == FavoriteTargetType.REGIONAL_GUIDE }
                        .mapNotNull { favorite ->
                            regionalGuideSnapshotsById[favorite.targetId]
                                ?.let { snapshot -> regionalGuideUiMapper.map(snapshot) }
                        }

                    FavoritesUiState(
                        selectedTab = selectedTab,
                        itemGuideFavorites = itemGuideFavorites,
                        collectionSpotFavorites = collectionSpotFavoriteUiModels,
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

        fun removeCollectionSpotFavorite(targetId: String) {
            viewModelScope.launch {
                removeCollectionSpotFavoriteUseCase(targetId)
            }
        }

        fun removeRegionalGuideFavorite(targetId: String) {
            viewModelScope.launch {
                removeRegionalGuideFavoriteUseCase(targetId)
            }
        }
    }
