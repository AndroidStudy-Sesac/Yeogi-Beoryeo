package com.team.yeogibeoryeo.presentation.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.yeogibeoryeo.domain.spot.usecase.ClearRecentCurrentLocationSpotsUseCase
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.cache.RecentCurrentLocationCacheClearNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsCacheViewModel
@Inject
constructor(
    private val clearRecentCurrentLocationSpotsUseCase: ClearRecentCurrentLocationSpotsUseCase,
    private val recentCurrentLocationCacheClearNotifier: RecentCurrentLocationCacheClearNotifier,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsCacheUiState())
    val uiState: StateFlow<SettingsCacheUiState> = _uiState.asStateFlow()
    private var clearLocationCacheJob: Job? = null

    fun clearLocationCache() {
        if (clearLocationCacheJob?.isActive == true) return

        clearLocationCacheJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isClearingLocationCache = true,
                    locationCacheMessageResId = null,
                )
            }

            try {
                clearRecentCurrentLocationSpotsUseCase()
                recentCurrentLocationCacheClearNotifier.notifyCleared()
                _uiState.update {
                    it.copy(
                        isClearingLocationCache = false,
                        locationCacheMessageResId = R.string.settings_cache_delete_success_message,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                _uiState.update {
                    it.copy(
                        isClearingLocationCache = false,
                        locationCacheMessageResId = R.string.settings_cache_delete_failure_message,
                    )
                }
            }
        }
    }
}

data class SettingsCacheUiState(
    val isClearingLocationCache: Boolean = false,
    @param:StringRes val locationCacheMessageResId: Int? = null,
)
