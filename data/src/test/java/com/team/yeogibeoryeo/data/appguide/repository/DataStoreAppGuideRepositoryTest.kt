package com.team.yeogibeoryeo.data.appguide.repository

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

class DataStoreAppGuideRepositoryTest {
    @Test
    fun `저장된 완료 버전이 없으면 영을 반환한다`() = runBlocking {
        withRepository { repository ->
            assertEquals(0, repository.observeCompletedVersion().first())
        }
    }

    @Test
    fun `가이드를 완료하면 완료 버전을 저장한다`() = runBlocking {
        withRepository { repository ->
            repository.markCompleted(version = 1)

            assertEquals(1, repository.observeCompletedVersion().first())
        }
    }

    @Test
    fun `낮은 버전으로 다시 완료해도 저장된 버전을 낮추지 않는다`() = runBlocking {
        withRepository { repository ->
            repository.markCompleted(version = 2)
            repository.markCompleted(version = 1)

            assertEquals(2, repository.observeCompletedVersion().first())
        }
    }

    private suspend fun withRepository(
        block: suspend (DataStoreAppGuideRepository) -> Unit,
    ) {
        val file = withContext(Dispatchers.IO) {
            File.createTempFile("app-guide", ".preferences_pb").apply { delete() }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file },
        )

        try {
            block(DataStoreAppGuideRepository(dataStore))
        } finally {
            scope.cancel()
            withContext(Dispatchers.IO) {
                file.delete()
            }
        }
    }
}
