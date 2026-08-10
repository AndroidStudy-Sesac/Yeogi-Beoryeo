package com.team.yeogibeoryeo.domain.operationnotice.usecase

import com.team.yeogibeoryeo.domain.operationnotice.repository.DismissedOperationNoticeRepository
import javax.inject.Inject

class DismissOperationNoticeUseCase
@Inject
constructor(
    private val repository: DismissedOperationNoticeRepository,
) {
    suspend operator fun invoke(id: String) {
        repository.dismissNotice(id)
    }
}

