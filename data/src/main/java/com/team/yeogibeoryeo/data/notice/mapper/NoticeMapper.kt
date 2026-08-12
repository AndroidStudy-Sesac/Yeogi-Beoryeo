package com.team.yeogibeoryeo.data.notice.mapper

import com.team.yeogibeoryeo.data.notice.remote.dto.NoticeDto
import com.team.yeogibeoryeo.domain.notice.model.Notice

internal object NoticeMapper {
    fun mapToDomainOrNull(dto: NoticeDto): Notice? {
        val title = dto.title?.takeIf(String::isNotBlank) ?: return null
        val body = dto.body?.takeIf(String::isNotBlank) ?: return null
        val publishedAtMillis = dto.publishedAtMillis ?: return null

        return Notice(
            id = dto.id,
            title = title,
            body = body,
            publishedAtMillis = publishedAtMillis,
            updatedAtMillis = dto.updatedAtMillis,
        )
    }
}
