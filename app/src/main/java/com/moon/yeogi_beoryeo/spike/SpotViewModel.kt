package com.moon.yeogi_beoryeo.spike

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moon.yeogi_beoryeo.BuildConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SpikeUiState {
    data object Loading : SpikeUiState
    data class Success(
        val locations: List<SpotBasicItem>,
        val schedules: List<SpotDetailItem>
    ) : SpikeUiState
    data class Error(val message: String) : SpikeUiState
}

class SpotViewModel : ViewModel() {

    private val apiService = SpotApiService.create()
    private val serviceKey = BuildConfig.SPOT_API_KEY

    private val _uiState = MutableStateFlow<SpikeUiState>(SpikeUiState.Loading)
    val uiState: StateFlow<SpikeUiState> = _uiState.asStateFlow()

    fun searchByKeyword(keyword: String) {
        if (keyword.isBlank()) return

        viewModelScope.launch {
            _uiState.value = SpikeUiState.Loading
            runCatching {
                val locationResponse = apiService.getSpotLocations(serviceKey, addr = keyword)
                val locationList = locationResponse.response.body.items?.itemList ?: emptyList()

                val firstAddr = locationList.firstOrNull()?.address ?: ""
                val extractedSggName = firstAddr.split(" ").find { it.endsWith("구") }
                    ?: firstAddr.split(" ").find { it.endsWith("군") }
                    ?: ""

                val sggTarget = extractedSggName.ifEmpty { keyword }

                val scheduleResponse = apiService.getSpotDetails(serviceKey, sggName = sggTarget)
                val scheduleList = scheduleResponse.response.body.items?.itemList ?: emptyList()

                Pair(locationList, scheduleList)
            }.onSuccess { (locations, schedules) ->
                _uiState.value = SpikeUiState.Success(locations, schedules)
                Log.d("SpotSpike", "검색 성공 - 키워드: $keyword, 추출된 구: ${locations.firstOrNull()?.address}")
            }.onFailure { exception ->
                _uiState.value = SpikeUiState.Error(exception.message ?: "검색 결과가 없거나 오류가 발생했습니다.")
                Log.e("SpotSpike", "API 호출 실패", exception)
            }
        }
    }

    fun searchByLocation(sggName: String, dongName: String) {
        viewModelScope.launch {
            _uiState.value = SpikeUiState.Loading
            runCatching {
                val locDeferred = async { apiService.getSpotLocations(serviceKey, addr = dongName) }
                val schDeferred = async { apiService.getSpotDetails(serviceKey, sggName = sggName) }

                val locations = locDeferred.await().response.body.items?.itemList ?: emptyList()
                val schedules = schDeferred.await().response.body.items?.itemList ?: emptyList()

                Pair(locations, schedules)
            }.onSuccess { (loc, sch) ->
                _uiState.value = SpikeUiState.Success(loc, sch)
            }.onFailure {
                _uiState.value = SpikeUiState.Error("데이터 호출에 실패했습니다.")
            }
        }
    }
}