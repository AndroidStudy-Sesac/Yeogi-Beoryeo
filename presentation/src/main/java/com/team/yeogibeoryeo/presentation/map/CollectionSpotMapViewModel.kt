package com.team.yeogibeoryeo.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.usecase.FilterCollectionSpotsUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.SearchCollectionSpotsByKeywordUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.SearchCollectionSpotsByLocationUseCase
import com.team.yeogibeoryeo.presentation.map.location.CurrentLocationProvider
import com.team.yeogibeoryeo.presentation.map.location.CurrentLocationResult
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
    private val searchCollectionSpotsByLocationUseCase: SearchCollectionSpotsByLocationUseCase,
    private val filterCollectionSpotsUseCase: FilterCollectionSpotsUseCase,
    private val currentLocationProvider: CurrentLocationProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionSpotMapUiState())
    val uiState: StateFlow<CollectionSpotMapUiState> = _uiState.asStateFlow()

    private var originalSpots: List<CollectionSpot> = emptyList()

    fun onSearchKeywordChanged(keyword: String) {
        _uiState.update {
            it.copy(
                searchKeyword = keyword,
                errorMessage = null,
                locationNoticeMessage = null,
            )
        }
    }

    fun searchByKeyword() {
        val keyword = uiState.value.searchKeyword.trim()

        if (keyword.isBlank()) {
            originalSpots = emptyList()

            _uiState.update {
                it.copy(
                    spots = emptyList(),
                    selectedSpot = null,
                    isLoading = false,
                    hasSearched = false,
                    errorMessage = "검색어를 입력해주세요.",
                    locationNoticeMessage = null,
                    searchMode = MapSearchMode.KEYWORD,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    hasSearched = true,
                    errorMessage = null,
                    locationNoticeMessage = null,
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
                updateSpotResult(spots)
            }.onFailure { throwable ->
                updateSpotFailure(
                    message = throwable.message ?: "수거 장소를 불러오지 못했습니다.",
                )
            }
        }
    }

    fun searchByCurrentLocation() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    hasSearched = true,
                    errorMessage = null,
                    locationNoticeMessage = null,
                    selectedSpot = null,
                    searchMode = MapSearchMode.CURRENT_LOCATION,
                )
            }


            when (val result = currentLocationProvider.getCurrentLocation()) {
                is CurrentLocationResult.Found -> {
                    searchByLocation(result.coordinate)
                }

                CurrentLocationResult.NotFound -> {
                    onCurrentLocationNotFound()
                }

                CurrentLocationResult.PermissionDenied -> {
                    onLocationPermissionDenied()
                }
            }
        }
    }

    private suspend fun searchByLocation(
        coordinate: Coordinate,
    ) {
        runCatching {
            searchCollectionSpotsByLocationUseCase(
                coordinate = coordinate,
                radiusMeter = DEFAULT_RADIUS_METER,
                types = emptySet(),
            )
        }.onSuccess { spots ->
            updateSpotResult(spots)
        }.onFailure { throwable ->
            updateSpotFailure(
                message = throwable.message ?: "현재 위치 주변 수거 장소를 불러오지 못했습니다.",
            )
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

    fun onLocationPermissionDenied() {
        _uiState.update {
            it.copy(
                isLoading = false,
                hasSearched = false,
                errorMessage = null,
                locationNoticeMessage = "현재 위치 검색은 정확한 위치 권한을 허용하면 사용할 수 있어요. 직접 동네나 주소를 검색할 수도 있습니다.",
                searchMode = MapSearchMode.KEYWORD,
            )
        }
    }

    fun onCurrentLocationNotFound() {
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = null,
                locationNoticeMessage = "현재 위치를 확인하지 못했습니다. 직접 동네나 주소를 검색해 주세요.",
                searchMode = MapSearchMode.KEYWORD,
            )
        }
    }

    fun clearErrorMessage() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }

    private fun updateSpotResult(spots: List<CollectionSpot>) {
        originalSpots = spots

        val filteredSpots = filterCollectionSpotsUseCase(
            spots = originalSpots,
            selectedTypes = uiState.value.selectedTypes,
        )

        _uiState.update {
            it.copy(
                spots = filteredSpots,
                selectedSpot = null,
                isLoading = false,
                hasSearched = true,
                errorMessage = null,
                locationNoticeMessage = null,
            )
        }
    }

    private fun updateSpotFailure(message: String) {
        originalSpots = emptyList()

        _uiState.update {
            it.copy(
                spots = emptyList(),
                selectedSpot = null,
                isLoading = false,
                hasSearched = true,
                errorMessage = message,
                locationNoticeMessage = null,
            )
        }
    }

    private companion object {
        const val DEFAULT_RADIUS_METER = 500
    }
}
