package com.team.yeogibeoryeo.presentation.cache

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class RecentCurrentLocationCacheClearNotifier
@Inject
constructor() {
    private val _events = MutableSharedFlow<RecentCurrentLocationCacheClearEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<RecentCurrentLocationCacheClearEvent> = _events.asSharedFlow()

    fun notifyClearRequested() {
        _events.tryEmit(RecentCurrentLocationCacheClearEvent.ClearRequested)
    }

    fun notifyCleared() {
        _events.tryEmit(RecentCurrentLocationCacheClearEvent.ClearSucceeded)
    }
}

sealed interface RecentCurrentLocationCacheClearEvent {
    data object ClearRequested : RecentCurrentLocationCacheClearEvent
    data object ClearSucceeded : RecentCurrentLocationCacheClearEvent
}
