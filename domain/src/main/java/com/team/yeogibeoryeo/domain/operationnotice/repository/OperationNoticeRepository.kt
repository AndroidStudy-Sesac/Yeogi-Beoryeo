package com.team.yeogibeoryeo.domain.operationnotice.repository

import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNotice
import kotlinx.coroutines.flow.Flow

interface OperationNoticeRepository {
    fun observeOperationNotices(): Flow<List<OperationNotice>>

    suspend fun refreshOperationNotices()
}

