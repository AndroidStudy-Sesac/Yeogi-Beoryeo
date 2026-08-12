package com.team.yeogibeoryeo.presentation.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.notice.model.Notice
import com.team.yeogibeoryeo.domain.notice.usecase.GetPublishedNoticesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsNoticeViewModel @Inject constructor(
    private val getPublishedNoticesUseCase: GetPublishedNoticesUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow<SettingsNoticeUiState>(SettingsNoticeUiState.Loading)
    val uiState: StateFlow<SettingsNoticeUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadNotices()
    }

    fun retryLoad() {
        if (_uiState.value != SettingsNoticeUiState.LoadFailed) return
        loadNotices()
    }

    fun selectNotice(noticeId: String) {
        val content = _uiState.value as? SettingsNoticeUiState.Content ?: return
        if (content.notices.none { notice -> notice.id == noticeId }) return

        savedStateHandle[SELECTED_NOTICE_ID_KEY] = noticeId
        _uiState.value = content.copy(selectedNoticeId = noticeId)
    }

    fun clearNoticeSelection() {
        val content = _uiState.value as? SettingsNoticeUiState.Content ?: return
        savedStateHandle.remove<String>(SELECTED_NOTICE_ID_KEY)
        _uiState.value = content.copy(selectedNoticeId = null)
    }

    private fun loadNotices() {
        if (loadJob?.isActive == true) return

        _uiState.value = SettingsNoticeUiState.Loading
        loadJob = viewModelScope.launch {
            try {
                val notices = getPublishedNoticesUseCase()
                val restoredNoticeId = savedStateHandle.get<String>(SELECTED_NOTICE_ID_KEY)
                    ?.takeIf { noticeId -> notices.any { notice -> notice.id == noticeId } }

                if (restoredNoticeId == null) {
                    savedStateHandle.remove<String>(SELECTED_NOTICE_ID_KEY)
                }
                _uiState.value = SettingsNoticeUiState.Content(
                    notices = notices,
                    selectedNoticeId = restoredNoticeId,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = SettingsNoticeUiState.LoadFailed
            }
        }
    }

    private companion object {
        const val SELECTED_NOTICE_ID_KEY = "selectedNoticeId"
    }
}

sealed interface SettingsNoticeUiState {
    data object Loading : SettingsNoticeUiState

    data object LoadFailed : SettingsNoticeUiState

    data class Content(
        val notices: List<Notice>,
        val selectedNoticeId: String? = null,
    ) : SettingsNoticeUiState {
        val selectedNotice: Notice?
            get() = notices.firstOrNull { notice -> notice.id == selectedNoticeId }
    }
}
