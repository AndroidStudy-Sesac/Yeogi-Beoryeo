package com.jeong.apitest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Single Source of Truth: UI State Data Class
data class WasteUiState(
    // UI Navigation & Display State
    val selectedTab: Int = 0,
    val showAdvancedSettings: Boolean = false,

    // Results
    val itemsResult: List<ItemResponse> = emptyList(),
    val spotsResult: List<SpotResponse> = emptyList(),
    val householdResult: List<HouseholdWasteInfo> = emptyList(),

    // Search Metadata
    val totalCount: Int = 0,
    val currentPage: Int = 1,
    val lastSearchType: SearchType? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastDetectedSgg: String? = null,

    // Inputs (SSoT)
    val itemQuery: String = "",
    val regionQuery: String = "",
    val updatedFrom: String = "",
    val updatedUntil: String = "",
    val administrativeCode: String = "",
    val baseDateFrom: String = "",
    val baseDateUntil: String = "",
    val latitudeText: String = "",
    val longitudeText: String = "",
    val radiusValue: Float = 500f,
    val numOfRowsText: String = "20"
)

class WasteViewModel(private val repository: WasteRepository) : ViewModel() {

    // UI State consolidated into a single StateFlow (SSOT)
    private val _uiState = MutableStateFlow(WasteUiState())
    val uiState: StateFlow<WasteUiState> = _uiState.asStateFlow()

    // Update functions for UDF (Unidirectional Data Flow)
    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun toggleAdvancedSettings() {
        _uiState.update { it.copy(showAdvancedSettings = !it.showAdvancedSettings) }
    }

    fun onItemQueryChange(newQuery: String) {
        _uiState.update { it.copy(itemQuery = newQuery) }
    }

    fun onRegionQueryChange(newQuery: String) {
        _uiState.update { it.copy(regionQuery = newQuery) }
    }

    fun onLatitudeChange(newLat: String) {
        _uiState.update { it.copy(latitudeText = newLat) }
    }

    fun onLongitudeChange(newLon: String) {
        _uiState.update { it.copy(longitudeText = newLon) }
    }

    fun onRadiusChange(newRadius: Float) {
        _uiState.update { it.copy(radiusValue = newRadius) }
    }

    fun onAdministrativeCodeChange(newCode: String) {
        _uiState.update { it.copy(administrativeCode = newCode) }
    }

    fun onUpdatedFromChange(newValue: String) {
        _uiState.update { it.copy(updatedFrom = newValue) }
    }

    fun onUpdatedUntilChange(newValue: String) {
        _uiState.update { it.copy(updatedUntil = newValue) }
    }

    fun onBaseDateFromChange(newValue: String) {
        _uiState.update { it.copy(baseDateFrom = newValue) }
    }

    fun onBaseDateUntilChange(newValue: String) {
        _uiState.update { it.copy(baseDateUntil = newValue) }
    }

    fun onNumOfRowsChange(newValue: String) {
        _uiState.update { it.copy(numOfRowsText = newValue) }
    }

    fun search(type: SearchType) {
        _uiState.update { it.copy(lastSearchType = type) }
        loadMore(isInitial = true)
    }

    fun loadMore(isInitial: Boolean = false) {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        val currentSize = when (currentState.lastSearchType) {
            SearchType.ITEM -> currentState.itemsResult.size
            SearchType.SPOT -> currentState.spotsResult.size
            SearchType.HOUSEHOLD_INFO -> currentState.householdResult.size
            null -> 0
        }

        if (!isInitial && (currentState.totalCount in 1..currentSize)) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    currentPage = if (isInitial) 1 else it.currentPage + 1,
                    itemsResult = if (isInitial) emptyList() else it.itemsResult,
                    spotsResult = if (isInitial) emptyList() else it.spotsResult,
                    householdResult = if (isInitial) emptyList() else it.householdResult,
                    totalCount = if (isInitial) 0 else it.totalCount
                )
            }

            try {
                val state = _uiState.value
                val rows = state.numOfRowsText.toIntOrNull() ?: 20
                when (state.lastSearchType) {
                    SearchType.ITEM -> {
                        val response =
                            repository.getItem(state.currentPage, rows, state.itemQuery)
                        val newList = response.response.body.items?.item ?: emptyList()
                        _uiState.update {
                            it.copy(
                                itemsResult = if (isInitial) newList else it.itemsResult + newList,
                                totalCount = response.response.body.totalCount
                            )
                        }
                    }

                    SearchType.SPOT -> {
                        val response = repository.getSpot(
                            state.currentPage,
                            rows,
                            state.regionQuery,
                            state.latitudeText.toDoubleOrNull(),
                            state.longitudeText.toDoubleOrNull(),
                            state.radiusValue.toInt()
                        )
                        val newList = response.response.body.items?.item ?: emptyList()

                        var detectedSgg = state.lastDetectedSgg
                        if (isInitial && newList.isNotEmpty()) {
                            detectedSgg = repository.extractSggFromAddress(newList.first().addrBase)
                                ?: state.lastDetectedSgg
                        }

                        _uiState.update {
                            it.copy(
                                spotsResult = if (isInitial) newList else it.spotsResult + newList,
                                totalCount = response.response.body.totalCount,
                                lastDetectedSgg = detectedSgg
                            )
                        }
                    }

                    SearchType.HOUSEHOLD_INFO -> {
                        val trimmedQuery = state.regionQuery.trim()
                        val isInputSgg =
                            trimmedQuery.endsWith("시") || trimmedQuery.endsWith("군") || trimmedQuery.endsWith(
                                "구"
                            )
                        val isInputSubRegion =
                            trimmedQuery.endsWith("동") || trimmedQuery.endsWith("가") || trimmedQuery.endsWith(
                                "로"
                            ) ||
                                    trimmedQuery.endsWith("읍") || trimmedQuery.endsWith("면") || trimmedQuery.endsWith(
                                "리"
                            )

                        val finalSgg = when {
                            isInputSgg -> trimmedQuery
                            !state.lastDetectedSgg.isNullOrBlank() -> state.lastDetectedSgg
                            isInputSubRegion -> null
                            else -> trimmedQuery.ifBlank { null }
                        }

                        val response = repository.getHouseholdInfo(
                            state.currentPage, rows,
                            sggNameQuery = finalSgg,
                            updatedFrom = state.updatedFrom.ifBlank { null },
                            updatedUntil = state.updatedUntil.ifBlank { null },
                            administrativeCode = state.administrativeCode.ifBlank { null },
                            baseDateFrom = state.baseDateFrom.ifBlank { null },
                            baseDateUntil = state.baseDateUntil.ifBlank { null }
                        )
                        val newList = response.response.body.items?.item ?: emptyList()

                        _uiState.update {
                            it.copy(
                                householdResult = if (isInitial) newList else it.householdResult + newList,
                                totalCount = response.response.body.totalCount
                            )
                        }
                    }

                    null -> {}
                }
                if (isInitial && _uiState.value.totalCount == 0) {
                    _uiState.update { it.copy(errorMessage = "조회 결과가 없습니다. 검색어를 확인해 주세요.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
