package com.team.yeogibeoryeo.data.notice.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.team.yeogibeoryeo.data.notice.di.NoticePreferencesDataStore
import com.team.yeogibeoryeo.data.notice.mapper.NoticeMapper
import com.team.yeogibeoryeo.data.notice.remote.NoticeRemoteDataSource
import com.team.yeogibeoryeo.domain.notice.model.Notice
import com.team.yeogibeoryeo.domain.notice.repository.NoticeRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class NoticeRepositoryImpl @Inject constructor(
    private val remoteDataSource: NoticeRemoteDataSource,
    @param:NoticePreferencesDataStore private val dataStore: DataStore<Preferences>,
) : NoticeRepository {
    override suspend fun getPublishedNotices(): List<Notice> {
        return remoteDataSource.fetchPublishedNotices()
            .mapNotNull(NoticeMapper::mapToDomainOrNull)
            .sortedByDescending(Notice::publishedAtMillis)
    }

    override suspend fun getReadNoticeIds(): Set<String> =
        dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                emit(emptyPreferences())
            }
            .map { preferences -> preferences[READ_NOTICE_IDS_KEY].orEmpty() }
            .first()

    override suspend fun markNoticeRead(noticeId: String) {
        val normalizedId = noticeId.trim()
        if (normalizedId.isEmpty()) return

        try {
            dataStore.edit { preferences ->
                preferences[READ_NOTICE_IDS_KEY] =
                    preferences[READ_NOTICE_IDS_KEY].orEmpty() + normalizedId
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            // 저장 실패 시 다음 실행에서 다시 미확인 상태로 표시한다.
        }
    }

    private companion object {
        val READ_NOTICE_IDS_KEY = stringSetPreferencesKey("read_notice_ids")
    }
}
