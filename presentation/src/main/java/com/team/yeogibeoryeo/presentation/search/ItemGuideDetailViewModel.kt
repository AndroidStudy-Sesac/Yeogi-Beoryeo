package com.team.yeogibeoryeo.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.usecase.SearchDisposalItemGuidesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ItemGuideDetailViewModel
@Inject
constructor(
    private val searchDisposalItemGuidesUseCase: SearchDisposalItemGuidesUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ItemGuideDetailUiState>(ItemGuideDetailUiState.Loading)
    val uiState: StateFlow<ItemGuideDetailUiState> = _uiState.asStateFlow()

    fun loadGuide(guideId: String) {
        viewModelScope.launch {
            _uiState.value = ItemGuideDetailUiState.Loading

            try {
                val guides = searchDisposalItemGuidesUseCase(guideId)
                val guide =
                    guides.firstOrNull { it.id == guideId || it.name == guideId }
                        ?: guides.firstOrNull()

                _uiState.value =
                    if (guide != null) {
                        ItemGuideDetailUiState.Success(guide)
                    } else {
                        ItemGuideDetailUiState.Error("다시 검색해서 품목을 선택해주세요.")
                    }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                _uiState.value = ItemGuideDetailUiState.Error("품목 정보를 불러오는 중 오류가 발생했습니다.")
            }
        }
    }
}

sealed interface ItemGuideDetailUiState {
    data object Loading : ItemGuideDetailUiState

    data class Success(
        val guide: DisposalItemGuide,
    ) : ItemGuideDetailUiState

    data class Error(
        val message: String,
    ) : ItemGuideDetailUiState
}
