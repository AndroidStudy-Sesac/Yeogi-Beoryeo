package com.team.yeogibeoryeo.data.spot.cache

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheClearResult
import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheEntry
import com.team.yeogibeoryeo.domain.spot.repository.RecentCurrentLocationSpotCacheRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataStoreRecentCurrentLocationSpotCacheRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : RecentCurrentLocationSpotCacheRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun getRecentCurrentLocationSpots(): RecentCurrentLocationSpotCacheEntry? {
        return try {
            val cacheJson = dataStore.data.first()[RECENT_CURRENT_LOCATION_SPOTS_KEY] ?: return null

            json.decodeFromString<RecentCurrentLocationSpotCacheDto>(cacheJson).toDomain()
        } catch (exception: SerializationException) {
            clearRecentCurrentLocationSpots()
            null
        } catch (exception: IllegalArgumentException) {
            clearRecentCurrentLocationSpots()
            null
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            null
        }
    }

    override suspend fun saveRecentCurrentLocationSpots(
        entry: RecentCurrentLocationSpotCacheEntry,
    ) {
        try {
            val cacheJson = json.encodeToString(entry.toDto())

            dataStore.edit { preferences ->
                preferences[RECENT_CURRENT_LOCATION_SPOTS_KEY] = cacheJson
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            // Cache writes must not fail the current location search success flow.
        }
    }

    override suspend fun clearRecentCurrentLocationSpots(): RecentCurrentLocationSpotCacheClearResult {
        return try {
            val hadCache = dataStore.data.first()[RECENT_CURRENT_LOCATION_SPOTS_KEY] != null

            dataStore.edit { preferences ->
                preferences.remove(RECENT_CURRENT_LOCATION_SPOTS_KEY)
            }

            if (hadCache) {
                RecentCurrentLocationSpotCacheClearResult.Deleted
            } else {
                RecentCurrentLocationSpotCacheClearResult.NoCache
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            RecentCurrentLocationSpotCacheClearResult.Failed
        }
    }

    private companion object {
        val RECENT_CURRENT_LOCATION_SPOTS_KEY =
            stringPreferencesKey("recent_current_location_spots")
    }
}
