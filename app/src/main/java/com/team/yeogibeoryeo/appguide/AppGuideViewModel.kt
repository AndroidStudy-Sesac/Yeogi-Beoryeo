package com.team.yeogibeoryeo.appguide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.appguide.repository.AppGuideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AppGuideViewModel
    @Inject
    constructor(
        private val repository: AppGuideRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AppGuideUiState())
        val uiState: StateFlow<AppGuideUiState> = _uiState.asStateFlow()

        private var manuallyStarted = false

        init {
            viewModelScope.launch {
                val completedVersion = repository.observeCompletedVersion().first()
                _uiState.update { state ->
                    if (manuallyStarted) {
                        state.copy(isReady = true)
                    } else {
                        state.copy(
                            isReady = true,
                            isVisible = completedVersion < CURRENT_APP_GUIDE_VERSION,
                            currentStep = AppGuideStep.ITEM_SEARCH,
                        )
                    }
                }
            }
        }

        fun startGuide() {
            manuallyStarted = true
            _uiState.value = AppGuideUiState(
                isReady = true,
                isVisible = true,
                currentStep = AppGuideStep.ITEM_SEARCH,
            )
        }

        fun showNextStep() {
            val nextStep = _uiState.value.currentStep.nextOrNull()
            if (nextStep == null) {
                completeGuide()
            } else {
                _uiState.update { state -> state.copy(currentStep = nextStep) }
            }
        }

        fun showPreviousStep() {
            _uiState.update { state ->
                state.copy(currentStep = state.currentStep.previous())
            }
        }

        fun skipGuide() {
            completeGuide()
        }

        private fun completeGuide() {
            _uiState.update { state -> state.copy(isVisible = false) }
            viewModelScope.launch {
                repository.markCompleted(CURRENT_APP_GUIDE_VERSION)
            }
        }
    }
