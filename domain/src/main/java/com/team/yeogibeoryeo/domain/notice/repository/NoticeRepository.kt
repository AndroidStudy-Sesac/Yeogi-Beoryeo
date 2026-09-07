package com.team.yeogibeoryeo.domain.notice.repository

import com.team.yeogibeoryeo.domain.notice.model.Notice

interface NoticeRepository {
    suspend fun getPublishedNotices(): List<Notice>

    suspend fun getReadNoticeIds(): Set<String>

    suspend fun markNoticeRead(noticeId: String)
}
