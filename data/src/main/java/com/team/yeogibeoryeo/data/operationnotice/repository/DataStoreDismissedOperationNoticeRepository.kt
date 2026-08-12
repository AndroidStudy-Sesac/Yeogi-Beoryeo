package com.team.yeogibeoryeo.data.operationnotice.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.team.yeogibeoryeo.data.operationnotice.di.OperationNoticePreferencesDataStore
import com.team.yeogibeoryeo.domain.operationnotice.repository.DismissedOperationNoticeRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreDismissedOperationNoticeRepository
@Inject
constructor(
    @param:OperationNoticePreferencesDataStore private val dataStore: DataStore<Preferences>,
) : DismissedOperationNoticeRepository {

    override fun observeDismissedNoticeIds(): Flow<Set<String>> =
        dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                emit(emptyPreferences())
            }
            .map { preferences -> preferences[DISMISSED_NOTICE_IDS_KEY].orEmpty() }

    override suspend fun dismissNotice(id: String) {
        val normalizedId = id.trim()
        if (normalizedId.isEmpty()) return

        try {
            dataStore.edit { preferences ->
                preferences[DISMISSED_NOTICE_IDS_KEY] =
                    preferences[DISMISSED_NOTICE_IDS_KEY].orEmpty() + normalizedId
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            // 저장 실패 시 다음 노출 정책은 기존 상태를 유지한다.
        }
    }

    private companion object {
        val DISMISSED_NOTICE_IDS_KEY = stringSetPreferencesKey("dismissed_operation_notice_ids")
    }
}

