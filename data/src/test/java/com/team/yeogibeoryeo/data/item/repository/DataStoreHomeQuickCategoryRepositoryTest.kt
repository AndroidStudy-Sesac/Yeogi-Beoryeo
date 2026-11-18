package com.team.yeogibeoryeo.data.item.repository

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalApi
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalCategory
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorContext
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorReporter
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalStage
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.sink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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
    fun `DataStore를 다시 생성해도 선택한 분류와 순서를 복원한다`() =
        runBlocking {
            val file = withContext(Dispatchers.IO) {
                File.createTempFile("persisted-home-quick-category", ".preferences_pb").apply {
                    delete()
                }
            }
            val reporter = RecordingNonFatalErrorReporter()
            val firstJob = SupervisorJob()
            val firstDataStore = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(firstJob + Dispatchers.IO),
                produceFile = { file },
            )

            try {
                val repository = DataStoreHomeQuickCategoryRepository(firstDataStore, reporter)
                repository.toggleHomeQuickCategory(DisposalCategory.ELECTRONICS, maxSelectedCount = 2)
                repository.toggleHomeQuickCategory(DisposalCategory.BATTERY, maxSelectedCount = 2)
                firstJob.cancelAndJoin()

                val secondJob = SupervisorJob()
                val secondDataStore = PreferenceDataStoreFactory.create(
                    scope = CoroutineScope(secondJob + Dispatchers.IO),
                    produceFile = { file },
                )
                try {
                    val restoredCategories =
                        DataStoreHomeQuickCategoryRepository(secondDataStore, reporter)
                            .observeHomeQuickCategories()
                            .first()

                    assertEquals(
                        listOf(DisposalCategory.ELECTRONICS, DisposalCategory.BATTERY),
                        restoredCategories,
                    )
                    assertTrue(reporter.errors.isEmpty())
                } finally {
                    secondJob.cancelAndJoin()
                }
            } finally {
                firstJob.cancelAndJoin()
                withContext(Dispatchers.IO) {
                    file.delete()
                }
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
    fun `손상과 비입출력 읽기 실패는 재시도하지 않고 기존 빈 목록으로 복구한다`() =
        runBlocking {
            for (failure in listOf(CorruptionException("corrupted"), IllegalStateException("invalid"))) {
                val reporter = RecordingNonFatalErrorReporter()
                val dataStore = FailingDataStore(failure)
                val repository = DataStoreHomeQuickCategoryRepository(dataStore, reporter)

                val categories = repository.observeHomeQuickCategories().first()

                assertTrue(categories.isEmpty())
                assertEquals(1, dataStore.readAttempts)
                assertEquals(
                    listOf(RecordedError(failure, CACHE_READ_ERROR_CONTEXT)),
                    reporter.errors,
                )
            }
        }

    @Test
    fun `실제 DataStore 읽기 실패 후 저장값과 후속 변경을 같은 구독에서 받는다`() =
        runTest {
            val initialPreferences = batteryPreferences()
            val file = File.createTempFile("recovering-home-categories", ".preferences_pb")
            val dataStoreJob = SupervisorJob()
            val failure = IOException("temporary read failure")
            var readAttempts = 0
            val serializer = object : OkioSerializer<Preferences> {
                override val defaultValue: Preferences = emptyPreferences()

                override suspend fun readFrom(source: BufferedSource): Preferences {
                    readAttempts += 1
                    if (readAttempts == 1) throw failure
                    return PreferencesSerializer.readFrom(source)
                }

                override suspend fun writeTo(t: Preferences, sink: BufferedSink) {
                    PreferencesSerializer.writeTo(t, sink)
                }
            }
            val dataStore = PreferenceDataStoreFactory.create(
                storage = OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = serializer,
                    producePath = { file.absolutePath.toPath() },
                ),
                scope = CoroutineScope(dataStoreJob + StandardTestDispatcher(testScheduler)),
            )
            val trackedDataStore = TrackingDataStore(dataStore)
            val reporter = RecordingNonFatalErrorReporter()
            val repository = DataStoreHomeQuickCategoryRepository(trackedDataStore, reporter)
            val values = mutableListOf<List<DisposalCategory>>()

            try {
                file.sink().buffer().use { sink ->
                    PreferencesSerializer.writeTo(initialPreferences, sink)
                }
                val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    repository.observeHomeQuickCategories().toList(values)
                }
                runCurrent()

                assertTrue(values.isEmpty())
                assertEquals(1, readAttempts)
                assertEquals(1, trackedDataStore.collections)
                assertEquals(0, trackedDataStore.activeCollections)

                advanceTimeBy(1_000)
                runCurrent()

                assertEquals(listOf(listOf(DisposalCategory.BATTERY)), values)
                assertEquals(2, readAttempts)
                assertEquals(2, trackedDataStore.collections)
                assertEquals(1, trackedDataStore.activeCollections)
                assertEquals(1, trackedDataStore.maxActiveCollections)
                assertEquals(listOf(RecordedError(failure, CACHE_READ_ERROR_CONTEXT)), reporter.errors)

                repository.toggleHomeQuickCategory(DisposalCategory.ELECTRONICS, maxSelectedCount = 2)
                runCurrent()
                assertEquals(listOf(DisposalCategory.BATTERY, DisposalCategory.ELECTRONICS), values.last())

                collector.cancelAndJoin()
                assertEquals(0, trackedDataStore.activeCollections)
                assertEquals(2, trackedDataStore.finishedCollections)
                advanceTimeBy(30_000)
                runCurrent()
                assertEquals(2, trackedDataStore.collections)
            } finally {
                dataStoreJob.cancelAndJoin()
                file.delete()
            }
        }

    @Test
    fun `연속 읽기 실패는 지연 상한을 지키고 구독 취소 후 재시도하지 않는다`() =
        runTest {
            val failure = IOException("unavailable")
            val dataStore = FailingDataStore(failure)
            val reporter = RecordingNonFatalErrorReporter()
            val repository = DataStoreHomeQuickCategoryRepository(dataStore, reporter)
            val values = mutableListOf<List<DisposalCategory>>()
            val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                repository.observeHomeQuickCategories().toList(values)
            }
            runCurrent()

            listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 30_000L, 30_000L).forEachIndexed { index, delay ->
                advanceTimeBy(delay - 1)
                runCurrent()
                assertEquals(index + 1, dataStore.readAttempts)
                advanceTimeBy(1)
                runCurrent()
                assertEquals(index + 2, dataStore.readAttempts)
            }

            assertTrue(values.isEmpty())
            assertEquals(listOf(RecordedError(failure, CACHE_READ_ERROR_CONTEXT)), reporter.errors)
            collector.cancelAndJoin()
            val attemptsAtCancellation = dataStore.readAttempts
            advanceTimeBy(60_000)
            runCurrent()
            assertEquals(attemptsAtCancellation, dataStore.readAttempts)
            assertEquals(1, reporter.errors.size)
        }

    @Test
    fun `정상값 뒤 새 읽기 실패는 값을 보존하고 지연과 오류 기록을 다시 시작한다`() =
        runTest {
            val firstFailure = IOException("first outage")
            val nextFailure = IOException("next outage")
            val failAfterValue = CompletableDeferred<Unit>()
            val updates = MutableStateFlow(batteryPreferences())
            var attempts = 0
            val dataStore = object : DataStore<Preferences> {
                override val data: Flow<Preferences> = flow {
                    attempts += 1
                    when (attempts) {
                        1, 2 -> throw firstFailure
                        3 -> {
                            emit(updates.value)
                            failAfterValue.await()
                            throw nextFailure
                        }
                        else -> emitAll(updates)
                    }
                }

                override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                    updates.value = transform(updates.value)
                    return updates.value
                }
            }
            val trackedDataStore = TrackingDataStore(dataStore)
            val reporter = RecordingNonFatalErrorReporter()
            val repository = DataStoreHomeQuickCategoryRepository(trackedDataStore, reporter)
            val values = mutableListOf<List<DisposalCategory>>()
            val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                repository.observeHomeQuickCategories().toList(values)
            }
            runCurrent()
            advanceTimeBy(1_000)
            runCurrent()
            advanceTimeBy(2_000)
            runCurrent()
            assertEquals(listOf(listOf(DisposalCategory.BATTERY)), values)

            failAfterValue.complete(Unit)
            runCurrent()
            assertEquals(listOf(listOf(DisposalCategory.BATTERY)), values)
            assertEquals(
                listOf(
                    RecordedError(firstFailure, CACHE_READ_ERROR_CONTEXT),
                    RecordedError(nextFailure, CACHE_READ_ERROR_CONTEXT),
                ),
                reporter.errors,
            )
            advanceTimeBy(999)
            runCurrent()
            assertEquals(3, attempts)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(4, attempts)

            updates.value = emptyPreferences()
            runCurrent()
            assertEquals(emptyList<DisposalCategory>(), values.last())
            assertEquals(1, trackedDataStore.maxActiveCollections)
            collector.cancelAndJoin()
            assertEquals(0, trackedDataStore.activeCollections)
            assertEquals(4, trackedDataStore.finishedCollections)
        }

    @Test
    fun `취소된 읽기가 입출력 오류로 끝나도 기록하거나 재구독하지 않는다`() =
        runTest {
            val dataStore = object : DataStore<Preferences> {
                override val data: Flow<Preferences> = flow {
                    try {
                        awaitCancellation()
                    } catch (cancellation: CancellationException) {
                        throw IOException("cancelled read", cancellation)
                    }
                }

                override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                    error("Writes are not used in this test")
            }
            val trackedDataStore = TrackingDataStore(dataStore)
            val reporter = RecordingNonFatalErrorReporter()
            val repository = DataStoreHomeQuickCategoryRepository(trackedDataStore, reporter)
            val values = mutableListOf<List<DisposalCategory>>()
            val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                repository.observeHomeQuickCategories().toList(values)
            }
            runCurrent()
            assertEquals(1, trackedDataStore.activeCollections)

            collector.cancelAndJoin()
            advanceTimeBy(30_000)
            runCurrent()

            assertEquals(0, reporter.errors.size)
            assertTrue(values.isEmpty())
            assertEquals(1, trackedDataStore.collections)
            assertEquals(1, trackedDataStore.finishedCollections)
            assertEquals(0, trackedDataStore.activeCollections)
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
        var readAttempts = 0
            private set

        override val data: Flow<Preferences> = flow {
            readAttempts += 1
            throw failure
        }

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            throw failure
        }
    }

    private class TrackingDataStore(
        private val delegate: DataStore<Preferences>,
    ) : DataStore<Preferences> by delegate {
        var collections = 0
            private set
        var activeCollections = 0
            private set
        var maxActiveCollections = 0
            private set
        var finishedCollections = 0
            private set

        override val data: Flow<Preferences> = flow {
            collections += 1
            activeCollections += 1
            maxActiveCollections = maxOf(maxActiveCollections, activeCollections)
            try {
                emitAll(delegate.data)
            } finally {
                activeCollections -= 1
                finishedCollections += 1
            }
        }
    }

    private fun batteryPreferences(): Preferences =
        preferencesOf(
            stringPreferencesKey("pinned_disposal_categories") to DisposalCategory.BATTERY.name,
        )

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
