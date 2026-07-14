package com.team.yeogibeoryeo.presentation.favorites

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveCollectionSpotFavoritesUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoritesUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveRegionalGuideFavoriteSnapshotsUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.RemoveCollectionSpotFavoriteUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.RemoveFavoriteUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.RemoveRegionalGuideFavoriteUseCase
import com.team.yeogibeoryeo.presentation.favorites.mapper.FavoriteCollectionSpotUiMapper
import com.team.yeogibeoryeo.presentation.favorites.mapper.FavoriteItemGuideUiMapper
import com.team.yeogibeoryeo.presentation.favorites.mapper.FavoriteRegionalGuideUiMapper
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteTab
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FavoritesViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        observeFavoritesUseCase: ObserveFavoritesUseCase,
        observeCollectionSpotFavoritesUseCase: ObserveCollectionSpotFavoritesUseCase,
        observeRegionalGuideFavoriteSnapshotsUseCase: ObserveRegionalGuideFavoriteSnapshotsUseCase,
        private val removeFavoriteUseCase: RemoveFavoriteUseCase,
        private val removeCollectionSpotFavoriteUseCase: RemoveCollectionSpotFavoriteUseCase,
        private val removeRegionalGuideFavoriteUseCase: RemoveRegionalGuideFavoriteUseCase,
        private val itemGuideUiMapper: FavoriteItemGuideUiMapper,
        private val collectionSpotUiMapper: FavoriteCollectionSpotUiMapper,
        private val regionalGuideUiMapper: FavoriteRegionalGuideUiMapper,
    ) : ViewModel() {
        private val _events = MutableSharedFlow<FavoritesEvent>()
        val events: SharedFlow<FavoritesEvent> = _events.asSharedFlow()
        private val favoriteRemovalJobs = mutableMapOf<Pair<FavoriteTargetType, String>, Job>()
        private val selectedTab =
            savedStateHandle.getStateFlow(SELECTED_TAB_KEY, FavoriteTab.ITEM_GUIDE)

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
            savedStateHandle[SELECTED_TAB_KEY] = tab
        }

        fun removeItemGuideFavorite(targetId: String) {
            removeFavorite(FavoriteTargetType.ITEM_GUIDE, targetId) {
                removeFavoriteUseCase(FavoriteTargetType.ITEM_GUIDE, targetId)
            }
        }

        fun removeCollectionSpotFavorite(targetId: String) {
            removeFavorite(FavoriteTargetType.COLLECTION_SPOT, targetId) {
                removeCollectionSpotFavoriteUseCase(targetId)
            }
        }

        fun removeRegionalGuideFavorite(targetId: String) {
            removeFavorite(FavoriteTargetType.REGIONAL_GUIDE, targetId) {
                removeRegionalGuideFavoriteUseCase(targetId)
            }
        }

        private fun removeFavorite(
            type: FavoriteTargetType,
            targetId: String,
            remove: suspend () -> Unit,
        ) {
            val key = type to targetId
            if (favoriteRemovalJobs[key]?.isActive == true) return

            favoriteRemovalJobs[key] = viewModelScope.launch {
                try {
                    remove()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Throwable) {
                    _events.emit(FavoritesEvent.FavoriteUpdateFailed)
                } finally {
                    favoriteRemovalJobs.remove(key)
                }
            }
        }

        private companion object {
            const val SELECTED_TAB_KEY = "selected_tab"
        }
    }

sealed interface FavoritesEvent {
    data object FavoriteUpdateFailed : FavoritesEvent
}
