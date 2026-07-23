package com.team.yeogibeoryeo.presentation.regionalguide

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionalGuideScreenMetricsTest {
    @Test
    fun `높이가 480dp 이하인 가로 화면은 압축 레이아웃을 사용한다`() {
        val metrics = regionalGuideScreenMetricsSpec(
            maxWidth = 640.dp,
            maxHeight = 360.dp,
        )

        assertTrue(metrics.isCompactLandscape)
        assertEquals(16.dp, metrics.horizontalPadding)
        assertEquals(12.dp, metrics.topPadding)
        assertEquals(115.2.dp, metrics.candidateListMaxHeight)
    }

    @Test
    fun `세로 화면과 높이가 큰 가로 화면은 기존 레이아웃을 유지한다`() {
        listOf(
            360.dp to 640.dp,
            800.dp to 481.dp,
        ).forEach { (maxWidth, maxHeight) ->
            val metrics = regionalGuideScreenMetricsSpec(
                maxWidth = maxWidth,
                maxHeight = maxHeight,
            )

            assertFalse(metrics.isCompactLandscape)
            assertEquals(20.dp, metrics.horizontalPadding)
            assertEquals(20.dp, metrics.topPadding)
            assertEquals(260.dp, metrics.candidateListMaxHeight)
        }
    }
}
