package com.team.yeogibeoryeo.data.spot.cache

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalApi
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalCategory
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorContext
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorReporter
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalStage
import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheClearResult
import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheEntry
import com.team.yeogibeoryeo.domain.spot.repository.RecentCurrentLocationSpotCacheRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class DataStoreRecentCurrentLocationSpotCacheRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val reporter: NonFatalErrorReporter,
) : RecentCurrentLocationSpotCacheRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun getRecentCurrentLocationSpots(): RecentCurrentLocationSpotCacheEntry? {
        return try {
            val cacheJson = dataStore.data.first()[RECENT_CURRENT_LOCATION_SPOTS_KEY] ?: return null

            json.decodeFromString<RecentCurrentLocationSpotCacheDto>(cacheJson).toDomain()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            reporter.reportCacheFailure(exception, NonFatalStage.CACHE_READ)
            if (exception is SerializationException || exception is IllegalArgumentException) {
                clearRecentCurrentLocationSpots()
            }
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
            reporter.reportCacheFailure(exception, NonFatalStage.CACHE_WRITE)
            // Cache writes must not fail the current location search success flow.
        }
    }

    override suspend fun clearRecentCurrentLocationSpots(): RecentCurrentLocationSpotCacheClearResult {
        return try {
            var hadCache = false

            dataStore.edit { preferences ->
                hadCache = preferences[RECENT_CURRENT_LOCATION_SPOTS_KEY] != null
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
            reporter.reportCacheFailure(exception, NonFatalStage.CACHE_WRITE)
            RecentCurrentLocationSpotCacheClearResult.Failed
        }
    }

    private companion object {
        val RECENT_CURRENT_LOCATION_SPOTS_KEY =
            stringPreferencesKey("recent_current_location_spots")
    }
}

private fun NonFatalErrorReporter.reportCacheFailure(
    error: Throwable,
    stage: NonFatalStage,
) {
    val category = when (error) {
        is SerializationException,
        is IllegalArgumentException,
        -> NonFatalCategory.PARSING
        else -> NonFatalCategory.CACHE
    }

    report(
        error = error,
        context = NonFatalErrorContext(
            api = NonFatalApi.COLLECTION_SPOT,
            stage = stage,
            category = category,
        ),
    )
}
