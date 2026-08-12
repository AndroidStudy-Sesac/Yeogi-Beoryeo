package com.team.yeogibeoryeo.data.notice.repository

import com.team.yeogibeoryeo.data.notice.mapper.NoticeMapper
import com.team.yeogibeoryeo.data.notice.remote.NoticeRemoteDataSource
import com.team.yeogibeoryeo.domain.notice.model.Notice
import com.team.yeogibeoryeo.domain.notice.repository.NoticeRepository
import javax.inject.Inject

class NoticeRepositoryImpl @Inject constructor(
    private val remoteDataSource: NoticeRemoteDataSource,
) : NoticeRepository {
    override suspend fun getPublishedNotices(): List<Notice> {
        return remoteDataSource.fetchPublishedNotices()
            .mapNotNull(NoticeMapper::mapToDomainOrNull)
            .sortedByDescending(Notice::publishedAtMillis)
    }
}
