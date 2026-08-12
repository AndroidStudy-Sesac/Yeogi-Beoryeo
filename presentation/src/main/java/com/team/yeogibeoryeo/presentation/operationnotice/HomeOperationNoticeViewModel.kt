package com.team.yeogibeoryeo.presentation.operationnotice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeFeature
import com.team.yeogibeoryeo.domain.operationnotice.usecase.DismissOperationNoticeUseCase
import com.team.yeogibeoryeo.domain.operationnotice.usecase.ObserveOperationNoticesForFeatureUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeOperationNoticeViewModel
@Inject
constructor(
    observeOperationNoticesForFeatureUseCase: ObserveOperationNoticesForFeatureUseCase,
    private val dismissOperationNoticeUseCase: DismissOperationNoticeUseCase,
) : ViewModel() {
    val notice: StateFlow<OperationNoticeUiModel?> =
        observeOperationNoticesForFeatureUseCase(OperationNoticeFeature.HOME)
            .map { notices -> notices.firstOrNull()?.toUiModel() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = null,
            )

    fun dismissNotice(id: String) {
        viewModelScope.launch {
            dismissOperationNoticeUseCase(id)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

