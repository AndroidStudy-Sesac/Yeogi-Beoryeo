package com.team.yeogibeoryeo.domain.operationnotice.usecase

import com.team.yeogibeoryeo.domain.operationnotice.repository.OperationNoticeRepository
import javax.inject.Inject

class RefreshOperationNoticesUseCase
@Inject
constructor(
    private val repository: OperationNoticeRepository,
) {
    suspend operator fun invoke() {
        repository.refreshOperationNotices()
    }
}

