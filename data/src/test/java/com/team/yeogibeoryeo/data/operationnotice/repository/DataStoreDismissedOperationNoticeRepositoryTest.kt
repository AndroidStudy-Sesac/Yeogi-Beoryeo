package com.team.yeogibeoryeo.data.operationnotice.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test

class DataStoreDismissedOperationNoticeRepositoryTest {

    @Test
    fun `저장된 닫은 공지가 없으면 빈 set을 반환한다`() = runBlocking {
        withRepository { repository ->
            assertEquals(emptySet<String>(), repository.observeDismissedNoticeIds().first())
        }
    }

    @Test
    fun `공지 ID를 닫으면 trim된 ID를 저장한다`() = runBlocking {
        withRepository { repository ->
            repository.dismissNotice(" notice-1 ")

            assertEquals(setOf("notice-1"), repository.observeDismissedNoticeIds().first())
        }
    }

    @Test
    fun `빈 공지 ID는 저장하지 않는다`() = runBlocking {
        withRepository { repository ->
            repository.dismissNotice("   ")

            assertEquals(emptySet<String>(), repository.observeDismissedNoticeIds().first())
        }
    }

    @Test
    fun `여러 공지 ID를 닫으면 기존 ID와 함께 저장한다`() = runBlocking {
        withRepository { repository ->
            repository.dismissNotice("notice-1")
            repository.dismissNotice("notice-2")

            assertEquals(setOf("notice-1", "notice-2"), repository.observeDismissedNoticeIds().first())
        }
    }

    private suspend fun withRepository(
        block: suspend (DataStoreDismissedOperationNoticeRepository) -> Unit,
    ) {
        val file = withContext(Dispatchers.IO) {
            File.createTempFile("dismissed-operation-notices", ".preferences_pb").apply { delete() }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file },
        )

        try {
            block(DataStoreDismissedOperationNoticeRepository(dataStore))
        } finally {
            scope.cancel()
            withContext(Dispatchers.IO) {
                file.delete()
            }
        }
    }
}
