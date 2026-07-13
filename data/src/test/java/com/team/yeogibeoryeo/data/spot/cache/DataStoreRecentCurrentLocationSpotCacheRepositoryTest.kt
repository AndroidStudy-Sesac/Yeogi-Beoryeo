package com.team.yeogibeoryeo.data.spot.cache

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
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
    fun `저장된 최근 현재 위치 캐시를 삭제하면 Deleted를 반환하고 캐시를 제거한다`() =
        runBlocking {
            withRepository { repository ->
                repository.saveRecentCurrentLocationSpots(
                    RecentCurrentLocationSpotCacheEntry(
                        spots = listOf(sampleSpot("cached")),
                        savedAtMillis = 1_000L,
                    ),
                )

                val result = repository.clearRecentCurrentLocationSpots()

                assertEquals(RecentCurrentLocationSpotCacheClearResult.Deleted, result)
                assertNull(repository.getRecentCurrentLocationSpots())
            }
        }

    @Test
    fun `저장된 최근 현재 위치 캐시가 없으면 NoCache를 반환한다`() =
        runBlocking {
            withRepository { repository ->
                val result = repository.clearRecentCurrentLocationSpots()

                assertEquals(RecentCurrentLocationSpotCacheClearResult.NoCache, result)
                assertNull(repository.getRecentCurrentLocationSpots())
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

    private suspend fun withRepository(
        block: suspend (DataStoreRecentCurrentLocationSpotCacheRepository) -> Unit,
    ) {
        val dataStore = InMemoryPreferencesDataStore()
        val repository = DataStoreRecentCurrentLocationSpotCacheRepository(dataStore)

        block(repository)
    }

    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val preferences = MutableStateFlow<Preferences>(emptyPreferences())

        override val data: Flow<Preferences> = preferences

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences {
            val updatedPreferences = transform(preferences.value)
            preferences.value = updatedPreferences
            return updatedPreferences
        }
    }
}
