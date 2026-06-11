package com.team.yeogibeoryeo.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveCollectionSpotFavoriteSnapshotsUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoritesUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.RemoveCollectionSpotFavoriteUseCase
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
        observeCollectionSpotFavoriteSnapshotsUseCase: ObserveCollectionSpotFavoriteSnapshotsUseCase,
        private val removeCollectionSpotFavoriteUseCase: RemoveCollectionSpotFavoriteUseCase,
        private val itemGuideUiMapper: FavoriteItemGuideUiMapper,
        private val collectionSpotUiMapper: FavoriteCollectionSpotUiMapper,
        private val regionalGuideUiMapper: FavoriteRegionalGuideUiMapper,
    ) : ViewModel() {
        private val selectedTab = MutableStateFlow(FavoriteTab.ITEM_GUIDE)

        val uiState: StateFlow<FavoritesUiState> =
            combine(
                selectedTab,
                observeFavoritesUseCase(),
                observeCollectionSpotFavoriteSnapshotsUseCase(),
            ) { selectedTab, favorites, collectionSpotSnapshots ->
                val itemGuideFavorites =
                    favorites
                        .filter { it.type == FavoriteTargetType.ITEM_GUIDE }
                        .mapNotNull { favorite -> itemGuideUiMapper.map(favorite) }
                val collectionSpotFavoriteIds =
                    favorites
                        .filter { it.type == FavoriteTargetType.COLLECTION_SPOT }
                val collectionSpotSnapshotsById =
                    collectionSpotSnapshots.associateBy { snapshot -> snapshot.targetId }
                val collectionSpotFavorites =
                    collectionSpotFavoriteIds
                        .mapNotNull { favorite ->
                            collectionSpotSnapshotsById[favorite.targetId]
                                ?.let { snapshot -> collectionSpotUiMapper.map(snapshot) }
                        }
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

        fun removeCollectionSpotFavorite(targetId: String) {
            viewModelScope.launch {
                removeCollectionSpotFavoriteUseCase(targetId)
            }
        }
    }
