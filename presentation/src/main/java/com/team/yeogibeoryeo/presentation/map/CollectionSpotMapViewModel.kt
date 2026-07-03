package com.team.yeogibeoryeo.presentation.map

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoritesUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ToggleCollectionSpotFavoriteUseCase
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.usecase.CalculateDistanceMeterUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.ClearRecentCurrentLocationSpotsUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.FilterCollectionSpotsUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.GetFreshRecentCurrentLocationSpotsUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.SaveRecentCurrentLocationSpotsUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.SearchCollectionSpotsByKeywordUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.SearchCollectionSpotsByLocationUseCase
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.map.location.CurrentLocationProvider
import com.team.yeogibeoryeo.presentation.map.location.CurrentLocationResult
import com.team.yeogibeoryeo.presentation.map.location.LocationPermissionChecker
import com.team.yeogibeoryeo.presentation.map.model.FavoriteSpotMapMoveRequest
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
class CollectionSpotMapViewModel @Inject constructor(
    private val searchCollectionSpotsByKeywordUseCase: SearchCollectionSpotsByKeywordUseCase,
    private val searchCollectionSpotsByLocationUseCase: SearchCollectionSpotsByLocationUseCase,
    private val filterCollectionSpotsUseCase: FilterCollectionSpotsUseCase,
    private val currentLocationProvider: CurrentLocationProvider,
    private val locationPermissionChecker: LocationPermissionChecker,
    private val getFreshRecentCurrentLocationSpotsUseCase: GetFreshRecentCurrentLocationSpotsUseCase,
    private val saveRecentCurrentLocationSpotsUseCase: SaveRecentCurrentLocationSpotsUseCase,
    private val clearRecentCurrentLocationSpotsUseCase: ClearRecentCurrentLocationSpotsUseCase,
    private val observeFavoritesUseCase: ObserveFavoritesUseCase,
    private val toggleCollectionSpotFavoriteUseCase: ToggleCollectionSpotFavoriteUseCase,
    private val calculateDistanceMeterUseCase: CalculateDistanceMeterUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionSpotMapUiState())
    val uiState: StateFlow<CollectionSpotMapUiState> = _uiState.asStateFlow()

    private var originalSpots: List<CollectionSpot> = emptyList()
    private var spotSearchJob: Job? = null
    private var currentLocationRefreshJob: Job? = null
    private var hasRequestedInitialCurrentLocationSearch = false
    private var favoriteSpotIds: Set<String> = emptySet()
    private val consumedFavoriteSpotMoveRequestIds = mutableSetOf<String>()

    init {
        observeCollectionSpotFavorites()
    }

    fun onSearchKeywordChanged(keyword: String) {
        currentLocationRefreshJob?.cancel()

        val shouldCancelSpotSearch =
            uiState.value.isLoading &&
                uiState.value.searchMode in setOf(
                    MapSearchMode.KEYWORD,
                    MapSearchMode.CURRENT_LOCATION,
                    MapSearchMode.MAP_CENTER,
                )
        val shouldCancelFavoriteSpotNearbySearch = uiState.value.isFavoriteSpotNearbyLoading

        if (shouldCancelSpotSearch || shouldCancelFavoriteSpotNearbySearch) {
            spotSearchJob?.cancel()
        }
        if (shouldCancelSpotSearch) {
            originalSpots = emptyList()
        }

        _uiState.update {
            it.copy(
                searchKeyword = keyword,
                spots = if (shouldCancelSpotSearch) {
                    emptyList()
                } else {
                    it.spots
                },
                selectedSpot = if (shouldCancelSpotSearch) {
                    null
                } else {
                    it.selectedSpot
                },
                isLoading = if (shouldCancelSpotSearch) {
                    false
                } else {
                    it.isLoading
                },
                hasSearched = if (shouldCancelSpotSearch) {
                    false
                } else {
                    it.hasSearched
                },
                errorMessageResId = null,
                locationNotice = null,
                isFavoriteSpotNearbyLoading = false,
                searchMode = if (shouldCancelSpotSearch) {
                    MapSearchMode.KEYWORD
                } else {
                    it.searchMode
                },
            )
        }
    }

    fun searchByKeyword() {
        val keyword = uiState.value.searchKeyword.trim()
        currentLocationRefreshJob?.cancel()

        if (keyword.isBlank()) {
            spotSearchJob?.cancel()
            originalSpots = emptyList()

            _uiState.update {
                it.copy(
                    spots = emptyList(),
                    selectedSpot = null,
                    isLoading = false,
                    hasSearched = false,
                    errorMessageResId = R.string.map_search_blank_keyword_message,
                    locationNotice = null,
                    isFavoriteSpotNearbyLoading = false,
                    searchMode = MapSearchMode.KEYWORD,
                )
            }
            return
        }

        spotSearchJob?.cancel()
        spotSearchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    hasSearched = true,
                    errorMessageResId = null,
                    locationNotice = null,
                    selectedSpot = null,
                    isFavoriteSpotNearbyLoading = false,
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
                if (throwable is CancellationException) throw throwable

                updateSpotFailure(
                    messageResId = MapLocationNotices.SpotSearchFailureMessageResId,
                )
            }
        }
    }

    fun searchByCurrentLocation() {
        currentLocationRefreshJob?.cancel()
        spotSearchJob?.cancel()
        spotSearchJob = viewModelScope.launch {
            if (!locationPermissionChecker.hasFineLocationPermission()) {
                handleLocationPermissionDenied()
                return@launch
            }

            val cachedEntry = getFreshRecentCurrentLocationSpotsUseCase()

            if (cachedEntry != null) {
                showCachedCurrentLocationSpots(cachedEntry.spots)
                refreshCurrentLocationSilently()
                return@launch
            }

            searchByCurrentLocationInternal(
                showLoading = true,
                preservePreviousResultOnFailure = false,
            )
        }
    }

    fun searchByMapCenter(coordinate: Coordinate) {
        currentLocationRefreshJob?.cancel()
        spotSearchJob?.cancel()
        spotSearchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    hasSearched = true,
                    errorMessageResId = null,
                    locationNotice = null,
                    selectedSpot = null,
                    isFavoriteSpotNearbyLoading = false,
                    searchMode = MapSearchMode.MAP_CENTER,
                )
            }

            runCatching {
                searchCollectionSpotsByLocationUseCase(
                    coordinate = coordinate,
                    radiusMeter = DEFAULT_RADIUS_METER,
                    types = emptySet(),
                )
            }.onSuccess { spots ->
                updateSpotResult(spots)
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable

                updateSpotFailure(
                    messageResId = MapLocationNotices.SpotSearchFailureMessageResId,
                )
            }
        }
    }

    private suspend fun searchByLocation(
        coordinate: Coordinate,
        preservePreviousResultOnFailure: Boolean,
    ) {
        try {
            val spots = searchCollectionSpotsByLocationUseCase(
                coordinate = coordinate,
                radiusMeter = DEFAULT_RADIUS_METER,
                types = emptySet(),
            )

            if (!canApplyCurrentLocationResult()) return

            val spotsWithDistance = spots.withDistanceFrom(coordinate)

            updateSpotResult(spotsWithDistance)
            saveRecentCurrentLocationSpotsUseCase(spotsWithDistance)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable

            if (!preservePreviousResultOnFailure) {
                updateSpotFailure(
                    messageResId = MapLocationNotices.CurrentLocationSpotSearchFailureMessageResId,
                )
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

    fun clearSelectedSpot() {
        _uiState.update {
            it.copy(selectedSpot = null)
        }
    }

    fun showFavoriteSpot(request: FavoriteSpotMapMoveRequest) {
        if (!consumedFavoriteSpotMoveRequestIds.add(request.requestId)) return

        currentLocationRefreshJob?.cancel()
        spotSearchJob?.cancel()

        val selectedSpot =
            originalSpots.firstOrNull { spot -> spot.id == request.targetId }
                ?: uiState.value.spots.firstOrNull { spot -> spot.id == request.targetId }
                ?: request.toCollectionSpot()

        _uiState.update {
            it.copy(
                selectedSpot = selectedSpot.copy(
                    isBookmarked = request.targetId in favoriteSpotIds,
                ),
                isLoading = false,
                errorMessageResId = null,
                locationNotice = null,
                favoriteSpotMoveRequestId = request.targetId,
                favoriteSpotMoveRequestSequence = it.favoriteSpotMoveRequestSequence + 1,
                isFavoriteSpotNearbyLoading = true,
            )
        }

        searchNearbySpotsForFavoriteSpot(request)
    }

    fun onSpotFavoriteClick(spot: CollectionSpot) {
        viewModelScope.launch {
            toggleCollectionSpotFavoriteUseCase(spot)
        }
    }

    fun onLocationPermissionDenied() {
        viewModelScope.launch {
            handleLocationPermissionDenied()
        }
    }

    fun onLocationPermissionRevoked() {
        viewModelScope.launch {
            clearRecentCurrentLocationCache()

            if (uiState.value.searchMode == MapSearchMode.CURRENT_LOCATION) {
                showLocationPermissionDeniedNotice()
            }
        }
    }

    private suspend fun handleLocationPermissionDenied() {
        clearRecentCurrentLocationCache()
        showLocationPermissionDeniedNotice()
    }

    private suspend fun clearRecentCurrentLocationCache() {
        clearRecentCurrentLocationSpotsUseCase()
    }

    private fun showLocationPermissionDeniedNotice() {
        originalSpots = emptyList()

        _uiState.update {
            it.copy(
                spots = emptyList(),
                selectedSpot = null,
                isLoading = false,
                hasSearched = false,
                errorMessageResId = null,
                locationNotice = MapLocationNotices.PermissionDenied,
                isFavoriteSpotNearbyLoading = false,
                searchMode = MapSearchMode.KEYWORD,
            )
        }
    }

    fun searchByCurrentLocationOnMapEntryIfPermitted(initialSpotType: CollectionSpotType? = null) {
        applyInitialSpotType(initialSpotType)

        if (
            hasRequestedInitialCurrentLocationSearch ||
            uiState.value.hasSearched ||
            uiState.value.isLoading ||
            uiState.value.searchKeyword.isNotBlank()
        ) {
            return
        }

        hasRequestedInitialCurrentLocationSearch = true

        val currentState = uiState.value
        if (
            currentState.hasSearched ||
            currentState.isLoading ||
            currentState.searchKeyword.isNotBlank() ||
            !locationPermissionChecker.hasFineLocationPermission()
        ) {
            return
        }

        spotSearchJob?.cancel()
        spotSearchJob = viewModelScope.launch {
            val cachedEntry = getFreshRecentCurrentLocationSpotsUseCase()

            if (cachedEntry != null && canStartInitialCurrentLocationSearch()) {
                showCachedCurrentLocationSpots(cachedEntry.spots)
                refreshCurrentLocationSilently()
                return@launch
            }

            if (canStartInitialCurrentLocationSearch()) {
                searchByCurrentLocationInternal(
                    showLoading = true,
                    preservePreviousResultOnFailure = false,
                )
            }
        }
    }

    private fun applyInitialSpotType(type: CollectionSpotType?) {
        if (type == null) return

        val selectedTypes = setOf(type)
        val filteredSpots = filterCollectionSpotsUseCase(
            spots = originalSpots,
            selectedTypes = selectedTypes,
        )

        _uiState.update {
            it.copy(
                selectedTypes = selectedTypes,
                spots = filteredSpots,
                selectedSpot = null,
            )
        }
    }

    fun onCurrentLocationNotFound() {
        originalSpots = emptyList()

        _uiState.update {
            it.copy(
                spots = emptyList(),
                selectedSpot = null,
                isLoading = false,
                hasSearched = false,
                errorMessageResId = null,
                locationNotice = MapLocationNotices.CurrentLocationUnavailable,
                isFavoriteSpotNearbyLoading = false,
                searchMode = MapSearchMode.KEYWORD,
            )
        }
    }

    fun onLocationServiceDisabled() {
        originalSpots = emptyList()

        _uiState.update {
            it.copy(
                spots = emptyList(),
                selectedSpot = null,
                isLoading = false,
                hasSearched = false,
                errorMessageResId = null,
                locationNotice = MapLocationNotices.LocationServiceDisabled,
                isFavoriteSpotNearbyLoading = false,
                searchMode = MapSearchMode.KEYWORD,
            )
        }
    }

    fun clearErrorMessage() {
        _uiState.update {
            it.copy(errorMessageResId = null)
        }
    }

    private fun updateSpotResult(spots: List<CollectionSpot>) {
        originalSpots = spots.withFavoriteState()

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
                errorMessageResId = null,
                locationNotice = null,
                isFavoriteSpotNearbyLoading = false,
            )
        }
    }

    private fun searchNearbySpotsForFavoriteSpot(request: FavoriteSpotMapMoveRequest) {
        spotSearchJob = viewModelScope.launch {
            runCatching {
                searchCollectionSpotsByLocationUseCase(
                    coordinate = request.coordinate,
                    radiusMeter = DEFAULT_RADIUS_METER,
                    types = emptySet(),
                )
            }.onSuccess { spots ->
                val selectedSpotId = uiState.value.selectedSpot?.id
                originalSpots = spots.withFavoriteState()

                val filteredSpots =
                    filterCollectionSpotsUseCase(
                        spots = originalSpots,
                        selectedTypes = uiState.value.selectedTypes,
                    )
                val updatedSelectedSpot =
                    selectedSpotId?.let { id ->
                        originalSpots.firstOrNull { spot -> spot.id == id }
                            ?: uiState.value.selectedSpot
                    }

                _uiState.update {
                    it.copy(
                        spots = filteredSpots,
                        selectedSpot = updatedSelectedSpot,
                        isLoading = false,
                        isFavoriteSpotNearbyLoading = false,
                        hasSearched = true,
                        errorMessageResId = null,
                        locationNotice = null,
                        searchMode = MapSearchMode.CURRENT_LOCATION,
                    )
                }
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable

                _uiState.update {
                    it.copy(isFavoriteSpotNearbyLoading = false)
                }
            }
        }
    }

    private fun updateSpotFailure(@StringRes messageResId: Int) {
        originalSpots = emptyList()

        _uiState.update {
            it.copy(
                spots = emptyList(),
                selectedSpot = null,
                isLoading = false,
                hasSearched = true,
                errorMessageResId = messageResId,
                locationNotice = null,
                isFavoriteSpotNearbyLoading = false,
            )
        }
    }

    private suspend fun searchByCurrentLocationInternal(
        showLoading: Boolean,
        preservePreviousResultOnFailure: Boolean,
    ) {
        if (showLoading) {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    hasSearched = true,
                    errorMessageResId = null,
                    locationNotice = null,
                    selectedSpot = null,
                    isFavoriteSpotNearbyLoading = false,
                    searchMode = MapSearchMode.CURRENT_LOCATION,
                )
            }
        }

        when (val result = currentLocationProvider.getCurrentLocation()) {
            is CurrentLocationResult.Found -> {
                searchByLocation(
                    coordinate = result.coordinate,
                    preservePreviousResultOnFailure = preservePreviousResultOnFailure,
                )
            }

            CurrentLocationResult.NotFound -> {
                if (!preservePreviousResultOnFailure) {
                    onCurrentLocationNotFound()
                }
            }

            CurrentLocationResult.LocationServiceDisabled -> {
                if (!preservePreviousResultOnFailure) {
                    onLocationServiceDisabled()
                }
            }

            CurrentLocationResult.PermissionDenied -> {
                handleLocationPermissionDenied()
            }
        }
    }

    private fun showCachedCurrentLocationSpots(spots: List<CollectionSpot>) {
        originalSpots = spots.withFavoriteState()

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
                errorMessageResId = null,
                locationNotice = null,
                isFavoriteSpotNearbyLoading = false,
                searchMode = MapSearchMode.CURRENT_LOCATION,
            )
        }
    }

    private fun refreshCurrentLocationSilently() {
        currentLocationRefreshJob?.cancel()
        currentLocationRefreshJob = viewModelScope.launch {
            searchByCurrentLocationInternal(
                showLoading = false,
                preservePreviousResultOnFailure = true,
            )
        }
    }

    private fun canStartInitialCurrentLocationSearch(): Boolean {
        val currentState = uiState.value

        return currentState.searchKeyword.isBlank() &&
            (!currentState.hasSearched || currentState.searchMode == MapSearchMode.CURRENT_LOCATION) &&
            locationPermissionChecker.hasFineLocationPermission()
    }

    private fun canApplyCurrentLocationResult(): Boolean {
        val currentState = uiState.value

        return currentState.searchMode == MapSearchMode.CURRENT_LOCATION
    }

    private fun observeCollectionSpotFavorites() {
        viewModelScope.launch {
            observeFavoritesUseCase(FavoriteTargetType.COLLECTION_SPOT)
                .collect { favorites ->
                    favoriteSpotIds = favorites.map { favorite -> favorite.targetId }.toSet()
                    applyFavoriteStateToCurrentResults()
                }
        }
    }

    private fun applyFavoriteStateToCurrentResults() {
        if (originalSpots.isEmpty() && uiState.value.selectedSpot == null) return

        originalSpots = originalSpots.withFavoriteState()

        val filteredSpots = filterCollectionSpotsUseCase(
            spots = originalSpots,
            selectedTypes = uiState.value.selectedTypes,
        )
        val updatedSelectedSpot = uiState.value.selectedSpot?.let { selectedSpot ->
            originalSpots.firstOrNull { spot -> spot.id == selectedSpot.id }
                ?: selectedSpot.copy(isBookmarked = selectedSpot.id in favoriteSpotIds)
        }

        _uiState.update {
            it.copy(
                spots = filteredSpots,
                selectedSpot = updatedSelectedSpot,
            )
        }
    }

    private fun List<CollectionSpot>.withFavoriteState(): List<CollectionSpot> =
        map { spot ->
            spot.copy(isBookmarked = spot.id in favoriteSpotIds)
        }

    private fun List<CollectionSpot>.withDistanceFrom(
        coordinate: Coordinate,
    ): List<CollectionSpot> =
        map { spot ->
            val spotCoordinate = spot.coordinate ?: return@map spot

            spot.copy(
                distanceMeter = calculateDistanceMeterUseCase(
                    from = coordinate,
                    to = spotCoordinate,
                ),
            )
        }

    private fun FavoriteSpotMapMoveRequest.toCollectionSpot(): CollectionSpot =
        CollectionSpot(
            id = targetId,
            name = name,
            type = type,
            address = address,
            detailLocation = detailLocation,
            coordinate = coordinate,
            distanceMeter = null,
            isBookmarked = targetId in favoriteSpotIds,
        )

    private companion object {
        const val DEFAULT_RADIUS_METER = 500
    }
}
