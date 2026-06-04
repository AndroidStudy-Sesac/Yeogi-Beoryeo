package com.team.yeogibeoryeo.presentation.regionalguide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.usecase.ExtractRegionFromAddressUseCase
import com.team.yeogibeoryeo.domain.region.usecase.GetEupmyeondongOptionsUseCase
import com.team.yeogibeoryeo.domain.region.usecase.GetSidoOptionsUseCase
import com.team.yeogibeoryeo.domain.region.usecase.GetSigunguOptionsUseCase
import com.team.yeogibeoryeo.domain.region.usecase.ResolveRegionFromKeywordUseCase
import com.team.yeogibeoryeo.domain.region.usecase.ResolveRegionFromKeywordResult
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupResult
import com.team.yeogibeoryeo.domain.regionalguide.usecase.GetRegionalDisposalGuideUseCase
import com.team.yeogibeoryeo.presentation.regionalguide.mapper.toUiModel
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
class RegionalGuideViewModel @Inject constructor(
    private val resolveRegionFromKeywordUseCase: ResolveRegionFromKeywordUseCase,
    private val extractRegionFromAddressUseCase: ExtractRegionFromAddressUseCase,
    private val getRegionalDisposalGuideUseCase: GetRegionalDisposalGuideUseCase,
    private val getSidoOptionsUseCase: GetSidoOptionsUseCase,
    private val getSigunguOptionsUseCase: GetSigunguOptionsUseCase,
    private val getEupmyeondongOptionsUseCase: GetEupmyeondongOptionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegionalGuideUiState>(RegionalGuideUiState.Idle)
    val uiState: StateFlow<RegionalGuideUiState> = _uiState.asStateFlow()

    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    private val _regionSelectorUiState = MutableStateFlow(RegionSelectorUiState())
    val regionSelectorUiState: StateFlow<RegionSelectorUiState> =
        _regionSelectorUiState.asStateFlow()

    private var guideLookupJob: Job? = null
    private var lastRequest: RegionalGuideRequest? = null

    init {
        loadSidoOptions()
    }

    fun onSearchKeywordChanged(keyword: String) {
        _searchKeyword.value = keyword
    }

    fun onSidoSelected(sido: String) {
        viewModelScope.launch {
            val sigunguOptions = getSigunguOptionsUseCase(sido)

            _regionSelectorUiState.update { state ->
                state.copy(
                    selectedSido = sido,
                    selectedSigungu = null,
                    selectedEupmyeondong = null,
                    sigunguOptions = sigunguOptions,
                    eupmyeondongOptions = emptyList()
                )
            }
        }
    }

    fun onSigunguSelected(sigungu: String) {
        viewModelScope.launch {
            val selectedSido = regionSelectorUiState.value.selectedSido ?: return@launch
            val eupmyeondongOptions = getEupmyeondongOptionsUseCase(
                sido = selectedSido,
                sigungu = sigungu
            )

            _regionSelectorUiState.update { state ->
                state.copy(
                    selectedSigungu = sigungu,
                    selectedEupmyeondong = null,
                    eupmyeondongOptions = eupmyeondongOptions
                )
            }
        }
    }

    fun onEupmyeondongSelected(eupmyeondong: String) {
        _regionSelectorUiState.update { state ->
            state.copy(
                selectedEupmyeondong = eupmyeondong
            )
        }
    }

    fun onRegionSelectionSearchClick() {
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
            is RegionalGuideRequest.SelectedRegion -> searchBySelectedRegion(
                query = request.query,
                region = request.region
            )
            null -> searchCurrentKeyword()
        }
    }

    fun searchByKeyword(keyword: String) {
        val trimmedKeyword = keyword.trim()

        guideLookupJob?.cancel()

        if (trimmedKeyword.isBlank()) {
            _uiState.value = RegionalGuideUiState.Empty(
                query = trimmedKeyword,
                message = "검색할 지역명을 입력해주세요."
            )
            return
        }

        lastRequest = RegionalGuideRequest.Keyword(trimmedKeyword)

        guideLookupJob = viewModelScope.launch {
            _uiState.value = RegionalGuideUiState.Loading(query = trimmedKeyword)

            try {
                when (val result = resolveRegionFromKeywordUseCase(trimmedKeyword)) {
                    ResolveRegionFromKeywordResult.Ambiguous -> {
                        _uiState.value = RegionalGuideUiState.Ambiguous(
                            query = trimmedKeyword,
                            message = "여러 지역이 검색됩니다. 시도나 시군구를 함께 입력해주세요."
                        )
                    }

                    ResolveRegionFromKeywordResult.NotFound -> {
                        _uiState.value = RegionalGuideUiState.Empty(
                            query = trimmedKeyword,
                            message = "입력한 지역명을 찾을 수 없습니다."
                        )
                    }

                    is ResolveRegionFromKeywordResult.Resolved -> {
                        loadRegionalGuide(
                            query = trimmedKeyword,
                            region = result.region
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

                loadRegionalGuide(
                    query = trimmedAddress,
                    region = region
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

    fun resetState() {
        guideLookupJob?.cancel()
        lastRequest = null
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

    private fun searchBySelectedRegion(
        query: String,
        region: Region
    ) {
        guideLookupJob?.cancel()

        lastRequest = RegionalGuideRequest.SelectedRegion(
            query = query,
            region = region
        )

        guideLookupJob = viewModelScope.launch {
            _uiState.value = RegionalGuideUiState.Loading(query = query)

            try {
                loadRegionalGuide(
                    query = query,
                    region = region
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

    private suspend fun loadRegionalGuide(
        query: String,
        region: Region
    ) {
        val result = getRegionalDisposalGuideUseCase(region)

        _uiState.value = result.toUiState(query)
    }

    private fun RegionalGuideLookupResult.toUiState(
        query: String
    ): RegionalGuideUiState {
        return when (this) {
            is RegionalGuideLookupResult.Success -> RegionalGuideUiState.Success(
                query = query,
                guide = guide.toUiModel()
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
                message = throwable?.message ?: "지역별 배출 가이드를 조회하는 중 오류가 발생했습니다."
            )
        }
    }

    private sealed interface RegionalGuideRequest {
        data class Keyword(
            val keyword: String
        ) : RegionalGuideRequest

        data class Address(
            val address: String
        ) : RegionalGuideRequest

        data class SelectedRegion(
            val query: String,
            val region: Region
        ) : RegionalGuideRequest
    }
}
