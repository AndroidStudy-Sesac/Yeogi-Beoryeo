package com.team.yeogibeoryeo.data.operationnotice.mapper

import com.team.yeogibeoryeo.data.operationnotice.remote.OperationNoticeDto
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeFeature
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OperationNoticeMapperTest {

    @Test
    fun `비활성 공지는 제외한다`() {
        val notice = dto(enabled = false).toDomainOrNull()

        assertNull(notice)
    }

    @Test
    fun `필수 문자열이 비어 있으면 공지를 제외한다`() {
        assertNull(dto(id = " ").toDomainOrNull())
        assertNull(dto(title = " ").toDomainOrNull())
        assertNull(dto(message = " ").toDomainOrNull())
    }

    @Test
    fun `지원하지 않는 severity는 공지를 제외한다`() {
        val notice = dto(severity = "notice").toDomainOrNull()

        assertNull(notice)
    }

    @Test
    fun `원본 affectedFeatures가 비어 있으면 홈 기본 공지로 매핑한다`() {
        val notice = dto(affectedFeatures = emptyList()).toDomainOrNull()

        assertEquals(emptySet<OperationNoticeFeature>(), notice?.affectedFeatures)
    }

    @Test
    fun `원본 affectedFeatures가 있지만 모두 지원하지 않으면 공지를 제외한다`() {
        val notice = dto(affectedFeatures = listOf("collection_spot_ma")).toDomainOrNull()

        assertNull(notice)
    }

    @Test
    fun `지원하는 affectedFeatures만 공지 대상 기능으로 매핑한다`() {
        val notice =
            dto(
                affectedFeatures = listOf(
                    "unknown",
                    "collection_spot_map",
                ),
            ).toDomainOrNull()

        assertEquals(setOf(OperationNoticeFeature.COLLECTION_SPOT_MAP), notice?.affectedFeatures)
    }

    @Test
    fun `startsAt 값이 있는데 파싱할 수 없으면 공지를 제외한다`() {
        val notice = dto(startsAt = "2026-08-10").toDomainOrNull()

        assertNull(notice)
    }

    @Test
    fun `endsAt 값이 있는데 파싱할 수 없으면 공지를 제외한다`() {
        val notice = dto(endsAt = "2026-08-10").toDomainOrNull()

        assertNull(notice)
    }

    @Test
    fun `허용하지 않는 scheme의 actionUrl은 action과 함께 제외한다`() {
        val notice =
            dto(
                actionLabel = "자세히 보기",
                actionUrl = "intent://notice",
            ).toDomainOrNull()

        assertNull(notice?.actionLabel)
        assertNull(notice?.actionUrl)
    }

    @Test
    fun `http와 https actionUrl은 유지한다`() {
        val notice =
            dto(
                actionLabel = "자세히 보기",
                actionUrl = "https://www.data.go.kr",
            ).toDomainOrNull()

        assertEquals("자세히 보기", notice?.actionLabel)
        assertEquals("https://www.data.go.kr", notice?.actionUrl)
    }

    @Test
    fun `actionLabel과 actionUrl 중 하나만 있으면 action을 제외한다`() {
        val labelOnlyNotice = dto(actionLabel = "자세히 보기", actionUrl = null).toDomainOrNull()
        val urlOnlyNotice = dto(actionLabel = null, actionUrl = "https://www.data.go.kr").toDomainOrNull()

        assertNull(labelOnlyNotice?.actionLabel)
        assertNull(labelOnlyNotice?.actionUrl)
        assertNull(urlOnlyNotice?.actionLabel)
        assertNull(urlOnlyNotice?.actionUrl)
    }

    @Test
    fun `날짜 값이 비어 있으면 기간 제한 없이 매핑한다`() {
        val notice = dto(startsAt = " ", endsAt = null).toDomainOrNull()

        assertNull(notice?.startsAtMillis)
        assertNull(notice?.endsAtMillis)
    }

    private fun dto(
        id: String = "notice-id",
        enabled: Boolean = true,
        severity: String = OperationNoticeSeverity.WARNING.remoteValue,
        title: String = "운영 공지",
        message: String = "공지 내용",
        affectedFeatures: List<String> = listOf(OperationNoticeFeature.HOME.remoteValue),
        startsAt: String? = "2026-08-10T00:00:00+09:00",
        endsAt: String? = "2026-08-11T00:00:00+09:00",
        actionLabel: String? = null,
        actionUrl: String? = null,
    ): OperationNoticeDto =
        OperationNoticeDto(
            id = id,
            enabled = enabled,
            severity = severity,
            title = title,
            message = message,
            affectedFeatures = affectedFeatures,
            startsAt = startsAt,
            endsAt = endsAt,
            actionLabel = actionLabel,
            actionUrl = actionUrl,
        )
}
