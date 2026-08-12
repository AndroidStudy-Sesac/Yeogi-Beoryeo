package com.team.yeogibeoryeo.domain.operationnotice.policy

import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNotice
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeFeature
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeSeverity
import org.junit.Assert.assertEquals
import org.junit.Test

class OperationNoticeDisplayPolicyTest {
    private val policy = OperationNoticeDisplayPolicy()

    @Test
    fun 닫은_일반_공지는_표시하지_않는다() {
        val result =
            policy.visibleNotices(
                notices = listOf(공지(id = "닫은공지"), 공지(id = "표시공지")),
                feature = OperationNoticeFeature.HOME,
                dismissedNoticeIds = setOf("닫은공지"),
            )

        assertEquals(listOf("표시공지"), result.map(OperationNotice::id))
    }

    @Test
    fun 새_공지_식별자는_기존_닫힘_상태와_관계없이_표시한다() {
        val result =
            policy.visibleNotices(
                notices = listOf(공지(id = "새공지")),
                feature = OperationNoticeFeature.HOME,
                dismissedNoticeIds = setOf("이전공지"),
            )

        assertEquals(listOf("새공지"), result.map(OperationNotice::id))
    }

    @Test
    fun 위험_공지는_닫힘_상태여도_표시한다() {
        val result =
            policy.visibleNotices(
                notices = listOf(공지(id = "긴급공지", severity = OperationNoticeSeverity.CRITICAL)),
                feature = OperationNoticeFeature.HOME,
                dismissedNoticeIds = setOf("긴급공지"),
            )

        assertEquals(listOf("긴급공지"), result.map(OperationNotice::id))
    }

    @Test
    fun 영향_기능이_아닌_화면에는_공지하지_않는다() {
        val result =
            policy.visibleNotices(
                notices =
                    listOf(
                        공지(
                            id = "지도공지",
                            affectedFeatures = setOf(OperationNoticeFeature.COLLECTION_SPOT_MAP),
                        ),
                    ),
                feature = OperationNoticeFeature.HOME,
                dismissedNoticeIds = emptySet(),
            )

        assertEquals(emptyList<String>(), result.map(OperationNotice::id))
    }

    @Test
    fun 심각도와_우선순위에_따라_공지를_정렬한다() {
        val result =
            policy.visibleNotices(
                notices =
                    listOf(
                        공지(id = "정보", severity = OperationNoticeSeverity.INFO, priority = 100),
                        공지(id = "경고낮음", severity = OperationNoticeSeverity.WARNING, priority = 1),
                        공지(id = "경고높음", severity = OperationNoticeSeverity.WARNING, priority = 2),
                    ),
                feature = OperationNoticeFeature.HOME,
                dismissedNoticeIds = emptySet(),
            )

        assertEquals(listOf("경고높음", "경고낮음", "정보"), result.map(OperationNotice::id))
    }

    private fun 공지(
        id: String,
        severity: OperationNoticeSeverity = OperationNoticeSeverity.INFO,
        priority: Int = 0,
        affectedFeatures: Set<OperationNoticeFeature> = emptySet(),
    ): OperationNotice =
        OperationNotice(
            id = id,
            severity = severity,
            priority = priority,
            title = "운영 공지",
            message = "운영 공지 내용",
            affectedFeatures = affectedFeatures,
            startsAtMillis = null,
            endsAtMillis = null,
            minVersionCode = null,
            maxVersionCode = null,
            actionLabel = null,
            actionUrl = null,
        )
}
