package com.team.yeogibeoryeo.domain.operationnotice.repository

import kotlinx.coroutines.flow.Flow

interface DismissedOperationNoticeRepository {
    fun observeDismissedNoticeIds(): Flow<Set<String>>

    suspend fun dismissNotice(id: String)
}

