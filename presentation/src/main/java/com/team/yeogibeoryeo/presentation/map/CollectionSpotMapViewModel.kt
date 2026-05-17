package com.team.yeogibeoryeo.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.usecase.FilterCollectionSpotsUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.SearchCollectionSpotsByKeywordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CollectionSpotMapViewModel @Inject constructor(
    private val searchCollectionSpotsByKeywordUseCase: SearchCollectionSpotsByKeywordUseCase,
    private val filterCollectionSpotsUseCase: FilterCollectionSpotsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionSpotMapUiState())
    val uiState: StateFlow<CollectionSpotMapUiState> = _uiState.asStateFlow()

    private var originalSpots: List<CollectionSpot> = emptyList()

    fun onSearchKeywordChanged(keyword: String) {
        _uiState.update {
            it.copy(searchKeyword = keyword)
        }
    }

    fun searchByKeyword() {
        val keyword = uiState.value.searchKeyword.trim()

        if (keyword.isBlank()) {
            _uiState.update {
                it.copy(
                    spots = emptyList(),
                    selectedSpot = null,
                    errorMessage = "검색어를 입력해주세요.",
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    selectedSpot = null,
                    searchMode = MapSearchMode.KEYWORD,
                )
            }

            runCatching {
                searchCollectionSpotsByKeywordUseCase(
                    keyword = keyword,
                    types = emptySet(),
                )
            }.onSuccess { spots ->
                originalSpots = spots

                val filteredSpots = filterCollectionSpotsUseCase(
                    spots = originalSpots,
                    selectedTypes = uiState.value.selectedTypes,
                )

                _uiState.update {
                    it.copy(
                        spots = filteredSpots,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                originalSpots = emptyList()

                _uiState.update {
                    it.copy(
                        spots = emptyList(),
                        isLoading = false,
                        selectedSpot = null,
                        errorMessage = throwable.message ?: "수거 장소를 불러오지 못했습니다.",
                    )
                }
            }
        }
    }

    fun onSpotTypeClick(type: CollectionSpotType) {
        val currentTypes = uiState.value.selectedTypes

        val updatedTypes = if (type in currentTypes) {
            currentTypes - type
        } else {
            currentTypes + type
        }

        val filteredSpots = filterCollectionSpotsUseCase(
            spots = originalSpots,
            selectedTypes = updatedTypes,
        )

        _uiState.update {
            it.copy(
                selectedTypes = updatedTypes,
                spots = filteredSpots,
                selectedSpot = null,
            )
        }
    }

    fun onSpotClick(spot: CollectionSpot) {
        _uiState.update {
            it.copy(selectedSpot = spot)
        }
    }

    fun clearErrorMessage() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }
}