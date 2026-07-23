package com.team.yeogibeoryeo.appguide

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class AppGuideGeometryTest {
    @Test
    fun `강조 영역을 넓힐 때 화면 경계를 넘지 않는다`() {
        val bounds = expandedSpotlightBounds(
            targetBounds = Rect(2f, 4f, 98f, 96f),
            paddingPx = 8f,
            containerSize = IntSize(100, 100),
        )

        assertEquals(Rect(0f, 0f, 100f, 100f), bounds)
    }

    @Test
    fun `대상 아래 공간이 충분하면 안내 카드를 아래에 배치한다`() {
        val offset = calculateGuideCardOffset(
            targetBounds = Rect(100f, 100f, 300f, 200f),
            cardSize = IntSize(200, 160),
            containerSize = IntSize(400, 800),
            marginPx = 16,
            targetGapPx = 12,
            safeTopPx = 24,
            safeBottomPx = 32,
        )

        assertEquals(IntOffset(100, 212), offset)
    }

    @Test
    fun `대상 아래 공간이 부족하면 안내 카드를 위에 배치한다`() {
        val offset = calculateGuideCardOffset(
            targetBounds = Rect(100f, 650f, 300f, 730f),
            cardSize = IntSize(200, 180),
            containerSize = IntSize(400, 800),
            marginPx = 16,
            targetGapPx = 12,
            safeTopPx = 24,
            safeBottomPx = 32,
        )

        assertEquals(IntOffset(100, 458), offset)
    }
}
