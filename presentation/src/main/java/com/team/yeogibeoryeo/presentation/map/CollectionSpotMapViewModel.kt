package com.team.yeogibeoryeo.presentation.map

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoritesUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ToggleCollectionSpotFavoriteUseCase
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotSearchResult
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.model.MapRegionSearchCandidate
import com.team.yeogibeoryeo.domain.spot.model.MapRegionSearchCandidateResult
import com.team.yeogibeoryeo.domain.spot.log.MapSearchTimingLogger
import com.team.yeogibeoryeo.domain.spot.usecase.CalculateDistanceMeterUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.ClearRecentCurrentLocationSpotsUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.FilterCollectionSpotsUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.GetFreshRecentCurrentLocationSpotsUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.ResolveMapRegionSearchCandidateUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.SaveRecentCurrentLocationSpotsUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.SearchCollectionSpotsByKeywordUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.SearchCollectionSpotsByLocationUseCase
import com.team.yeogibeoryeo.presentation.cache.RecentCurrentLocationCacheClearEvent
import com.team.yeogibeoryeo.presentation.cache.RecentCurrentLocationCacheClearNotifier
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.map.location.CurrentLocationProvider
import com.team.yeogibeoryeo.presentation.map.location.CurrentLocationResult
import com.team.yeogibeoryeo.presentation.map.location.LocationPermissionChecker
import com.team.yeogibeoryeo.presentation.map.model.FavoriteSpotMapMoveRequest
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
class CollectionSpotMapViewModel @Inject constructor(
    private val resolveMapRegionSearchCandidateUseCase: ResolveMapRegionSearchCandidateUseCase,
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
    private val recentCurrentLocationCacheClearNotifier: RecentCurrentLocationCacheClearNotifier,
    private val mapSearchTimingLogger: MapSearchTimingLogger = MapSearchTimingLogger.NoOp,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionSpotMapUiState())
    val uiState: StateFlow<CollectionSpotMapUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<CollectionSpotMapEvent>()
    val events: SharedFlow<CollectionSpotMapEvent> = _events.asSharedFlow()

    private var originalSpots: List<CollectionSpot> = emptyList()
    private var spotSearchJob: Job? = null
    private var currentLocationRefreshJob: Job? = null
    private var hasRequestedInitialCurrentLocationSearch = false
    private var currentLocationSearchGeneration = 0
    private var favoriteSpotIds: Set<String> = emptySet()
    private val consumedFavoriteSpotMoveRequestIds = mutableSetOf<String>()
    private val favoriteToggleJobs = mutableMapOf<String, Job>()

    init {
        observeCollectionSpotFavorites()
        observeRecentCurrentLocationCacheClearEvents()
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
                regionSearchCandidates = emptyList(),
                regionDetailSearchCandidate = null,
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
                partialWarningMessageResId = null,
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
                    partialWarningMessageResId = null,
                    locationNotice = null,
                    regionSearchCandidates = emptyList(),
                    regionDetailSearchCandidate = null,
                    isFavoriteSpotNearbyLoading = false,
                    searchMode = MapSearchMode.KEYWORD,
                )
            }
            return
        }

        spotSearchJob?.cancel()
        startKeywordSearchLoading()
        spotSearchJob = viewModelScope.launch {
            when (val candidateResult = resolveMapRegionSearchCandidateUseCase(keyword)) {
                is MapRegionSearchCandidateResult.NeedSelection -> {
                    mapSearchTimingLogger.log(
                        "keyword search candidate selection required query=$keyword " +
                            "candidateCount=${candidateResult.candidates.size}",
                    )
                    showRegionSearchCandidates(candidateResult.candidates)
                }

                is MapRegionSearchCandidateResult.ReadyToSearch -> {
                    val selectedCandidate = candidateResult.selectedCandidate
                    if (selectedCandidate != null && selectedCandidate.hasDetailSearchOptions()) {
                        showRegionDetailSearchOptions(
                            candidate = selectedCandidate,
                            previousCandidates = emptyList(),
                        )
                    } else {
                        searchByKeywordInternal(
                            keyword = candidateResult.searchKeyword,
                            selectedRegionCandidate = selectedCandidate,
                        )
                    }
                }
            }
        }
    }

    fun onRegionSearchCandidateClick(candidate: MapRegionSearchCandidate) {
        if (candidate.hasDetailSearchOptions()) {
            showRegionDetailSearchOptions(
                candidate = candidate,
                previousCandidates = uiState.value.regionSearchCandidates,
            )
            return
        }

        currentLocationRefreshJob?.cancel()
        spotSearchJob?.cancel()
        spotSearchJob = viewModelScope.launch {
            searchByKeywordInternal(
                keyword = candidate.searchKeyword,
                selectedRegionCandidate = candidate,
            )
        }
    }

    fun onRegionDetailSearchAllClick() {
        val candidate = uiState.value.regionDetailSearchCandidate ?: return
        searchByRegionDetailSelection(
            candidate = candidate,
            keyword = candidate.searchKeyword,
            searchKeywords = candidate.searchKeywords,
        )
    }

    fun onRegionDetailSearchKeywordClick(keyword: String) {
        val candidate = uiState.value.regionDetailSearchCandidate ?: return
        searchByRegionDetailSelection(
            candidate = candidate,
            keyword = keyword,
            searchKeywords = listOf(keyword),
        )
    }

    fun onRegionDetailSearchBack() {
        _uiState.update {
            it.copy(regionDetailSearchCandidate = null)
        }
    }

    private fun searchByRegionDetailSelection(
        candidate: MapRegionSearchCandidate,
        keyword: String,
        searchKeywords: List<String>,
    ) {
        currentLocationRefreshJob?.cancel()
        spotSearchJob?.cancel()
        _uiState.update {
            it.copy(searchKeyword = keyword)
        }
        spotSearchJob = viewModelScope.launch {
            searchByKeywordInternal(
                keyword = keyword,
                selectedRegionCandidate = candidate,
                searchKeywordsOverride = searchKeywords,
            )
        }
    }

    private suspend fun searchByKeywordInternal(
        keyword: String,
        selectedRegionCandidate: MapRegionSearchCandidate?,
        searchKeywordsOverride: List<String>? = null,
    ) {
        val searchStartedAtNanos = System.nanoTime()
        mapSearchTimingLogger.log(
            "keyword search started query=$keyword " +
                "selectedRegion=${selectedRegionCandidate?.displayName ?: NO_SELECTED_REGION}",
        )
        startKeywordSearchLoading()

        runCatching {
            searchByCandidateKeywords(
                keyword = keyword,
                selectedRegionCandidate = selectedRegionCandidate,
                searchKeywordsOverride = searchKeywordsOverride,
            )
        }.onSuccess { result ->
            updateSpotResult(
                result = result,
                searchStartedAtNanos = searchStartedAtNanos,
            )
        }.onFailure { throwable ->
            if (throwable is CancellationException) throw throwable

            updateSpotFailure(
                messageResId = MapLocationNotices.SpotSearchFailureMessageResId,
            )
        }
    }

    private fun startKeywordSearchLoading() {
        _uiState.update {
            it.copy(
                isLoading = true,
                hasSearched = true,
                errorMessageResId = null,
                partialWarningMessageResId = null,
                locationNotice = null,
                regionSearchCandidates = emptyList(),
                regionDetailSearchCandidate = null,
                selectedSpot = null,
                isFavoriteSpotNearbyLoading = false,
                searchMode = MapSearchMode.KEYWORD,
            )
        }
    }

    private suspend fun searchByCandidateKeywords(
        keyword: String,
        selectedRegionCandidate: MapRegionSearchCandidate?,
        searchKeywordsOverride: List<String>? = null,
    ): CollectionSpotSearchResult {
        val searchKeywords = searchKeywordsOverride ?: selectedRegionCandidate
            ?.searchKeywords
            ?.takeIf { keywords -> keywords.isNotEmpty() }
            ?: listOf(keyword)
        val results = searchKeywords.map { searchKeyword ->
            searchCollectionSpotsByKeywordUseCase.searchWithResult(
                keyword = searchKeyword,
                types = emptySet(),
                selectedRegionCandidate = selectedRegionCandidate,
            )
        }
        val mergeStartedAtNanos = System.nanoTime()
        val mergedSpots = results.flatMap { result -> result.spots }
        val dedupedSpots = mergedSpots.distinctBy { spot -> spot.id }

        if (searchKeywords.size > 1) {
            mapSearchTimingLogger.log(
                "candidate keyword merge/dedup finished keywords=${searchKeywords.size} " +
                    "before=${mergedSpots.size} after=${dedupedSpots.size} " +
                    "elapsedMs=${mergeStartedAtNanos.elapsedMs()}",
            )
        }

        return CollectionSpotSearchResult(
            spots = dedupedSpots,
            isPartial = results.any { result -> result.isPartial },
        )
    }

    private fun showRegionSearchCandidates(candidates: List<MapRegionSearchCandidate>) {
        originalSpots = emptyList()

        _uiState.update {
            it.copy(
                spots = emptyList(),
                regionSearchCandidates = candidates,
                regionDetailSearchCandidate = null,
                selectedSpot = null,
                isLoading = false,
                hasSearched = false,
                errorMessageResId = null,
                partialWarningMessageResId = null,
                locationNotice = null,
                isFavoriteSpotNearbyLoading = false,
                searchMode = MapSearchMode.KEYWORD,
            )
        }
    }

    private fun showRegionDetailSearchOptions(
        candidate: MapRegionSearchCandidate,
        previousCandidates: List<MapRegionSearchCandidate>,
    ) {
        originalSpots = emptyList()

        _uiState.update {
            it.copy(
                spots = emptyList(),
                regionSearchCandidates = previousCandidates,
                regionDetailSearchCandidate = candidate,
                selectedSpot = null,
                isLoading = false,
                hasSearched = false,
                errorMessageResId = null,
                partialWarningMessageResId = null,
                locationNotice = null,
                isFavoriteSpotNearbyLoading = false,
                searchMode = MapSearchMode.KEYWORD,
            )
        }
    }

    fun searchByCurrentLocation() {
        currentLocationRefreshJob?.cancel()
        spotSearchJob?.cancel()
        val searchGeneration = ++currentLocationSearchGeneration
        _uiState.update {
            it.copy(searchKeyword = EMPTY_SEARCH_KEYWORD)
        }
        spotSearchJob = viewModelScope.launch {
            if (!locationPermissionChecker.hasFineLocationPermission()) {
                handleLocationPermissionDenied()
                return@launch
            }

            val cachedEntry = getFreshRecentCurrentLocationSpotsUseCase()

            if (cachedEntry != null) {
                if (searchGeneration != currentLocationSearchGeneration) return@launch
                showCachedCurrentLocationSpots(cachedEntry.spots)
                refreshCurrentLocationSilently()
                return@launch
            }

            searchByCurrentLocationInternal(
                showLoading = true,
                preservePreviousResultOnFailure = false,
                searchGeneration = searchGeneration,
            )
        }
    }

    fun searchByMapCenter(coordinate: Coordinate) {
        currentLocationRefreshJob?.cancel()
        spotSearchJob?.cancel()
        spotSearchJob = viewModelScope.launch {
            val searchStartedAtNanos = System.nanoTime()
            mapSearchTimingLogger.log(
                "map center search started latitude=${coordinate.latitude} " +
                    "longitude=${coordinate.longitude}",
            )
            _uiState.update {
                it.copy(
                    isLoading = true,
                    hasSearched = true,
                    searchKeyword = EMPTY_SEARCH_KEYWORD,
                    errorMessageResId = null,
                    partialWarningMessageResId = null,
                    locationNotice = null,
                    regionSearchCandidates = emptyList(),
                    regionDetailSearchCandidate = null,
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
                updateSpotResult(
                    result = CollectionSpotSearchResult(spots = spots),
                    searchStartedAtNanos = searchStartedAtNanos,
                )
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
        searchGeneration: Int,
    ) {
        val searchStartedAtNanos = System.nanoTime()
        mapSearchTimingLogger.log(
            "current location search started latitude=${coordinate.latitude} " +
                "longitude=${coordinate.longitude}",
        )
        try {
            val spots = searchCollectionSpotsByLocationUseCase(
                coordinate = coordinate,
                radiusMeter = DEFAULT_RADIUS_METER,
                types = emptySet(),
            )

            if (!canApplyCurrentLocationResult(searchGeneration)) return

            val spotsWithDistance = spots.withDistanceFrom(coordinate)

            updateSpotResult(
                result = CollectionSpotSearchResult(spots = spotsWithDistance),
                searchStartedAtNanos = searchStartedAtNanos,
            )
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
                partialWarningMessageResId = null,
                locationNotice = null,
                regionSearchCandidates = emptyList(),
                regionDetailSearchCandidate = null,
                favoriteSpotMoveRequestId = request.targetId,
                favoriteSpotMoveRequestSequence = it.favoriteSpotMoveRequestSequence + 1,
                isFavoriteSpotNearbyLoading = true,
            )
        }

        searchNearbySpotsForFavoriteSpot(request)
    }

    fun onSpotFavoriteClick(spot: CollectionSpot) {
        if (favoriteToggleJobs[spot.id]?.isActive == true) return

        favoriteToggleJobs[spot.id] = viewModelScope.launch {
            try {
                toggleCollectionSpotFavoriteUseCase(spot)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Throwable) {
                _events.emit(CollectionSpotMapEvent.FavoriteUpdateFailed)
            } finally {
                favoriteToggleJobs.remove(spot.id)
            }
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

    private suspend fun clearRecentCurrentLocationCache() {
        clearRecentCurrentLocationSpotsUseCase()
    }

    private fun clearCurrentLocationSearchMemoryState() {
        if (uiState.value.searchMode != MapSearchMode.CURRENT_LOCATION) return

        currentLocationRefreshJob?.cancel()
        spotSearchJob?.cancel()
        originalSpots = emptyList()
        hasRequestedInitialCurrentLocationSearch = false

        _uiState.update {
            it.copy(
                spots = emptyList(),
                selectedSpot = null,
                isLoading = false,
                hasSearched = false,
                searchKeyword = EMPTY_SEARCH_KEYWORD,
                errorMessageResId = null,
                partialWarningMessageResId = null,
                locationNotice = null,
                regionSearchCandidates = emptyList(),
                regionDetailSearchCandidate = null,
                isFavoriteSpotNearbyLoading = false,
                searchMode = MapSearchMode.KEYWORD,
            )
        }
    }

    private suspend fun handleLocationPermissionDenied() {
        clearRecentCurrentLocationCache()
        showLocationPermissionDeniedNotice()
    }

    private fun showLocationPermissionDeniedNotice() {
        originalSpots = emptyList()

        _uiState.update {
            it.copy(
                spots = emptyList(),
                selectedSpot = null,
                isLoading = false,
                hasSearched = false,
                searchKeyword = EMPTY_SEARCH_KEYWORD,
                errorMessageResId = null,
                partialWarningMessageResId = null,
                locationNotice = MapLocationNotices.PermissionDenied,
                regionSearchCandidates = emptyList(),
                regionDetailSearchCandidate = null,
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
            currentState.searchKeyword.isNotBlank()
        ) {
            return
        }

        if (!locationPermissionChecker.hasFineLocationPermission()) {
            onLocationPermissionDenied()
            return
        }

        spotSearchJob?.cancel()
        val searchGeneration = ++currentLocationSearchGeneration
        spotSearchJob = viewModelScope.launch {
            val cachedEntry = getFreshRecentCurrentLocationSpotsUseCase()

            if (cachedEntry != null && canStartInitialCurrentLocationSearch()) {
                if (searchGeneration != currentLocationSearchGeneration) return@launch
                showCachedCurrentLocationSpots(cachedEntry.spots)
                refreshCurrentLocationSilently()
                return@launch
            }

            if (canStartInitialCurrentLocationSearch()) {
                searchByCurrentLocationInternal(
                    showLoading = true,
                    preservePreviousResultOnFailure = false,
                    searchGeneration = searchGeneration,
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
                searchKeyword = EMPTY_SEARCH_KEYWORD,
                errorMessageResId = null,
                partialWarningMessageResId = null,
                locationNotice = MapLocationNotices.CurrentLocationUnavailable,
                regionSearchCandidates = emptyList(),
                regionDetailSearchCandidate = null,
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
                searchKeyword = EMPTY_SEARCH_KEYWORD,
                errorMessageResId = null,
                partialWarningMessageResId = null,
                locationNotice = MapLocationNotices.LocationServiceDisabled,
                regionSearchCandidates = emptyList(),
                regionDetailSearchCandidate = null,
                isFavoriteSpotNearbyLoading = false,
                searchMode = MapSearchMode.KEYWORD,
            )
        }
    }

    private fun updateSpotResult(
        result: CollectionSpotSearchResult,
        searchStartedAtNanos: Long? = null,
    ) {
        originalSpots = result.spots.withFavoriteState()

        val filteredSpots = filterCollectionSpotsUseCase(
            spots = originalSpots,
            selectedTypes = uiState.value.selectedTypes,
        )

        if (searchStartedAtNanos != null) {
            mapSearchTimingLogger.log(
                "viewModel search success resultCount=${filteredSpots.size} " +
                    "elapsedMs=${searchStartedAtNanos.elapsedMs()}",
            )
        }
        val stateUpdateStartedAtNanos = System.nanoTime()
        _uiState.update {
            it.copy(
                spots = filteredSpots,
                selectedSpot = null,
                isLoading = false,
                hasSearched = true,
                errorMessageResId = null,
                partialWarningMessageResId = if (result.isPartial) {
                    R.string.map_spot_search_partial_failure_message
                } else {
                    null
                },
                locationNotice = null,
                regionSearchCandidates = emptyList(),
                regionDetailSearchCandidate = null,
                isFavoriteSpotNearbyLoading = false,
            )
        }
        mapSearchTimingLogger.log(
            "ui state updated mode=ResultList resultCount=${filteredSpots.size} " +
                "elapsedMs=${stateUpdateStartedAtNanos.elapsedMs()}",
        )
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
                        partialWarningMessageResId = null,
                        locationNotice = null,
                        regionSearchCandidates = emptyList(),
                        regionDetailSearchCandidate = null,
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
                partialWarningMessageResId = null,
                locationNotice = null,
                regionSearchCandidates = emptyList(),
                regionDetailSearchCandidate = null,
                isFavoriteSpotNearbyLoading = false,
            )
        }
    }

    private suspend fun searchByCurrentLocationInternal(
        showLoading: Boolean,
        preservePreviousResultOnFailure: Boolean,
        searchGeneration: Int,
    ) {
        if (searchGeneration != currentLocationSearchGeneration) return

        if (showLoading) {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    hasSearched = true,
                    searchKeyword = EMPTY_SEARCH_KEYWORD,
                    errorMessageResId = null,
                    partialWarningMessageResId = null,
                    locationNotice = null,
                    regionSearchCandidates = emptyList(),
                    regionDetailSearchCandidate = null,
                    selectedSpot = null,
                    isFavoriteSpotNearbyLoading = false,
                    searchMode = MapSearchMode.CURRENT_LOCATION,
                )
            }
        }

        val result = currentLocationProvider.getCurrentLocation()
        if (searchGeneration != currentLocationSearchGeneration) return

        when (result) {
            is CurrentLocationResult.Found -> {
                searchByLocation(
                    coordinate = result.coordinate,
                    preservePreviousResultOnFailure = preservePreviousResultOnFailure,
                    searchGeneration = searchGeneration,
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
                searchKeyword = EMPTY_SEARCH_KEYWORD,
                errorMessageResId = null,
                partialWarningMessageResId = null,
                locationNotice = null,
                regionSearchCandidates = emptyList(),
                regionDetailSearchCandidate = null,
                isFavoriteSpotNearbyLoading = false,
                searchMode = MapSearchMode.CURRENT_LOCATION,
            )
        }
    }

    private fun refreshCurrentLocationSilently() {
        currentLocationRefreshJob?.cancel()
        val searchGeneration = currentLocationSearchGeneration
        currentLocationRefreshJob = viewModelScope.launch {
            searchByCurrentLocationInternal(
                showLoading = false,
                preservePreviousResultOnFailure = true,
                searchGeneration = searchGeneration,
            )
        }
    }

    private fun canStartInitialCurrentLocationSearch(): Boolean {
        val currentState = uiState.value

        return currentState.searchKeyword.isBlank() &&
            (!currentState.hasSearched || currentState.searchMode == MapSearchMode.CURRENT_LOCATION) &&
            locationPermissionChecker.hasFineLocationPermission()
    }

    private fun canApplyCurrentLocationResult(searchGeneration: Int): Boolean {
        val currentState = uiState.value

        return currentState.searchMode == MapSearchMode.CURRENT_LOCATION &&
            searchGeneration == currentLocationSearchGeneration
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

    private fun observeRecentCurrentLocationCacheClearEvents() {
        viewModelScope.launch {
            recentCurrentLocationCacheClearNotifier.events.collect { event ->
                when (event) {
                    RecentCurrentLocationCacheClearEvent.ClearRequested -> {
                        currentLocationSearchGeneration += 1
                    }

                    RecentCurrentLocationCacheClearEvent.ClearSucceeded -> {
                        currentLocationSearchGeneration += 1
                        clearCurrentLocationSearchMemoryState()
                    }
                }
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
        const val EMPTY_SEARCH_KEYWORD = ""
        const val NO_SELECTED_REGION = "none"
    }
}

sealed interface CollectionSpotMapEvent {
    data object FavoriteUpdateFailed : CollectionSpotMapEvent
}

private fun Long.elapsedMs(): Long =
    (System.nanoTime() - this) / NANOS_PER_MILLISECOND

private fun MapRegionSearchCandidate.hasDetailSearchOptions(): Boolean =
    searchKeywords.distinct().size > 1

private const val NANOS_PER_MILLISECOND = 1_000_000L
