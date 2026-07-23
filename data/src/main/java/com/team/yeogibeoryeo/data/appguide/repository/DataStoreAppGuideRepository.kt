package com.team.yeogibeoryeo.data.appguide.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.team.yeogibeoryeo.data.appguide.di.AppGuidePreferencesDataStore
import com.team.yeogibeoryeo.domain.appguide.repository.AppGuideRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreAppGuideRepository
    @Inject
    constructor(
        @param:AppGuidePreferencesDataStore private val dataStore: DataStore<Preferences>,
    ) : AppGuideRepository {
        override fun observeCompletedVersion(): Flow<Int> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { preferences ->
                    preferences[COMPLETED_VERSION_KEY] ?: NOT_COMPLETED_VERSION
                }

        override suspend fun markCompleted(version: Int) {
            try {
                dataStore.edit { preferences ->
                    val completedVersion = preferences[COMPLETED_VERSION_KEY] ?: NOT_COMPLETED_VERSION
                    preferences[COMPLETED_VERSION_KEY] = maxOf(completedVersion, version)
                }
            } catch (_: IOException) {
                // 저장 실패 시 다음 실행에서 가이드를 다시 보여줍니다.
            }
        }

        override fun observeCompletedMapLocationGuideVersion(): Flow<Int> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { preferences ->
                    preferences[COMPLETED_MAP_LOCATION_GUIDE_VERSION_KEY] ?: NOT_COMPLETED_VERSION
                }

        override suspend fun markMapLocationGuideCompleted(version: Int) {
            try {
                dataStore.edit { preferences ->
                    val completedVersion =
                        preferences[COMPLETED_MAP_LOCATION_GUIDE_VERSION_KEY] ?: NOT_COMPLETED_VERSION
                    preferences[COMPLETED_MAP_LOCATION_GUIDE_VERSION_KEY] = maxOf(completedVersion, version)
                }
            } catch (_: IOException) {
                // 저장 실패 시 다음 실행에서 가이드를 다시 보여줍니다.
            }
        }

        private companion object {
            const val NOT_COMPLETED_VERSION = 0
            val COMPLETED_VERSION_KEY = intPreferencesKey("completed_app_guide_version")
            val COMPLETED_MAP_LOCATION_GUIDE_VERSION_KEY =
                intPreferencesKey("completed_map_location_guide_version")
        }
    }
