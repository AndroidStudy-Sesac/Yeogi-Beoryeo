package com.team.yeogibeoryeo.data.spot.cache

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheClearResult
import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
            val repository = DataStoreRecentCurrentLocationSpotCacheRepository(dataStore)

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
            val repository = DataStoreRecentCurrentLocationSpotCacheRepository(dataStore)

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

    private suspend fun withRepository(
        block: suspend (DataStoreRecentCurrentLocationSpotCacheRepository) -> Unit,
    ) {
        val dataStore = InMemoryPreferencesDataStore()
        val repository = DataStoreRecentCurrentLocationSpotCacheRepository(dataStore)

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

    private companion object {
        val TEST_COORDINATE = Coordinate(latitude = 37.5666102, longitude = 126.9783881)
    }
}
