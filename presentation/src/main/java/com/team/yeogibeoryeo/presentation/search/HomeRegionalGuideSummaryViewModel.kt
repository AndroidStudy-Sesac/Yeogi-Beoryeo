package com.team.yeogibeoryeo.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.regionalguide.usecase.ObserveHomeRegionalGuideSummaryUseCase
import com.team.yeogibeoryeo.presentation.search.mapper.toUiState
import com.team.yeogibeoryeo.presentation.search.model.HomeRegionalGuideSummaryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeRegionalGuideSummaryViewModel
    @Inject
    constructor(
        private val observeHomeRegionalGuideSummaryUseCase: ObserveHomeRegionalGuideSummaryUseCase,
    ) : ViewModel() {
        private val retryRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        val uiState =
            retryRequests
                .onStart { emit(Unit) }
                .flatMapLatest { observeHomeRegionalGuideSummaryUseCase() }
                .map { result -> result.toUiState() }
                .runningFold(
                    initial = HomeRegionalGuideSummaryUiState.Loading(),
                    operation = ::keepPreviousSummaryWhileRefreshing,
                )
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = HomeRegionalGuideSummaryUiState.Loading(),
                )

        fun retry() {
            retryRequests.tryEmit(Unit)
        }

        private fun keepPreviousSummaryWhileRefreshing(
            previous: HomeRegionalGuideSummaryUiState,
            next: HomeRegionalGuideSummaryUiState,
        ): HomeRegionalGuideSummaryUiState {
            return if (
                previous is HomeRegionalGuideSummaryUiState.Summary &&
                    next is HomeRegionalGuideSummaryUiState.Loading &&
                    previous.targetId == next.targetId
            ) {
                previous
            } else {
                next
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
