package com.team.yeogibeoryeo.presentation.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.notice.model.Notice
import com.team.yeogibeoryeo.domain.notice.usecase.GetPublishedNoticesUseCase
import com.team.yeogibeoryeo.domain.notice.usecase.GetReadNoticeIdsUseCase
import com.team.yeogibeoryeo.domain.notice.usecase.MarkNoticeReadUseCase
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
    private val getReadNoticeIdsUseCase: GetReadNoticeIdsUseCase,
    private val markNoticeReadUseCase: MarkNoticeReadUseCase,
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

    fun refreshNotices() {
        loadNotices()
    }

    fun selectNotice(noticeId: String) {
        val content = _uiState.value as? SettingsNoticeUiState.Content ?: return
        if (content.notices.none { notice -> notice.id == noticeId }) return

        savedStateHandle[SELECTED_NOTICE_ID_KEY] = noticeId
        _uiState.value = content.copy(
            readNoticeIds = content.readNoticeIds + noticeId,
            selectedNoticeId = noticeId,
        )
        viewModelScope.launch {
            markNoticeReadUseCase(noticeId)
        }
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
                val persistedReadNoticeIds = getReadNoticeIdsUseCase()
                val restoredNoticeId = savedStateHandle.get<String>(SELECTED_NOTICE_ID_KEY)
                    ?.takeIf { noticeId -> notices.any { notice -> notice.id == noticeId } }
                val readNoticeIds = if (restoredNoticeId != null) {
                    persistedReadNoticeIds + restoredNoticeId
                } else {
                    persistedReadNoticeIds
                }

                if (restoredNoticeId == null) {
                    savedStateHandle.remove<String>(SELECTED_NOTICE_ID_KEY)
                }
                _uiState.value = SettingsNoticeUiState.Content(
                    notices = notices,
                    readNoticeIds = readNoticeIds,
                    selectedNoticeId = restoredNoticeId,
                )
                if (restoredNoticeId != null && restoredNoticeId !in persistedReadNoticeIds) {
                    markNoticeReadUseCase(restoredNoticeId)
                }
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
        val readNoticeIds: Set<String> = emptySet(),
        val selectedNoticeId: String? = null,
    ) : SettingsNoticeUiState {
        val selectedNotice: Notice?
            get() = notices.firstOrNull { notice -> notice.id == selectedNoticeId }

        val hasUnreadNotices: Boolean
            get() = notices.any { notice -> notice.id !in readNoticeIds }

        fun isUnread(noticeId: String): Boolean = noticeId !in readNoticeIds
    }
}
