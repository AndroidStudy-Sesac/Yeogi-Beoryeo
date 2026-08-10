package com.team.yeogibeoryeo.core.startup

import com.team.yeogibeoryeo.core.di.ApplicationScope
import com.team.yeogibeoryeo.domain.operationnotice.usecase.RefreshOperationNoticesUseCase
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Singleton
class OperationNoticeStartupInitializer
@Inject
constructor(
    @param:ApplicationScope private val applicationScope: CoroutineScope,
    private val refreshOperationNoticesUseCase: RefreshOperationNoticesUseCase,
) {
    private val started = AtomicBoolean(false)

    fun initialize() {
        if (!started.compareAndSet(false, true)) return

        applicationScope.launch {
            refreshOperationNoticesUseCase()
        }
    }
}
