package com.team.yeogibeoryeo.data.regionalguide.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.team.yeogibeoryeo.data.regionalguide.di.RegionalGuidePreferencesDataStore
import com.team.yeogibeoryeo.domain.regionalguide.repository.HomeRegionalGuidePrimaryFavoriteRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreHomeRegionalGuidePrimaryFavoriteRepository
    @Inject
    constructor(
        @param:RegionalGuidePreferencesDataStore private val dataStore: DataStore<Preferences>,
    ) : HomeRegionalGuidePrimaryFavoriteRepository {
        override fun observePrimaryFavoriteTargetId(): Flow<String?> =
            dataStore.data
                .catch { exception ->
                    if (exception is CancellationException) throw exception
                    emit(emptyPreferences())
                }
                .map { preferences ->
                    preferences[HOME_REGIONAL_GUIDE_PRIMARY_FAVORITE_TARGET_ID_KEY]
                        ?.trim()
                        ?.takeIf { targetId -> targetId.isNotBlank() }
                }

        override fun observeLastSelectedFavoriteTargetId(): Flow<String?> =
            dataStore.data
                .catch { exception ->
                    if (exception is CancellationException) throw exception
                    emit(emptyPreferences())
                }
                .map { preferences ->
                    preferences[HOME_REGIONAL_GUIDE_LAST_SELECTED_FAVORITE_TARGET_ID_KEY]
                        ?.trim()
                        ?.takeIf { targetId -> targetId.isNotBlank() }
                }

        override suspend fun setPrimaryFavoriteTargetId(targetId: String) {
            val trimmedTargetId = targetId.trim()
            if (trimmedTargetId.isBlank()) return

            dataStore.edit { preferences ->
                preferences[HOME_REGIONAL_GUIDE_PRIMARY_FAVORITE_TARGET_ID_KEY] = trimmedTargetId
            }
        }

        override suspend fun clearPrimaryFavoriteTargetId() {
            dataStore.edit { preferences ->
                preferences.remove(HOME_REGIONAL_GUIDE_PRIMARY_FAVORITE_TARGET_ID_KEY)
            }
        }

        override suspend fun clearPrimaryFavoriteTargetIdIfMatches(targetId: String) {
            dataStore.edit { preferences ->
                if (preferences[HOME_REGIONAL_GUIDE_PRIMARY_FAVORITE_TARGET_ID_KEY] == targetId) {
                    preferences.remove(HOME_REGIONAL_GUIDE_PRIMARY_FAVORITE_TARGET_ID_KEY)
                }
            }
        }

        override suspend fun setLastSelectedFavoriteTargetId(targetId: String) {
            val trimmedTargetId = targetId.trim()
            if (trimmedTargetId.isBlank()) return

            dataStore.edit { preferences ->
                preferences[HOME_REGIONAL_GUIDE_LAST_SELECTED_FAVORITE_TARGET_ID_KEY] = trimmedTargetId
            }
        }

        override suspend fun clearLastSelectedFavoriteTargetId() {
            dataStore.edit { preferences ->
                preferences.remove(HOME_REGIONAL_GUIDE_LAST_SELECTED_FAVORITE_TARGET_ID_KEY)
            }
        }

        override suspend fun clearLastSelectedFavoriteTargetIdIfMatches(targetId: String) {
            dataStore.edit { preferences ->
                if (preferences[HOME_REGIONAL_GUIDE_LAST_SELECTED_FAVORITE_TARGET_ID_KEY] == targetId) {
                    preferences.remove(HOME_REGIONAL_GUIDE_LAST_SELECTED_FAVORITE_TARGET_ID_KEY)
                }
            }
        }

        override suspend fun clearPrimaryAndLastSelectedFavoriteTargetIdsIfMatches(
            targetIds: Collection<String>,
        ) {
            val targetIdSet =
                targetIds
                    .mapNotNull { targetId -> targetId.trim().takeIf { it.isNotBlank() } }
                    .toSet()
            if (targetIdSet.isEmpty()) return

            dataStore.edit { preferences ->
                if (preferences[HOME_REGIONAL_GUIDE_PRIMARY_FAVORITE_TARGET_ID_KEY] in targetIdSet) {
                    preferences.remove(HOME_REGIONAL_GUIDE_PRIMARY_FAVORITE_TARGET_ID_KEY)
                }
                if (preferences[HOME_REGIONAL_GUIDE_LAST_SELECTED_FAVORITE_TARGET_ID_KEY] in targetIdSet) {
                    preferences.remove(HOME_REGIONAL_GUIDE_LAST_SELECTED_FAVORITE_TARGET_ID_KEY)
                }
            }
        }

        private companion object {
            val HOME_REGIONAL_GUIDE_PRIMARY_FAVORITE_TARGET_ID_KEY =
                stringPreferencesKey("home_regional_guide_primary_favorite_target_id")
            val HOME_REGIONAL_GUIDE_LAST_SELECTED_FAVORITE_TARGET_ID_KEY =
                stringPreferencesKey("home_regional_guide_last_selected_favorite_target_id")
        }
    }
