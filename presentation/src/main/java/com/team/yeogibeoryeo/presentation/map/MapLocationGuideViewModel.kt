package com.team.yeogibeoryeo.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.appguide.repository.AppGuideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapLocationGuideUiState(
    val isReady: Boolean = false,
    val isVisible: Boolean = false,
    val hasRequestedLocationPermission: Boolean = false,
    val isLocationPermissionRequestBlocked: Boolean = false,
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
                combine(
                    repository.observeCompletedMapLocationGuideVersion(),
                    repository.observeHasRequestedMapLocationPermission(),
                    repository.observeIsMapLocationPermissionBlocked(),
                ) { completedVersion, hasRequestedLocationPermission, isLocationPermissionRequestBlocked ->
                    MapLocationGuideUiState(
                        isReady = true,
                        isVisible = completedVersion < CURRENT_MAP_LOCATION_GUIDE_VERSION,
                        hasRequestedLocationPermission = hasRequestedLocationPermission,
                        isLocationPermissionRequestBlocked = isLocationPermissionRequestBlocked,
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            }
        }

        fun dismissGuide() {
            _uiState.update { it.copy(isVisible = false) }
            viewModelScope.launch {
                repository.markMapLocationGuideCompleted(CURRENT_MAP_LOCATION_GUIDE_VERSION)
            }
        }

        fun markLocationPermissionRequested() {
            _uiState.update { it.copy(hasRequestedLocationPermission = true) }
            viewModelScope.launch {
                repository.markMapLocationPermissionRequested()
            }
        }

        fun markLocationPermissionBlocked() {
            _uiState.update { it.copy(isLocationPermissionRequestBlocked = true) }
            viewModelScope.launch {
                repository.markMapLocationPermissionBlocked()
            }
        }

        fun clearLocationPermissionBlocked() {
            _uiState.update { it.copy(isLocationPermissionRequestBlocked = false) }
            viewModelScope.launch {
                repository.clearMapLocationPermissionBlocked()
            }
        }
    }

const val CURRENT_MAP_LOCATION_GUIDE_VERSION = 1
