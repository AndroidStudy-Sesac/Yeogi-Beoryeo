package com.team.yeogibeoryeo.presentation.regionalguide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.model.toFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.usecase.GetRegionalGuideFavoriteSnapshotUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoriteUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ToggleRegionalGuideFavoriteUseCase
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.usecase.ExtractRegionFromAddressUseCase
import com.team.yeogibeoryeo.domain.region.usecase.GetEupmyeondongOptionsUseCase
import com.team.yeogibeoryeo.domain.region.usecase.GetSidoOptionsUseCase
import com.team.yeogibeoryeo.domain.region.usecase.GetSigunguOptionsUseCase
import com.team.yeogibeoryeo.domain.region.usecase.NormalizeRegionForRegionalGuideUseCase
import com.team.yeogibeoryeo.domain.region.usecase.ResolveRegionFromKeywordUseCase
import com.team.yeogibeoryeo.domain.region.usecase.ResolveRegionFromKeywordResult
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideFailureReason
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupResult
import com.team.yeogibeoryeo.domain.regionalguide.usecase.GetRegionalDisposalGuideUseCase
import com.team.yeogibeoryeo.presentation.regionalguide.mapper.toUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideCandidateUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionSearchCandidateUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RegionalGuideViewModel @Inject constructor(
    private val resolveRegionFromKeywordUseCase: ResolveRegionFromKeywordUseCase,
    private val extractRegionFromAddressUseCase: ExtractRegionFromAddressUseCase,
    private val getRegionalDisposalGuideUseCase: GetRegionalDisposalGuideUseCase,
    private val getSidoOptionsUseCase: GetSidoOptionsUseCase,
    private val getSigunguOptionsUseCase: GetSigunguOptionsUseCase,
    private val getEupmyeondongOptionsUseCase: GetEupmyeondongOptionsUseCase,
    private val normalizeRegionForRegionalGuideUseCase: NormalizeRegionForRegionalGuideUseCase,
    private val observeFavoriteUseCase: ObserveFavoriteUseCase,
    private val toggleRegionalGuideFavoriteUseCase: ToggleRegionalGuideFavoriteUseCase,
    private val getRegionalGuideFavoriteSnapshotUseCase: GetRegionalGuideFavoriteSnapshotUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegionalGuideUiState>(RegionalGuideUiState.Idle)
    val uiState: StateFlow<RegionalGuideUiState> = _uiState.asStateFlow()

    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    private val _regionSelectorUiState = MutableStateFlow(RegionSelectorUiState())
    val regionSelectorUiState: StateFlow<RegionSelectorUiState> =
        _regionSelectorUiState.asStateFlow()

    private var guideLookupJob: Job? = null
    private var keywordSuggestionJob: Job? = null
    private var sigunguOptionsJob: Job? = null
    private var eupmyeondongOptionsJob: Job? = null
    private var favoriteStateJob: Job? = null
    private var lastRequest: RegionalGuideRequest? = null
    private var currentRegionalGuideFavoriteSnapshot: RegionalGuideFavoriteSnapshot? = null

    init {
        loadSidoOptions()
    }

    fun onSearchKeywordChanged(keyword: String) {
        _searchKeyword.value = keyword
        clearStaleCandidateState(keyword)
        scheduleKeywordSuggestion(keyword)
    }

    fun onSidoSelected(sido: String) {
        sigunguOptionsJob?.cancel()
        eupmyeondongOptionsJob?.cancel()

        _regionSelectorUiState.update { state ->
            state.copy(
                selectedSido = sido,
                selectedSigungu = null,
                selectedEupmyeondong = null,
                sigunguOptions = emptyList(),
                eupmyeondongOptions = emptyList(),
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
        eupmyeondongOptionsJob?.cancel()

        val selectedSido = regionSelectorUiState.value.selectedSido ?: return

        _regionSelectorUiState.update { state ->
            state.copy(
                selectedSigungu = sigungu,
                selectedEupmyeondong = null,
                eupmyeondongOptions = emptyList(),
                expandedDropdown = null
            )
        }

        eupmyeondongOptionsJob = viewModelScope.launch {
            val eupmyeondongOptions = getEupmyeondongOptionsUseCase(
                sido = selectedSido,
                sigungu = sigungu
            )

            _regionSelectorUiState.update { state ->
                if (state.selectedSido == selectedSido && state.selectedSigungu == sigungu) {
                    state.copy(eupmyeondongOptions = eupmyeondongOptions)
                } else {
                    state
                }
            }
        }
    }

    fun onEupmyeondongSelected(eupmyeondong: String) {
        _regionSelectorUiState.update { state ->
            state.copy(
                selectedEupmyeondong = eupmyeondong,
                expandedDropdown = null
            )
        }
    }

    fun onRegionSelectorDropdownExpanded(dropdown: RegionSelectorDropdown) {
        _regionSelectorUiState.update { state ->
            state.copy(expandedDropdown = dropdown)
        }
    }

    fun onRegionSelectorDropdownDismissed() {
        collapseRegionSelectorDropdowns()
    }

    fun onRegionSelectionSearchClick() {
        collapseRegionSelectorDropdowns()

        val state = regionSelectorUiState.value
        val selectedSido = state.selectedSido
        val selectedSigungu = state.selectedSigungu

        if (selectedSido.isNullOrBlank() || selectedSigungu.isNullOrBlank()) {
            _uiState.value = RegionalGuideUiState.Empty(
                query = state.selectedRegionText.orEmpty(),
                message = "시도와 시군구를 선택해주세요."
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
            region = region
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

    fun onRegionCandidateSelected(candidate: RegionSearchCandidateUiModel) {
        if (!candidate.isValid) return

        val region = candidate.toRegion()
        searchBySelectedRegion(
            query = candidate.displayText,
            region = region
        )
    }

    fun onRegionalGuideCandidateSelected(candidate: RegionalGuideCandidateUiModel) {
        keywordSuggestionJob?.cancel()

        val query = (uiState.value as? RegionalGuideUiState.GuideCandidates)
            ?.query
            ?: candidate.displayText

        applyRegionSelection(candidate.toRegion())

        val snapshot = candidate.toFavoriteSnapshot()
        currentRegionalGuideFavoriteSnapshot = snapshot
        observeRegionalGuideFavoriteState(snapshot)

        _uiState.value = RegionalGuideUiState.Success(
            query = query,
            guide = candidate.guide
        )
    }

    fun onFavoriteClick() {
        val snapshot = currentRegionalGuideFavoriteSnapshot ?: return

        viewModelScope.launch {
            val isFavorite = toggleRegionalGuideFavoriteUseCase(snapshot)

            _uiState.update { state ->
                if (state is RegionalGuideUiState.Success) {
                    state.copy(isFavorite = isFavorite)
                } else {
                    state
                }
            }
        }
    }

    fun searchByKeyword(keyword: String) {
        val trimmedKeyword = keyword.trim()

        _searchKeyword.value = keyword
        keywordSuggestionJob?.cancel()
        guideLookupJob?.cancel()
        collapseRegionSelectorDropdowns()

        if (trimmedKeyword.isBlank()) {
            _uiState.value = RegionalGuideUiState.Empty(
                query = trimmedKeyword,
                message = "검색할 지역명을 입력해주세요."
            )
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
                            message = "입력한 지역명을 찾을 수 없습니다."
                        )
                    }

                    is ResolveRegionFromKeywordResult.Resolved -> {
                        val regionalGuideRegion = normalizeAndApplyRegionSelection(result.region)
                        loadRegionalGuide(
                            query = trimmedKeyword,
                            region = regionalGuideRegion
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = RegionalGuideUiState.Error(
                    query = trimmedKeyword,
                    message = e.message ?: "지역별 배출 가이드를 조회하는 중 오류가 발생했습니다."
                )
            }
        }
    }

    fun loadByAddress(address: String) {
        val trimmedAddress = address.trim()

        keywordSuggestionJob?.cancel()
        guideLookupJob?.cancel()

        if (trimmedAddress.isBlank()) {
            _uiState.value = RegionalGuideUiState.Empty(
                query = trimmedAddress,
                message = "주소 정보가 없습니다."
            )
            return
        }

        lastRequest = RegionalGuideRequest.Address(trimmedAddress)

        guideLookupJob = viewModelScope.launch {
            _uiState.value = RegionalGuideUiState.Loading(query = trimmedAddress)

            try {
                val region = extractRegionFromAddressUseCase(trimmedAddress)

                if (region == null) {
                    _uiState.value = RegionalGuideUiState.Empty(
                        query = trimmedAddress,
                        message = "주소에서 지역 정보를 찾을 수 없습니다."
                    )
                    return@launch
                }

                val regionalGuideRegion = normalizeAndApplyRegionSelection(region)
                loadRegionalGuide(
                    query = trimmedAddress,
                    region = regionalGuideRegion
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = RegionalGuideUiState.Error(
                    query = trimmedAddress,
                    message = e.message ?: "주소 기반 지역별 배출 가이드를 조회하는 중 오류가 발생했습니다."
                )
            }
        }
    }

    fun loadByFavoriteTargetId(targetId: String) {
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
                        message = "저장된 지역 가이드를 찾을 수 없습니다."
                    )
                    return@launch
                }

                val regionalGuideRegion = normalizeAndApplyRegionSelection(snapshot.region)
                loadRegionalGuide(
                    query = snapshot.displayText(),
                    region = regionalGuideRegion,
                    preferredTargetRegionName = snapshot.targetRegionName,
                    preferredManagementZoneName = snapshot.managementZoneName,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = RegionalGuideUiState.Error(
                    query = "",
                    message = e.message ?: "저장된 지역 가이드를 불러오는 중 오류가 발생했습니다."
                )
            }
        }
    }

    fun resetState() {
        guideLookupJob?.cancel()
        keywordSuggestionJob?.cancel()
        sigunguOptionsJob?.cancel()
        eupmyeondongOptionsJob?.cancel()
        favoriteStateJob?.cancel()
        lastRequest = null
        currentRegionalGuideFavoriteSnapshot = null
        _uiState.value = RegionalGuideUiState.Idle
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
        region: Region
    ) {
        keywordSuggestionJob?.cancel()
        guideLookupJob?.cancel()
        collapseRegionSelectorDropdowns()

        guideLookupJob = viewModelScope.launch {
            try {
                val regionalGuideRegion = normalizeAndApplyRegionSelection(region)

                lastRequest = RegionalGuideRequest.SelectedRegion(
                    query = query,
                    region = regionalGuideRegion
                )

                _uiState.value = RegionalGuideUiState.Loading(query = query)

                loadRegionalGuide(
                    query = query,
                    region = regionalGuideRegion
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = RegionalGuideUiState.Error(
                    query = query,
                    message = e.message ?: "선택한 지역의 배출 가이드를 조회하는 중 오류가 발생했습니다."
                )
            }
        }
    }

    private suspend fun normalizeAndApplyRegionSelection(region: Region): Region {
        val regionalGuideRegion = normalizeRegionForRegionalGuideUseCase(region)

        applyRegionSelection(regionalGuideRegion)

        return regionalGuideRegion
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

    private fun applyRegionSelection(region: Region) {
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
                eupmyeondongOptions = emptyList(),
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

        if (!selectedSido.isNullOrBlank() && !selectedSigungu.isNullOrBlank()) {
            eupmyeondongOptionsJob = viewModelScope.launch {
                val eupmyeondongOptions = getEupmyeondongOptionsUseCase(
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
    ) {
        val result = getRegionalDisposalGuideUseCase(
            region = region,
            preferredTargetRegionName = preferredTargetRegionName,
            preferredManagementZoneName = preferredManagementZoneName,
        )

        _uiState.value = result.toUiState(query)
    }

    private fun RegionalGuideLookupResult.toUiState(
        query: String
    ): RegionalGuideUiState {
        return when (this) {
            is RegionalGuideLookupResult.Success -> {
                val snapshot = guide.toFavoriteSnapshot()
                currentRegionalGuideFavoriteSnapshot = snapshot
                observeRegionalGuideFavoriteState(snapshot)

                RegionalGuideUiState.Success(
                    query = query,
                    guide = guide.toUiModel()
                )
            }

            is RegionalGuideLookupResult.Candidates -> RegionalGuideUiState.GuideCandidates(
                query = query,
                message = "여러 배출 권역이 검색됩니다. 해당하는 권역을 선택해주세요.",
                candidates = guides.map { guide ->
                    RegionalGuideCandidateUiModel(
                        guide = guide.toUiModel(),
                        sido = guide.region.sido,
                        sigungu = guide.region.sigungu,
                        eupmyeondong = guide.region.eupmyeondong
                    )
                }
            )

            RegionalGuideLookupResult.NotFound -> RegionalGuideUiState.Empty(
                query = query,
                message = "해당 지역의 배출 가이드 정보가 없습니다."
            )

            RegionalGuideLookupResult.CandidateNotFound -> RegionalGuideUiState.Empty(
                query = query,
                message = "선택한 지역에 맞는 배출 가이드를 찾을 수 없습니다."
            )

            is RegionalGuideLookupResult.Failure -> RegionalGuideUiState.Error(
                query = query,
                message = reason.toErrorMessage()
            )
        }
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
            targetId = com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteKey(
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

    private companion object {
        const val KEYWORD_SUGGESTION_DEBOUNCE_MILLIS = 400L
        const val AMBIGUOUS_REGION_MESSAGE = "여러 지역이 검색됩니다. 원하는 지역을 선택해주세요."
    }
}
