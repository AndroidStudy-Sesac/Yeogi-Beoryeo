package com.team.yeogibeoryeo.presentation.regionalguide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.usecase.ExtractRegionFromAddressUseCase
import com.team.yeogibeoryeo.domain.region.usecase.ResolveRegionFromKeywordUseCase
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.usecase.GetRegionalDisposalGuideUseCase
import com.team.yeogibeoryeo.presentation.regionalguide.mapper.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegionalGuideViewModel @Inject constructor(
    private val resolveRegionFromKeywordUseCase: ResolveRegionFromKeywordUseCase,
    private val extractRegionFromAddressUseCase: ExtractRegionFromAddressUseCase,
    private val getRegionalDisposalGuideUseCase: GetRegionalDisposalGuideUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegionalGuideUiState>(RegionalGuideUiState.Idle)
    val uiState: StateFlow<RegionalGuideUiState> = _uiState.asStateFlow()

    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    private var guideLookupJob: Job? = null
    private var lastRequest: RegionalGuideRequest? = null

    fun onSearchKeywordChanged(keyword: String) {
        _searchKeyword.value = keyword
    }

    fun searchCurrentKeyword() {
        searchByKeyword(_searchKeyword.value)
    }

    fun retryLastRequest() {
        when (val request = lastRequest) {
            is RegionalGuideRequest.Keyword -> searchByKeyword(request.keyword)
            is RegionalGuideRequest.Address -> loadByAddress(request.address)
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
                val region = resolveRegionFromKeywordUseCase(trimmedKeyword)

                if (region == null) {
                    _uiState.value = RegionalGuideUiState.Empty(
                        query = trimmedKeyword,
                        message = "입력한 지역명을 찾을 수 없습니다."
                    )
                    return@launch
                }

                loadRegionalGuide(
                    query = trimmedKeyword,
                    region = region
                )
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

    /**
     * 지도 화면 또는 수거 장소 상세에서 전달받은 addrBase 기반으로
     * 지역별 배출 가이드를 조회합니다.
     *
     * 실제 화면 이동 및 호출 시점은 Navigation 공통 작업에서 연결합니다.
     */
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

    private suspend fun loadRegionalGuide(
        query: String,
        region: Region
    ) {
        val guide = getRegionalDisposalGuideUseCase(region)

        _uiState.value = guide.toUiState(query)
    }

    private fun RegionalDisposalGuide?.toUiState(
        query: String
    ): RegionalGuideUiState {
        return if (this == null) {
            RegionalGuideUiState.Empty(
                query = query,
                message = "해당 지역의 배출 가이드 정보가 없습니다."
            )
        } else {
            RegionalGuideUiState.Success(
                query = query,
                guide = this.toUiModel()
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
    }
}