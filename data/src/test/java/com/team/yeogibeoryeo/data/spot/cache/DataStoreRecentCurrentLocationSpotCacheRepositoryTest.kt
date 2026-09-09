package com.team.yeogibeoryeo.data.spot.cache

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalApi
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalCategory
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorContext
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorReporter
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalStage
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheClearResult
import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DataStoreRecentCurrentLocationSpotCacheRepositoryTest {

    @Test
    fun `저장된 최근 현재 위치 캐시를 삭제하면 삭제됨 결과를 반환하고 캐시를 제거한다`() =
        runBlocking {
            withRepository { repository ->
                repository.saveRecentCurrentLocationSpots(
                    RecentCurrentLocationSpotCacheEntry(
                        spots = listOf(sampleSpot("저장됨")),
                        searchCoordinate = TEST_COORDINATE,
                        savedAtMillis = 1_000L,
                    ),
                )

                val result = repository.clearRecentCurrentLocationSpots()

                assertEquals(RecentCurrentLocationSpotCacheClearResult.Deleted, result)
                assertNull(repository.getRecentCurrentLocationSpots())
            }
        }

    @Test
    fun `저장된 최근 현재 위치 캐시가 없으면 캐시 없음 결과를 반환한다`() =
        runBlocking {
            withRepository { repository ->
                val result = repository.clearRecentCurrentLocationSpots()

                assertEquals(RecentCurrentLocationSpotCacheClearResult.NoCache, result)
                assertNull(repository.getRecentCurrentLocationSpots())
            }
        }

    @Test
    fun `삭제 작업 직전에 캐시가 저장되면 삭제됨 결과를 반환하고 캐시를 제거한다`() =
        runBlocking {
            val dataStore = InMemoryPreferencesDataStore()
            val repository = DataStoreRecentCurrentLocationSpotCacheRepository(
                dataStore,
                NoOpNonFatalErrorReporter,
            )

            dataStore.runBeforeNextUpdate {
                repository.saveRecentCurrentLocationSpots(
                    RecentCurrentLocationSpotCacheEntry(
                        spots = listOf(sampleSpot("동시저장")),
                        searchCoordinate = TEST_COORDINATE,
                        savedAtMillis = 1_000L,
                    ),
                )
            }

            val result = repository.clearRecentCurrentLocationSpots()

            assertEquals(RecentCurrentLocationSpotCacheClearResult.Deleted, result)
            assertNull(repository.getRecentCurrentLocationSpots())
        }

    @Test
    fun `기준 좌표가 없는 기존 캐시는 조회 시 무효 처리한다`() =
        runBlocking {
            val dataStore = InMemoryPreferencesDataStore()
            val reporter = RecordingNonFatalErrorReporter()
            val repository = DataStoreRecentCurrentLocationSpotCacheRepository(dataStore, reporter)

            dataStore.putString(
                key = "recent_current_location_spots",
                value = """{"spots":[],"savedAtMillis":1000}""",
            )

            val result = repository.getRecentCurrentLocationSpots()

            assertNull(result)
            assertEquals(
                RecentCurrentLocationSpotCacheClearResult.NoCache,
                repository.clearRecentCurrentLocationSpots(),
            )
            assertEquals(1, reporter.errors.size)
            assertEquals(CACHE_READ_PARSING_CONTEXT, reporter.errors.single().context)
        }

    @Test
    fun `손상된 캐시는 파싱 실패를 기록하고 제거한 뒤 null로 복구한다`() =
        runBlocking {
            val dataStore = InMemoryPreferencesDataStore().apply {
                putString(
                    key = "recent_current_location_spots",
                    value = "private malformed cache",
                )
            }
            val reporter = RecordingNonFatalErrorReporter()
            val repository = DataStoreRecentCurrentLocationSpotCacheRepository(dataStore, reporter)

            val result = repository.getRecentCurrentLocationSpots()

            assertNull(result)
            assertEquals(
                RecentCurrentLocationSpotCacheClearResult.NoCache,
                repository.clearRecentCurrentLocationSpots(),
            )
            assertEquals(1, reporter.errors.size)
            assertEquals(CACHE_READ_PARSING_CONTEXT, reporter.errors.single().context)
        }

    @Test
    fun `DataStore 읽기 실패를 기록하고 null로 복구한다`() =
        runBlocking {
            val failure = IOException("private cache path")
            val reporter = RecordingNonFatalErrorReporter()
            val repository = DataStoreRecentCurrentLocationSpotCacheRepository(
                FailingDataStore(failure),
                reporter,
            )

            val result = repository.getRecentCurrentLocationSpots()

            assertNull(result)
            assertEquals(
                listOf(RecordedError(failure, CACHE_READ_CONTEXT)),
                reporter.errors,
            )
        }

    @Test
    fun `캐시 저장 실패를 기록하고 호출을 정상 종료한다`() =
        runBlocking {
            val failure = IOException("private cache path")
            val reporter = RecordingNonFatalErrorReporter()
            val repository = DataStoreRecentCurrentLocationSpotCacheRepository(
                FailingDataStore(failure),
                reporter,
            )

            repository.saveRecentCurrentLocationSpots(sampleEntry())

            assertEquals(
                listOf(RecordedError(failure, CACHE_WRITE_CONTEXT)),
                reporter.errors,
            )
        }

    @Test
    fun `캐시 삭제 실패를 기록하고 Failed 결과를 유지한다`() =
        runBlocking {
            val failure = IOException("private cache path")
            val reporter = RecordingNonFatalErrorReporter()
            val repository = DataStoreRecentCurrentLocationSpotCacheRepository(
                FailingDataStore(failure),
                reporter,
            )

            val result = repository.clearRecentCurrentLocationSpots()

            assertEquals(RecentCurrentLocationSpotCacheClearResult.Failed, result)
            assertEquals(
                listOf(RecordedError(failure, CACHE_WRITE_CONTEXT)),
                reporter.errors,
            )
        }

    @Test
    fun `캐시 없음과 정상 읽기 쓰기 삭제는 기록하지 않는다`() =
        runBlocking {
            val dataStore = InMemoryPreferencesDataStore()
            val reporter = RecordingNonFatalErrorReporter()
            val repository = DataStoreRecentCurrentLocationSpotCacheRepository(dataStore, reporter)

            assertNull(repository.getRecentCurrentLocationSpots())
            repository.saveRecentCurrentLocationSpots(sampleEntry())
            assertEquals(sampleEntry(), repository.getRecentCurrentLocationSpots())
            assertEquals(
                RecentCurrentLocationSpotCacheClearResult.Deleted,
                repository.clearRecentCurrentLocationSpots(),
            )
            assertTrue(reporter.errors.isEmpty())
        }

    @Test
    fun `캐시 작업의 cancellation과 fatal Error는 기록하지 않고 원래 오류를 전파한다`() =
        runBlocking {
            val failures = listOf(CancellationException("cancelled"), LinkageError("fatal"))
            val actions = listOf<suspend (DataStoreRecentCurrentLocationSpotCacheRepository) -> Unit>(
                { repository -> repository.getRecentCurrentLocationSpots() },
                { repository -> repository.saveRecentCurrentLocationSpots(sampleEntry()) },
                { repository -> repository.clearRecentCurrentLocationSpots() },
            )

            failures.forEach { failure ->
                actions.forEach { action ->
                    val reporter = RecordingNonFatalErrorReporter()
                    val repository = DataStoreRecentCurrentLocationSpotCacheRepository(
                        FailingDataStore(failure),
                        reporter,
                    )

                    val thrown = runCatching { action(repository) }.exceptionOrNull()

                    assertSame(failure, thrown)
                    assertTrue(reporter.errors.isEmpty())
                }
            }
        }

    private fun sampleSpot(id: String): CollectionSpot {
        return CollectionSpot(
            id = id,
            name = "수거 장소 $id",
            type = CollectionSpotType.STANDARD_BAG_STORE,
            address = "서울특별시 영등포구",
            detailLocation = null,
            coordinate = null,
            distanceMeter = null,
            isBookmarked = false,
        )
    }

    private fun sampleEntry(): RecentCurrentLocationSpotCacheEntry =
        RecentCurrentLocationSpotCacheEntry(
            spots = listOf(sampleSpot("저장됨")),
            searchCoordinate = TEST_COORDINATE,
            savedAtMillis = 1_000L,
        )

    private suspend fun withRepository(
        block: suspend (DataStoreRecentCurrentLocationSpotCacheRepository) -> Unit,
    ) {
        val dataStore = InMemoryPreferencesDataStore()
        val repository = DataStoreRecentCurrentLocationSpotCacheRepository(
            dataStore,
            NoOpNonFatalErrorReporter,
        )

        block(repository)
    }

    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val preferences = MutableStateFlow<Preferences>(emptyPreferences())
        private var beforeNextUpdate: (suspend () -> Unit)? = null

        override val data: Flow<Preferences> = preferences

        fun runBeforeNextUpdate(block: suspend () -> Unit) {
            beforeNextUpdate = block
        }

        fun putString(
            key: String,
            value: String,
        ) {
            preferences.value = mutablePreferencesOf(stringPreferencesKey(key) to value)
        }

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences {
            val beforeUpdate = beforeNextUpdate
            beforeNextUpdate = null
            beforeUpdate?.invoke()

            val updatedPreferences = transform(preferences.value)
            preferences.value = updatedPreferences
            return updatedPreferences
        }
    }

    private class FailingDataStore(
        private val failure: Throwable,
    ) : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw failure }

        override suspend fun updateData(
            transform: suspend (Preferences) -> Preferences,
        ): Preferences {
            throw failure
        }
    }

    private object NoOpNonFatalErrorReporter : NonFatalErrorReporter {
        override fun report(error: Throwable, context: NonFatalErrorContext) = Unit
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
        val TEST_COORDINATE = Coordinate(latitude = 37.5666102, longitude = 126.9783881)
        val CACHE_READ_PARSING_CONTEXT = NonFatalErrorContext(
            api = NonFatalApi.COLLECTION_SPOT,
            stage = NonFatalStage.CACHE_READ,
            category = NonFatalCategory.PARSING,
        )
        val CACHE_READ_CONTEXT = NonFatalErrorContext(
            api = NonFatalApi.COLLECTION_SPOT,
            stage = NonFatalStage.CACHE_READ,
            category = NonFatalCategory.CACHE,
        )
        val CACHE_WRITE_CONTEXT = NonFatalErrorContext(
            api = NonFatalApi.COLLECTION_SPOT,
            stage = NonFatalStage.CACHE_WRITE,
            category = NonFatalCategory.CACHE,
        )
    }
}
