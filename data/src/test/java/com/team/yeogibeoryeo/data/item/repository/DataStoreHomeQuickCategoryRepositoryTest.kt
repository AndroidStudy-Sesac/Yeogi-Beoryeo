package com.team.yeogibeoryeo.data.item.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalApi
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalCategory
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorContext
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorReporter
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalStage
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStoreHomeQuickCategoryRepositoryTest {

    @Test
    fun `저장된 분류가 없으면 빈 목록을 반환한다`() =
        runBlocking {
            withRepository { repository, reporter ->
                assertEquals(emptyList<DisposalCategory>(), repository.observeHomeQuickCategories().first())
                assertTrue(reporter.errors.isEmpty())
            }
        }

    @Test
    fun `분류를 토글하면 선택 상태를 저장하고 다시 토글하면 제거한다`() =
        runBlocking {
            withRepository { repository, _ ->
                repository.toggleHomeQuickCategory(DisposalCategory.BATTERY, maxSelectedCount = 2)

                assertEquals(
                    listOf(DisposalCategory.BATTERY),
                    repository.observeHomeQuickCategories().first(),
                )

                repository.toggleHomeQuickCategory(DisposalCategory.BATTERY, maxSelectedCount = 2)

                assertEquals(emptyList<DisposalCategory>(), repository.observeHomeQuickCategories().first())
            }
        }

    @Test
    fun `최대 개수에 도달하면 새 분류를 추가하지 않는다`() =
        runBlocking {
            withRepository { repository, _ ->
                repository.toggleHomeQuickCategory(DisposalCategory.BATTERY, maxSelectedCount = 1)
                repository.toggleHomeQuickCategory(DisposalCategory.ELECTRONICS, maxSelectedCount = 1)

                assertEquals(
                    listOf(DisposalCategory.BATTERY),
                    repository.observeHomeQuickCategories().first(),
                )
            }
        }

    @Test
    fun `표시 개수를 제한하면 앞에서부터 지정한 개수만 유지한다`() =
        runBlocking {
            withRepository { repository, _ ->
                repository.toggleHomeQuickCategory(DisposalCategory.BATTERY, maxSelectedCount = 2)
                repository.toggleHomeQuickCategory(DisposalCategory.ELECTRONICS, maxSelectedCount = 2)

                repository.limitHomeQuickCategories(maxSelectedCount = 1)

                assertEquals(
                    listOf(DisposalCategory.BATTERY),
                    repository.observeHomeQuickCategories().first(),
                )
            }
        }

    @Test
    fun `캐시 읽기 실패를 한 번 기록하고 빈 목록으로 복구한다`() =
        runBlocking {
            val failure = IOException("private cache path")
            val reporter = RecordingNonFatalErrorReporter()
            val repository = DataStoreHomeQuickCategoryRepository(FailingDataStore(failure), reporter)

            val categories = repository.observeHomeQuickCategories().first()

            assertTrue(categories.isEmpty())
            assertEquals(
                listOf(RecordedError(failure, CACHE_READ_ERROR_CONTEXT)),
                reporter.errors,
            )
        }

    @Test
    fun `캐시 읽기 취소와 치명 오류는 기록하지 않고 원래 오류를 전파한다`() =
        runBlocking {
            for (failure in listOf(CancellationException("cancelled"), LinkageError("fatal"))) {
                val reporter = RecordingNonFatalErrorReporter()
                val repository = DataStoreHomeQuickCategoryRepository(FailingDataStore(failure), reporter)

                val thrown = runCatching {
                    repository.observeHomeQuickCategories().first()
                }.exceptionOrNull()

                assertSame(failure, thrown)
                assertTrue(reporter.errors.isEmpty())
            }
        }

    @Test
    fun `캐시 쓰기 실패를 한 번 기록하고 기존 상태 유지 흐름을 끝낸다`() =
        runBlocking {
            val actions =
                listOf<suspend (DataStoreHomeQuickCategoryRepository) -> Unit>(
                    { repository ->
                        repository.toggleHomeQuickCategory(DisposalCategory.BATTERY, maxSelectedCount = 1)
                    },
                    { repository -> repository.limitHomeQuickCategories(maxSelectedCount = 1) },
                )

            actions.forEach { action ->
                val failure = IOException("private cache path")
                val reporter = RecordingNonFatalErrorReporter()
                val repository = DataStoreHomeQuickCategoryRepository(FailingDataStore(failure), reporter)

                action(repository)

                assertEquals(
                    listOf(RecordedError(failure, CACHE_WRITE_ERROR_CONTEXT)),
                    reporter.errors,
                )
            }
        }

    @Test
    fun `캐시 쓰기 취소는 기록하지 않고 원래 취소를 전파한다`() =
        runBlocking {
            val cancellation = CancellationException("cancelled")
            val reporter = RecordingNonFatalErrorReporter()
            val repository = DataStoreHomeQuickCategoryRepository(FailingDataStore(cancellation), reporter)

            val thrown = runCatching {
                repository.toggleHomeQuickCategory(DisposalCategory.BATTERY, maxSelectedCount = 1)
            }.exceptionOrNull()

            assertSame(cancellation, thrown)
            assertTrue(reporter.errors.isEmpty())
        }

    private suspend fun withRepository(
        block: suspend (DataStoreHomeQuickCategoryRepository, RecordingNonFatalErrorReporter) -> Unit,
    ) {
        val file = withContext(Dispatchers.IO) {
            File.createTempFile("home-quick-category", ".preferences_pb").apply {
                delete()
            }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore =
            PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { file },
            )
        val reporter = RecordingNonFatalErrorReporter()
        val repository = DataStoreHomeQuickCategoryRepository(dataStore, reporter)

        try {
            block(repository, reporter)
        } finally {
            scope.cancel()
            withContext(Dispatchers.IO) {
                file.delete()
            }
        }
    }

    private class FailingDataStore(
        private val failure: Throwable,
    ) : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw failure }

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            throw failure
        }
    }

    private class RecordingNonFatalErrorReporter : NonFatalErrorReporter {
        val errors = mutableListOf<RecordedError>()

        override fun report(error: Throwable, context: NonFatalErrorContext) {
            errors += RecordedError(error, context)
        }
    }

    private data class RecordedError(
        val error: Throwable,
        val context: NonFatalErrorContext,
    )

    private companion object {
        val CACHE_READ_ERROR_CONTEXT =
            NonFatalErrorContext(
                api = NonFatalApi.ITEM_GUIDE,
                stage = NonFatalStage.CACHE_READ,
                category = NonFatalCategory.CACHE,
            )
        val CACHE_WRITE_ERROR_CONTEXT =
            NonFatalErrorContext(
                api = NonFatalApi.ITEM_GUIDE,
                stage = NonFatalStage.CACHE_WRITE,
                category = NonFatalCategory.CACHE,
            )
    }
}
