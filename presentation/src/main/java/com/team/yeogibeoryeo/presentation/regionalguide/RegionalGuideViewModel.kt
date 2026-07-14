package com.team.yeogibeoryeo.presentation.regionalguide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteKey
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.usecase.GetRegionalGuideFavoriteSnapshotUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoriteUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ToggleRegionalGuideFavoriteUseCase
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.usecase.ClassifyRegionSearchInputUseCase
import com.team.yeogibeoryeo.domain.region.usecase.ExtractRegionFromAddressUseCase
import com.team.yeogibeoryeo.domain.region.usecase.GetSidoOptionsUseCase
import com.team.yeogibeoryeo.domain.region.usecase.GetSigunguOptionsUseCase
import com.team.yeogibeoryeo.domain.region.usecase.RegionSearchInputType
import com.team.yeogibeoryeo.domain.region.usecase.ResolveRegionFromKeywordResult
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideCandidateLookupReason
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideEupmyeondongNamePolicy
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideFailureReason
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupResult
import com.team.yeogibeoryeo.domain.regionalguide.usecase.GetRegionalDisposalGuideUseCase
import com.team.yeogibeoryeo.domain.regionalguide.usecase.GetRegionalGuideEupmyeondongOptionsUseCase
import com.team.yeogibeoryeo.domain.regionalguide.usecase.NormalizeRegionalGuideDisplayRegionUseCase
import com.team.yeogibeoryeo.domain.regionalguide.usecase.ResolveRegionalGuideRegionFromKeywordUseCase
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.regionalguide.mapper.toUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionSearchCandidateUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideCandidateUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.regionalGuideCandidateDisplayComparator
import com.team.yeogibeoryeo.presentation.regionalguide.model.withDuplicateDisplayDisambiguation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegionalGuideViewModel @Inject constructor(
    private val classifyRegionSearchInputUseCase: ClassifyRegionSearchInputUseCase,
    private val resolveRegionFromKeywordUseCase: ResolveRegionalGuideRegionFromKeywordUseCase,
    private val extractRegionFromAddressUseCase: ExtractRegionFromAddressUseCase,
    private val getRegionalDisposalGuideUseCase: GetRegionalDisposalGuideUseCase,
    private val getSidoOptionsUseCase: GetSidoOptionsUseCase,
    private val getSigunguOptionsUseCase: GetSigunguOptionsUseCase,
    private val getRegionalGuideEupmyeondongOptionsUseCase: GetRegionalGuideEupmyeondongOptionsUseCase,
    private val normalizeRegionalGuideDisplayRegionUseCase: NormalizeRegionalGuideDisplayRegionUseCase,
    private val observeFavoriteUseCase: ObserveFavoriteUseCase,
    private val toggleRegionalGuideFavoriteUseCase: ToggleRegionalGuideFavoriteUseCase,
    private val getRegionalGuideFavoriteSnapshotUseCase: GetRegionalGuideFavoriteSnapshotUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegionalGuideUiState>(RegionalGuideUiState.Idle)
    val uiState: StateFlow<RegionalGuideUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<RegionalGuideEvent>()
    val events: SharedFlow<RegionalGuideEvent> = _events.asSharedFlow()

    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    private val _regionSelectorUiState = MutableStateFlow(RegionSelectorUiState())
    val regionSelectorUiState: StateFlow<RegionSelectorUiState> =
        _regionSelectorUiState.asStateFlow()

    private var guideLookupJob: Job? = null
    private var keywordSuggestionJob: Job? = null
    private var sigunguOptionsJob: Job? = null
    private var eupmyeondongOptionsJob: Job? = null
    private var eupmyeondongOptionsRequestId = 0L
    private var favoriteStateJob: Job? = null
    private var lastRequest: RegionalGuideRequest? = null
    private var currentRegionalGuideFavoriteSnapshot: RegionalGuideFavoriteSnapshot? = null
    private val guideCandidateBackStackEntries = mutableListOf<RegionalGuideCandidateBackStackEntry>()
    private val favoriteToggleJobs = mutableMapOf<String, Job>()

    init {
        loadSidoOptions()
    }

    fun onSearchKeywordChanged(keyword: String) {
        clearGuideCandidateBackStack()
        _searchKeyword.value = keyword
        clearStaleCandidateState(keyword)
        scheduleKeywordSuggestion(keyword)
    }

    fun onSidoSelected(sido: String) {
        dismissSearchCandidateState()
        clearGuideCandidateBackStack()
        sigunguOptionsJob?.cancel()
        eupmyeondongOptionsJob?.cancel()
        eupmyeondongOptionsRequestId += 1

        _regionSelectorUiState.update { state ->
            state.copy(
                selectedSido = sido,
                selectedSigungu = null,
                selectedEupmyeondong = null,
                sigunguOptions = emptyList(),
                eupmyeondongOptions = emptyList(),
                isEupmyeondongOptionsLoading = false,
                expandedDropdown = null
            )
        }

        sigunguOptionsJob = viewModelScope.launch {
            val sigunguOptions = getSigunguOptionsUseCase(sido)

            _regionSelectorUiState.update { state ->
                if (state.selectedSido == sido) {
                    state.copy(sigunguOptions = sigunguOptions)
                } else {
                    state
                }
            }
        }
    }

    fun onSigunguSelected(sigungu: String) {
        dismissSearchCandidateState()
        clearGuideCandidateBackStack()
        eupmyeondongOptionsJob?.cancel()

        val selectedSido = regionSelectorUiState.value.selectedSido ?: return
        val requestId = ++eupmyeondongOptionsRequestId

        _regionSelectorUiState.update { state ->
            state.copy(
                selectedSigungu = sigungu,
                selectedEupmyeondong = null,
                eupmyeondongOptions = emptyList(),
                isEupmyeondongOptionsLoading = true,
                expandedDropdown = null
            )
        }

        eupmyeondongOptionsJob = viewModelScope.launch {
            try {
                val eupmyeondongOptions = getRegionalGuideEupmyeondongOptionsUseCase(
                    sido = selectedSido,
                    sigungu = sigungu
                )

                _regionSelectorUiState.update { state ->
                    if (eupmyeondongOptionsRequestId == requestId) {
                        state.copy(eupmyeondongOptions = eupmyeondongOptions)
                    } else {
                        state
                    }
                }
            } finally {
                _regionSelectorUiState.update { state ->
                    if (eupmyeondongOptionsRequestId == requestId) {
                        state.copy(isEupmyeondongOptionsLoading = false)
                    } else {
                        state
                    }
                }
            }
        }
    }

    fun onEupmyeondongSelected(eupmyeondong: String) {
        dismissSearchCandidateState()
        clearGuideCandidateBackStack()
        _regionSelectorUiState.update { state ->
            state.copy(
                selectedEupmyeondong = eupmyeondong,
                expandedDropdown = null
            )
        }
    }

    fun onRegionSelectorDropdownExpanded(dropdown: RegionSelectorDropdown) {
        dismissSearchCandidateState()
        _regionSelectorUiState.update { state ->
            if (dropdown == RegionSelectorDropdown.EUPMYEONDONG &&
                !state.isEupmyeondongSelectionEnabled
            ) {
                state
            } else {
                state.copy(expandedDropdown = dropdown)
            }
        }
    }

    fun onRegionSelectorDropdownDismissed() {
        collapseRegionSelectorDropdowns()
    }

    fun onRegionSelectionSearchClick() {
        dismissSearchCandidateState()
        collapseRegionSelectorDropdowns()

        val state = regionSelectorUiState.value
        val selectedSido = state.selectedSido
        val selectedSigungu = state.selectedSigungu

        if (selectedSido.isNullOrBlank() || selectedSigungu.isNullOrBlank()) {
            _uiState.value = RegionalGuideUiState.Empty(
                query = state.selectedRegionText.orEmpty(),
                titleResId = R.string.regional_guide_empty_select_region_title,
                messageResId = R.string.regional_guide_empty_select_region_message,
                action = selectRegionAction()
            )
            return
        }

        val region = Region(
            sido = selectedSido,
            sigungu = selectedSigungu,
            eupmyeondong = state.selectedEupmyeondong
        )
        val query = state.selectedRegionText ?: "$selectedSido > $selectedSigungu"

        searchBySelectedRegion(
            query = query,
            region = region,
            syncSearchKeyword = true,
        )
    }

    fun searchCurrentKeyword() {
        searchByKeyword(_searchKeyword.value)
    }

    fun retryLastRequest() {
        when (val request = lastRequest) {
            is RegionalGuideRequest.Keyword -> searchByKeyword(request.keyword)
            is RegionalGuideRequest.Address -> loadByAddress(request.address)
            is RegionalGuideRequest.Favorite -> loadByFavoriteTargetId(request.targetId)
            is RegionalGuideRequest.SelectedRegion -> searchBySelectedRegion(
                query = request.query,
                region = request.region
            )
            null -> searchCurrentKeyword()
        }
    }

    fun prepareSearchAgain() {
        if (_uiState.value is RegionalGuideUiState.Empty) {
            _uiState.value = RegionalGuideUiState.Idle
        }
    }

    fun onRegionCandidateSelected(candidate: RegionSearchCandidateUiModel) {
        if (!candidate.isValid) return

        val ambiguousState = uiState.value as? RegionalGuideUiState.Ambiguous
        if (ambiguousState != null) {
            pushGuideCandidateBackStackEntry(ambiguousState)
        }

        val region = candidate.toRegion()
        searchBySelectedRegion(
            query = candidate.displayText,
            region = region,
            clearCandidateBackStack = ambiguousState == null,
        )
    }

    fun onRegionalGuideCandidateSelected(candidate: RegionalGuideCandidateUiModel) {
        keywordSuggestionJob?.cancel()

        val guideCandidatesState = uiState.value as? RegionalGuideUiState.GuideCandidates
        val query = guideCandidatesState
            ?.query
            ?: candidate.displayText

        if (guideCandidatesState != null) {
            pushGuideCandidateBackStackEntry(guideCandidatesState)
        }

        applyRegionSelection(candidate.toRegion())

        val snapshot = candidate.toFavoriteSnapshot()
        currentRegionalGuideFavoriteSnapshot = snapshot
        observeRegionalGuideFavoriteState(snapshot)

        _uiState.value = RegionalGuideUiState.Success(
            query = query,
            guide = candidate.guide,
            canRestoreCandidates = guideCandidateBackStackEntries.isNotEmpty(),
        )
    }

    fun restoreCandidatesFromDetail(): Boolean {
        val entry = guideCandidateBackStackEntries.removeLastOrNull()

        guideLookupJob?.cancel()
        keywordSuggestionJob?.cancel()
        favoriteStateJob?.cancel()
        currentRegionalGuideFavoriteSnapshot = null

        if (entry == null) {
            return when (_uiState.value) {
                is RegionalGuideUiState.Ambiguous,
                is RegionalGuideUiState.GuideCandidates -> {
                    _searchKeyword.value = ""
                    clearSelectedRegion()
                    _uiState.value = RegionalGuideUiState.Idle
                    true
                }

                else -> false
            }
        }

        lastRequest = entry.lastRequest
        _searchKeyword.value = entry.searchKeyword
        _regionSelectorUiState.value = entry.regionSelectorUiState
        _uiState.value = entry.uiState

        return true
    }

    fun onFavoriteClick() {
        val snapshot = currentRegionalGuideFavoriteSnapshot ?: return
        if (favoriteToggleJobs[snapshot.targetId]?.isActive == true) return

        favoriteToggleJobs[snapshot.targetId] = viewModelScope.launch {
            try {
                toggleRegionalGuideFavoriteUseCase(snapshot)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Throwable) {
                _events.emit(RegionalGuideEvent.FavoriteUpdateFailed)
            } finally {
                favoriteToggleJobs.remove(snapshot.targetId)
            }
        }
    }

    fun searchByKeyword(keyword: String) {
        val trimmedKeyword = keyword.trim()

        clearGuideCandidateBackStack()
        _searchKeyword.value = keyword
        keywordSuggestionJob?.cancel()
        guideLookupJob?.cancel()
        collapseRegionSelectorDropdowns()

        if (trimmedKeyword.isBlank()) {
            _uiState.value = RegionalGuideUiState.Empty(
                query = trimmedKeyword,
                titleResId = R.string.regional_guide_empty_blank_keyword_title,
                messageResId = R.string.regional_guide_empty_blank_keyword_message,
                action = searchAgainAction()
            )
            return
        }

        if (classifyRegionSearchInputUseCase(trimmedKeyword) == RegionSearchInputType.ADDRESS) {
            loadByAddress(trimmedKeyword)
            return
        }

        lastRequest = RegionalGuideRequest.Keyword(trimmedKeyword)

        guideLookupJob = viewModelScope.launch {
            clearSelectedRegion()
            _uiState.value = RegionalGuideUiState.Loading(query = trimmedKeyword)

            try {
                when (val result = resolveRegionFromKeywordUseCase(trimmedKeyword)) {
                    is ResolveRegionFromKeywordResult.Ambiguous -> {
                        _uiState.value = RegionalGuideUiState.Ambiguous(
                            query = trimmedKeyword,
                            message = AMBIGUOUS_REGION_MESSAGE,
                            candidates = result.candidates
                                .map { region -> region.toCandidateUiModel() }
                        )
                    }

                    ResolveRegionFromKeywordResult.NotFound -> {
                        _uiState.value = RegionalGuideUiState.Empty(
                            query = trimmedKeyword,
                            titleResId = R.string.regional_guide_empty_keyword_not_found_title,
                            messageResId = R.string.regional_guide_empty_keyword_not_found_message,
                            action = searchAgainAction()
                        )
                    }

                    is ResolveRegionFromKeywordResult.Resolved -> {
                        val selectionResult = normalizeAndApplyRegionSelection(result.region)
                        if (selectionResult.removedEupmyeondong != null) {
                            _uiState.value = unavailableEupmyeondongEmptyState(trimmedKeyword)
                            return@launch
                        }

                        loadRegionalGuide(
                            query = trimmedKeyword,
                            region = selectionResult.region
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                clearGuideCandidateBackStack()
                _uiState.value = RegionalGuideUiState.Error(
                    query = trimmedKeyword,
                    message = e.message ?: "지역별 배출 가이드를 조회하는 중 오류가 발생했습니다."
                )
            }
        }
    }

    fun loadByAddress(address: String) {
        val trimmedAddress = address.trim()

        clearGuideCandidateBackStack()
        keywordSuggestionJob?.cancel()
        guideLookupJob?.cancel()

        if (trimmedAddress.isBlank()) {
            _uiState.value = RegionalGuideUiState.Empty(
                query = trimmedAddress,
                titleResId = R.string.regional_guide_empty_blank_address_title,
                messageResId = R.string.regional_guide_empty_blank_address_message,
                action = searchAgainAction()
            )
            return
        }

        lastRequest = RegionalGuideRequest.Address(trimmedAddress)

        guideLookupJob = viewModelScope.launch {
            clearSelectedRegion()
            _uiState.value = RegionalGuideUiState.Loading(query = trimmedAddress)

            try {
                val region = extractRegionFromAddressUseCase(trimmedAddress)

                if (region == null) {
                    _uiState.value = RegionalGuideUiState.Empty(
                        query = trimmedAddress,
                        titleResId = R.string.regional_guide_empty_address_parse_failed_title,
                        messageResId = R.string.regional_guide_empty_address_parse_failed_message,
                        action = searchAgainAction()
                    )
                    return@launch
                }

                val selectionResult = normalizeAndApplyRegionSelection(region)
                if (selectionResult.removedEupmyeondong != null) {
                    _uiState.value = unavailableEupmyeondongEmptyState(trimmedAddress)
                    return@launch
                }

                loadRegionalGuide(
                    query = trimmedAddress,
                    region = selectionResult.region
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                clearGuideCandidateBackStack()
                _uiState.value = RegionalGuideUiState.Error(
                    query = trimmedAddress,
                    message = e.message ?: "주소 기반 지역별 배출 가이드를 조회하는 중 오류가 발생했습니다."
                )
            }
        }
    }

    fun loadByFavoriteTargetId(targetId: String) {
        clearGuideCandidateBackStack()
        keywordSuggestionJob?.cancel()
        guideLookupJob?.cancel()

        lastRequest = RegionalGuideRequest.Favorite(targetId)

        guideLookupJob = viewModelScope.launch {
            _uiState.value = RegionalGuideUiState.Loading(query = "")

            try {
                val snapshot = getRegionalGuideFavoriteSnapshotUseCase(targetId)

                if (snapshot == null) {
                    _uiState.value = RegionalGuideUiState.Empty(
                        query = "",
                        titleResId = R.string.regional_guide_empty_favorite_restore_failed_title,
                        messageResId = R.string.regional_guide_empty_favorite_restore_failed_message,
                        action = selectRegionAction()
                    )
                    return@launch
                }

                val selectionResult = normalizeAndApplyRegionSelection(snapshot.region)
                if (selectionResult.removedEupmyeondong != null) {
                    _uiState.value = unavailableEupmyeondongEmptyState(snapshot.displayText())
                    return@launch
                }

                loadRegionalGuide(
                    query = snapshot.displayText(),
                    region = selectionResult.region,
                    preferredTargetRegionName = snapshot.targetRegionName,
                    preferredManagementZoneName = snapshot.managementZoneName,
                    favoriteKey = snapshot.key,
                    emptyContext = RegionalGuideEmptyContext.FAVORITE_RESTORE,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                clearGuideCandidateBackStack()
                _uiState.value = RegionalGuideUiState.Error(
                    query = "",
                    message = e.message ?: "저장된 지역 가이드를 불러오는 중 오류가 발생했습니다."
                )
            }
        }
    }

    private fun loadSidoOptions() {
        viewModelScope.launch {
            val sidoOptions = getSidoOptionsUseCase()

            _regionSelectorUiState.update { state ->
                state.copy(
                    sidoOptions = sidoOptions
                )
            }
        }
    }

    private fun clearStaleCandidateState(keyword: String) {
        val trimmedKeyword = keyword.trim()
        val candidateQuery = when (val state = uiState.value) {
            is RegionalGuideUiState.Ambiguous -> state.query
            is RegionalGuideUiState.GuideCandidates -> state.query
            else -> return
        }

        if (candidateQuery != trimmedKeyword) {
            _uiState.value = RegionalGuideUiState.Idle
        }
    }

    private fun scheduleKeywordSuggestion(keyword: String) {
        keywordSuggestionJob?.cancel()

        val trimmedKeyword = keyword.trim()
        if (trimmedKeyword.isBlank()) return

        keywordSuggestionJob = viewModelScope.launch {
            delay(KEYWORD_SUGGESTION_DEBOUNCE_MILLIS)

            if (searchKeyword.value.trim() != trimmedKeyword) return@launch

            try {
                when (val result = resolveRegionFromKeywordUseCase(trimmedKeyword)) {
                    is ResolveRegionFromKeywordResult.Ambiguous -> {
                        if (searchKeyword.value.trim() == trimmedKeyword) {
                            _uiState.value = RegionalGuideUiState.Ambiguous(
                                query = trimmedKeyword,
                                message = AMBIGUOUS_REGION_MESSAGE,
                                candidates = result.candidates
                                    .map { region -> region.toCandidateUiModel() }
                            )
                        }
                    }

                    ResolveRegionFromKeywordResult.NotFound,
                    is ResolveRegionFromKeywordResult.Resolved -> Unit
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // 입력 중 후보 추천은 명시 조회가 아니므로 실패해도 현재 화면 상태를 유지합니다.
            }
        }
    }

    private fun searchBySelectedRegion(
        query: String,
        region: Region,
        syncSearchKeyword: Boolean = false,
        clearCandidateBackStack: Boolean = true,
    ) {
        if (clearCandidateBackStack) {
            clearGuideCandidateBackStack()
        }
        if (syncSearchKeyword) {
            _searchKeyword.value = query
        }
        keywordSuggestionJob?.cancel()
        guideLookupJob?.cancel()
        collapseRegionSelectorDropdowns()

        guideLookupJob = viewModelScope.launch {
            try {
                val selectionResult = normalizeAndApplyRegionSelection(region)
                if (selectionResult.removedEupmyeondong != null) {
                    lastRequest = RegionalGuideRequest.SelectedRegion(
                        query = query,
                        region = selectionResult.region
                    )
                    _uiState.value = unavailableEupmyeondongEmptyState(
                        query = query,
                    )
                    return@launch
                }

                lastRequest = RegionalGuideRequest.SelectedRegion(
                    query = query,
                    region = selectionResult.region
                )

                _uiState.value = RegionalGuideUiState.Loading(
                    query = query,
                    canRestoreCandidates = guideCandidateBackStackEntries.isNotEmpty(),
                )

                loadRegionalGuide(
                    query = query,
                    region = selectionResult.region
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = RegionalGuideUiState.Error(
                    query = query,
                    message = e.message ?: "선택한 지역의 배출 가이드를 조회하는 중 오류가 발생했습니다.",
                    canRestoreCandidates = guideCandidateBackStackEntries.isNotEmpty(),
                )
            }
        }
    }

    private suspend fun normalizeAndApplyRegionSelection(region: Region): RegionSelectionNormalizationResult {
        val regionalGuideRegion = normalizeRegionalGuideDisplayRegionUseCase(region)
        val selectedSido = regionalGuideRegion.sido
        val selectedSigungu = regionalGuideRegion.sigungu
        val selectedEupmyeondong = regionalGuideRegion.eupmyeondong
        val eupmyeondongOptions =
            if (
                !selectedSido.isNullOrBlank() &&
                !selectedSigungu.isNullOrBlank() &&
                !selectedEupmyeondong.isNullOrBlank()
            ) {
                getRegionalGuideEupmyeondongOptionsUseCase(
                    sido = selectedSido,
                    sigungu = selectedSigungu
                )
            } else {
                null
            }
        val selectableRegion =
            if (
                selectedEupmyeondong != null &&
                !eupmyeondongOptions.isNullOrEmpty() &&
                eupmyeondongOptions.none { option ->
                    RegionalGuideEupmyeondongNamePolicy.isSameName(
                        first = selectedEupmyeondong,
                        second = option,
                    )
                }
            ) {
                regionalGuideRegion.copy(eupmyeondong = null)
            } else {
                regionalGuideRegion
            }
        val removedEupmyeondong =
            if (selectableRegion.eupmyeondong != selectedEupmyeondong) {
                selectedEupmyeondong
            } else {
                null
            }

        applyRegionSelection(
            region = selectableRegion,
            preloadedEupmyeondongOptions = eupmyeondongOptions
        )

        return RegionSelectionNormalizationResult(
            region = selectableRegion,
            removedEupmyeondong = removedEupmyeondong,
        )
    }

    private fun clearSelectedRegion() {
        sigunguOptionsJob?.cancel()
        eupmyeondongOptionsJob?.cancel()

        _regionSelectorUiState.update { state ->
            state.copy(
                selectedSido = null,
                selectedSigungu = null,
                selectedEupmyeondong = null,
                sigunguOptions = emptyList(),
                eupmyeondongOptions = emptyList(),
                expandedDropdown = null
            )
        }
    }

    private fun observeRegionalGuideFavoriteState(snapshot: RegionalGuideFavoriteSnapshot) {
        favoriteStateJob?.cancel()
        favoriteStateJob = viewModelScope.launch {
            val favoriteStateFlows = snapshot.compatibleTargetIds.map { targetId ->
                observeFavoriteUseCase(
                    type = FavoriteTargetType.REGIONAL_GUIDE,
                    targetId = targetId,
                )
            }
            val favoriteStateFlow =
                when (favoriteStateFlows.size) {
                    0 -> return@launch
                    1 -> favoriteStateFlows.single()
                    else -> combine(favoriteStateFlows) { favoriteStates ->
                        favoriteStates.any { isFavorite -> isFavorite }
                    }
                }

            favoriteStateFlow.collect { isFavorite ->
                _uiState.update { state ->
                    if (
                        state is RegionalGuideUiState.Success &&
                        currentRegionalGuideFavoriteSnapshot?.targetId == snapshot.targetId
                    ) {
                        state.copy(isFavorite = isFavorite)
                    } else {
                        state
                    }
                }
            }
        }
    }

    private fun applyRegionSelection(
        region: Region,
        preloadedEupmyeondongOptions: List<String>? = null,
    ) {
        val selectedSido = region.sido
        val selectedSigungu = region.sigungu

        sigunguOptionsJob?.cancel()
        eupmyeondongOptionsJob?.cancel()

        _regionSelectorUiState.update { state ->
            state.copy(
                selectedSido = selectedSido,
                selectedSigungu = selectedSigungu,
                selectedEupmyeondong = region.eupmyeondong,
                sigunguOptions = emptyList(),
                eupmyeondongOptions = preloadedEupmyeondongOptions.orEmpty(),
                expandedDropdown = null
            )
        }

        if (!selectedSido.isNullOrBlank()) {
            sigunguOptionsJob = viewModelScope.launch {
                val sigunguOptions = getSigunguOptionsUseCase(selectedSido)

                _regionSelectorUiState.update { state ->
                    if (state.selectedSido == selectedSido) {
                        state.copy(sigunguOptions = sigunguOptions)
                    } else {
                        state
                    }
                }
            }
        }

        if (
            !selectedSido.isNullOrBlank() &&
            !selectedSigungu.isNullOrBlank() &&
            preloadedEupmyeondongOptions == null
        ) {
            eupmyeondongOptionsJob = viewModelScope.launch {
                val eupmyeondongOptions = getRegionalGuideEupmyeondongOptionsUseCase(
                    sido = selectedSido,
                    sigungu = selectedSigungu
                )

                _regionSelectorUiState.update { state ->
                    if (state.selectedSido == selectedSido && state.selectedSigungu == selectedSigungu) {
                        state.copy(eupmyeondongOptions = eupmyeondongOptions)
                    } else {
                        state
                    }
                }
            }
        }
    }

    private suspend fun loadRegionalGuide(
        query: String,
        region: Region,
        preferredTargetRegionName: String? = null,
        preferredManagementZoneName: String? = null,
        favoriteKey: RegionalGuideFavoriteKey? = null,
        emptyContext: RegionalGuideEmptyContext = RegionalGuideEmptyContext.DEFAULT,
    ) {
        val result = getRegionalDisposalGuideUseCase(
            region = region,
            preferredTargetRegionName = preferredTargetRegionName,
            preferredManagementZoneName = preferredManagementZoneName,
            favoriteKey = favoriteKey,
        )

        _uiState.value = result.toUiState(
            query = query,
            emptyContext = emptyContext,
        )
    }

    private fun RegionalGuideLookupResult.toUiState(
        query: String,
        emptyContext: RegionalGuideEmptyContext,
    ): RegionalGuideUiState {
        return when (this) {
            is RegionalGuideLookupResult.Success -> {
                val snapshot = guide.toFavoriteSnapshot()
                currentRegionalGuideFavoriteSnapshot = snapshot
                observeRegionalGuideFavoriteState(snapshot)

                RegionalGuideUiState.Success(
                    query = query,
                    guide = guide.toUiModel(),
                    canRestoreCandidates = guideCandidateBackStackEntries.isNotEmpty(),
                )
            }

            is RegionalGuideLookupResult.Candidates -> RegionalGuideUiState.GuideCandidates(
                query = query,
                reason = toUiCandidateReason(
                    lookupReason = reason,
                    emptyContext = emptyContext,
                ),
                canRestoreCandidates = guideCandidateBackStackEntries.isNotEmpty(),
                candidates = guides.map { guide ->
                    RegionalGuideCandidateUiModel(
                        guide = guide.toUiModel(),
                        sido = guide.region.sido,
                        sigungu = guide.region.sigungu,
                        eupmyeondong = guide.region.eupmyeondong
                    )
                }
                    .withDuplicateDisplayDisambiguation()
                    .sortedWith(regionalGuideCandidateDisplayComparator)
            )

            RegionalGuideLookupResult.NotFound -> {
                clearGuideCandidateBackStack()
                when (emptyContext) {
                    RegionalGuideEmptyContext.FAVORITE_RESTORE ->
                        favoriteRestoreFailedEmptyState(query)

                    RegionalGuideEmptyContext.DEFAULT ->
                        RegionalGuideUiState.Empty(
                            query = query,
                            titleResId = R.string.regional_guide_empty_info_not_found_title,
                            messageResId = R.string.regional_guide_empty_info_not_found_message,
                            action = selectRegionAction()
                        )
                }
            }

            RegionalGuideLookupResult.CandidateNotFound -> {
                clearGuideCandidateBackStack()
                when (emptyContext) {
                    RegionalGuideEmptyContext.FAVORITE_RESTORE ->
                        favoriteRestoreFailedEmptyState(query)

                    RegionalGuideEmptyContext.DEFAULT ->
                        RegionalGuideUiState.Empty(
                            query = query,
                            titleResId = R.string.regional_guide_empty_candidate_not_found_title,
                            messageResId = R.string.regional_guide_empty_candidate_not_found_message,
                            action = selectRegionAction()
                        )
                }
            }

            is RegionalGuideLookupResult.Failure -> {
                RegionalGuideUiState.Error(
                    query = query,
                    message = reason.toErrorMessage(),
                    canRestoreCandidates = guideCandidateBackStackEntries.isNotEmpty(),
                )
            }
        }
    }

    private fun toUiCandidateReason(
        lookupReason: RegionalGuideCandidateLookupReason,
        emptyContext: RegionalGuideEmptyContext,
    ): RegionalGuideCandidateReason =
        emptyContext.favoriteRestoreCandidateReason()
            ?: lookupReason.toDefaultUiCandidateReason()

    private fun RegionalGuideEmptyContext.favoriteRestoreCandidateReason(): RegionalGuideCandidateReason? =
        when (this) {
            RegionalGuideEmptyContext.FAVORITE_RESTORE ->
                RegionalGuideCandidateReason.FAVORITE_RESTORE_AMBIGUOUS

            RegionalGuideEmptyContext.DEFAULT -> null
        }

    private fun RegionalGuideCandidateLookupReason.toDefaultUiCandidateReason(): RegionalGuideCandidateReason =
        when (this) {
            RegionalGuideCandidateLookupReason.MULTIPLE_CANDIDATES ->
                RegionalGuideCandidateReason.MULTIPLE_CANDIDATES

            RegionalGuideCandidateLookupReason.MULTIPLE_EXACT_MATCHES ->
                RegionalGuideCandidateReason.MULTIPLE_EXACT_MATCHES

            RegionalGuideCandidateLookupReason.FALLBACK_BECAUSE_DIRECT_MATCH_NOT_FOUND ->
                RegionalGuideCandidateReason.FALLBACK_BECAUSE_DIRECT_MATCH_NOT_FOUND
        }

    private fun RegionalGuideFailureReason.toErrorMessage(): String {
        return when (this) {
            RegionalGuideFailureReason.NETWORK ->
                "네트워크 연결을 확인한 뒤 다시 시도해주세요."

            RegionalGuideFailureReason.API ->
                "지역별 배출 가이드 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요."

            RegionalGuideFailureReason.UNKNOWN ->
                "지역별 배출 가이드를 조회하는 중 오류가 발생했습니다."
        }
    }

    private fun searchAgainAction(): RegionalGuideEmptyActionUiModel =
        RegionalGuideEmptyActionUiModel(
            type = RegionalGuideEmptyActionType.SEARCH_AGAIN,
            labelResId = R.string.regional_guide_empty_action_search_again
        )

    private fun selectRegionAction(): RegionalGuideEmptyActionUiModel =
        RegionalGuideEmptyActionUiModel(
            type = RegionalGuideEmptyActionType.SELECT_REGION,
            labelResId = R.string.regional_guide_empty_action_select_region
        )

    private fun favoriteRestoreFailedEmptyState(query: String): RegionalGuideUiState.Empty =
        RegionalGuideUiState.Empty(
            query = query,
            titleResId = R.string.regional_guide_empty_favorite_restore_failed_title,
            messageResId = R.string.regional_guide_empty_favorite_restore_failed_message,
            action = selectRegionAction()
        )

    private fun unavailableEupmyeondongEmptyState(
        query: String,
    ): RegionalGuideUiState.Empty =
        RegionalGuideUiState.Empty(
            query = query,
            titleResId = R.string.regional_guide_empty_unavailable_eupmyeondong_title,
            messageResId = R.string.regional_guide_empty_unavailable_eupmyeondong_message,
            action = selectRegionAction(),
        )

    private fun Region.toCandidateUiModel(): RegionSearchCandidateUiModel =
        RegionSearchCandidateUiModel(
            sido = sido,
            sigungu = sigungu,
            eupmyeondong = eupmyeondong
        )

    private fun RegionSearchCandidateUiModel.toRegion(): Region =
        Region(
            sido = sido,
            sigungu = sigungu,
            eupmyeondong = eupmyeondong
        )

    private fun RegionalGuideCandidateUiModel.toRegion(): Region =
        Region(
            sido = sido,
            sigungu = sigungu,
            eupmyeondong = eupmyeondong
        )

    private fun RegionalGuideCandidateUiModel.toFavoriteSnapshot(): RegionalGuideFavoriteSnapshot {
        val region = toRegion()

        return RegionalGuideFavoriteSnapshot(
            targetId = RegionalGuideFavoriteKey(
                sido = region.sido,
                sigungu = region.sigungu,
                eupmyeondong = region.eupmyeondong,
                targetRegionName = guide.targetRegionName,
                managementZoneName = guide.managementZoneName,
            ).encode(),
            region = region,
            targetRegionName = guide.targetRegionName,
            managementZoneName = guide.managementZoneName,
        )
    }

    private fun RegionalDisposalGuide.toFavoriteSnapshot(): RegionalGuideFavoriteSnapshot {
        return RegionalGuideFavoriteSnapshot(
            targetId = RegionalGuideFavoriteKey(
                sido = region.sido,
                sigungu = region.sigungu,
                eupmyeondong = region.eupmyeondong,
                targetRegionName = targetRegionName,
                managementZoneName = managementZoneName,
            ).encode(),
            region = region,
            targetRegionName = targetRegionName?.trim()?.takeIf { it.isNotBlank() },
            managementZoneName = managementZoneName?.trim()?.takeIf { it.isNotBlank() },
        )
    }

    private fun RegionalGuideFavoriteSnapshot.displayText(): String =
        listOfNotNull(
            region.sido,
            region.sigungu,
            region.eupmyeondong,
        )
            .filter { regionName -> regionName.isNotBlank() }
            .joinToString(" > ")
            .ifBlank { targetRegionName ?: managementZoneName ?: "" }

    private fun collapseRegionSelectorDropdowns() {
        _regionSelectorUiState.update { state ->
            if (state.expandedDropdown == null) {
                state
            } else {
                state.copy(expandedDropdown = null)
            }
        }
    }

    private fun dismissSearchCandidateState() {
        keywordSuggestionJob?.cancel()

        val shouldDismissCandidates = when (uiState.value) {
            is RegionalGuideUiState.Ambiguous,
            is RegionalGuideUiState.GuideCandidates -> true

            else -> false
        }

        if (shouldDismissCandidates) {
            clearGuideCandidateBackStack()
            _uiState.value = RegionalGuideUiState.Idle
        }
    }

    private fun clearGuideCandidateBackStack() {
        guideCandidateBackStackEntries.clear()
        _uiState.update { state ->
            when (state) {
                is RegionalGuideUiState.Success ->
                    state.takeUnless { it.canRestoreCandidates }
                        ?: state.copy(canRestoreCandidates = false)

                is RegionalGuideUiState.GuideCandidates ->
                    state.takeUnless { it.canRestoreCandidates }
                        ?: state.copy(canRestoreCandidates = false)

                is RegionalGuideUiState.Error ->
                    state.takeUnless { it.canRestoreCandidates }
                        ?: state.copy(canRestoreCandidates = false)

                else -> state
            }
        }
    }

    private fun pushGuideCandidateBackStackEntry(uiState: RegionalGuideUiState) {
        guideCandidateBackStackEntries += RegionalGuideCandidateBackStackEntry(
            uiState = uiState,
            searchKeyword = searchKeyword.value,
            regionSelectorUiState = regionSelectorUiState.value,
            lastRequest = lastRequest,
        )
    }

    private sealed interface RegionalGuideRequest {
        data class Keyword(
            val keyword: String
        ) : RegionalGuideRequest

        data class Address(
            val address: String
        ) : RegionalGuideRequest

        data class Favorite(
            val targetId: String
        ) : RegionalGuideRequest

        data class SelectedRegion(
            val query: String,
            val region: Region
        ) : RegionalGuideRequest
    }

    private enum class RegionalGuideEmptyContext {
        DEFAULT,
        FAVORITE_RESTORE,
    }

    private data class RegionSelectionNormalizationResult(
        val region: Region,
        val removedEupmyeondong: String?,
    )

    private data class RegionalGuideCandidateBackStackEntry(
        val uiState: RegionalGuideUiState,
        val searchKeyword: String,
        val regionSelectorUiState: RegionSelectorUiState,
        val lastRequest: RegionalGuideRequest?,
    )

    private companion object {
        const val KEYWORD_SUGGESTION_DEBOUNCE_MILLIS = 400L
        const val AMBIGUOUS_REGION_MESSAGE = "여러 지역이 검색됩니다. 원하는 지역을 선택해주세요."
    }
}

sealed interface RegionalGuideEvent {
    data object FavoriteUpdateFailed : RegionalGuideEvent
}
