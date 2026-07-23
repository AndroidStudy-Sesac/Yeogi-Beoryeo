package com.team.yeogibeoryeo.data.appguide.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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

        override fun observeHasRequestedMapLocationPermission(): Flow<Boolean> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { preferences ->
                    preferences[HAS_REQUESTED_MAP_LOCATION_PERMISSION_KEY] ?: false
                }

        override suspend fun markMapLocationPermissionRequested() {
            try {
                dataStore.edit { preferences ->
                    preferences[HAS_REQUESTED_MAP_LOCATION_PERMISSION_KEY] = true
                }
            } catch (_: IOException) {
                // 저장 실패 시 다음 실행에서 시스템 권한 상태를 기준으로 다시 판단합니다.
            }
        }

        override fun observeIsMapLocationPermissionBlocked(): Flow<Boolean> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { preferences ->
                    preferences[IS_MAP_LOCATION_PERMISSION_BLOCKED_KEY] ?: false
                }

        override suspend fun markMapLocationPermissionBlocked() {
            try {
                dataStore.edit { preferences ->
                    preferences[IS_MAP_LOCATION_PERMISSION_BLOCKED_KEY] = true
                }
            } catch (_: IOException) {
                // 저장 실패 시 현재 세션의 권한 요청 결과만 반영합니다.
            }
        }

        override suspend fun clearMapLocationPermissionBlocked() {
            try {
                dataStore.edit { preferences ->
                    preferences[IS_MAP_LOCATION_PERMISSION_BLOCKED_KEY] = false
                }
            } catch (_: IOException) {
                // 저장 실패 시 다음 실행에서 기존 blocked 상태가 유지될 수 있습니다.
            }
        }

        private companion object {
            const val NOT_COMPLETED_VERSION = 0
            val COMPLETED_VERSION_KEY = intPreferencesKey("completed_app_guide_version")
            val COMPLETED_MAP_LOCATION_GUIDE_VERSION_KEY =
                intPreferencesKey("completed_map_location_guide_version")
            val HAS_REQUESTED_MAP_LOCATION_PERMISSION_KEY =
                booleanPreferencesKey("has_requested_map_location_permission")
            val IS_MAP_LOCATION_PERMISSION_BLOCKED_KEY =
                booleanPreferencesKey("is_map_location_permission_blocked")
        }
    }
