package com.team.yeogibeoryeo.data.notice.mapper

import com.team.yeogibeoryeo.data.notice.remote.dto.NoticeDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoticeMapperTest {
    @Test
    fun `공지 DTO의 모든 값을 도메인 모델로 변환한다`() {
        val dto = noticeDto()

        val notice = NoticeMapper.mapToDomainOrNull(dto)

        requireNotNull(notice)
        assertEquals(dto.id, notice.id)
        assertEquals(dto.title, notice.title)
        assertEquals(dto.body, notice.body)
        assertEquals(dto.publishedAtMillis, notice.publishedAtMillis)
        assertEquals(dto.updatedAtMillis, notice.updatedAtMillis)
    }

    @Test
    fun `수정 시각이 없는 공지도 변환한다`() {
        val notice = NoticeMapper.mapToDomainOrNull(
            noticeDto().copy(updatedAtMillis = null),
        )

        requireNotNull(notice)
        assertNull(notice.updatedAtMillis)
    }

    @Test
    fun `필수 내용이 없는 공지는 제외한다`() {
        val notices = listOf(
            noticeDto().copy(title = null),
            noticeDto().copy(title = ""),
            noticeDto().copy(body = null),
            noticeDto().copy(body = ""),
            noticeDto().copy(publishedAtMillis = null),
        )

        notices.forEach { dto ->
            assertNull(NoticeMapper.mapToDomainOrNull(dto))
        }
    }

    private fun noticeDto(): NoticeDto {
        return NoticeDto(
            id = "service-update",
            title = "서비스 업데이트 안내",
            body = "새 기능을 안내합니다.",
            publishedAtMillis = 1_754_000_000_000,
            updatedAtMillis = 1_754_000_100_000,
        )
    }
}
