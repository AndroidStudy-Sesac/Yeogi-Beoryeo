package com.team.yeogibeoryeo.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.appguide.repository.AppGuideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapLocationGuideUiState(
    val isReady: Boolean = false,
    val isVisible: Boolean = false,
)

@HiltViewModel
class MapLocationGuideViewModel
    @Inject
    constructor(
        private val repository: AppGuideRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MapLocationGuideUiState())
        val uiState = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val completedVersion = repository.observeCompletedMapLocationGuideVersion().first()
                _uiState.update {
                    it.copy(
                        isReady = true,
                        isVisible = completedVersion < CURRENT_MAP_LOCATION_GUIDE_VERSION,
                    )
                }
            }
        }

        fun dismissGuide() {
            _uiState.update { it.copy(isVisible = false) }
            viewModelScope.launch {
                repository.markMapLocationGuideCompleted(CURRENT_MAP_LOCATION_GUIDE_VERSION)
            }
        }
    }

const val CURRENT_MAP_LOCATION_GUIDE_VERSION = 1
