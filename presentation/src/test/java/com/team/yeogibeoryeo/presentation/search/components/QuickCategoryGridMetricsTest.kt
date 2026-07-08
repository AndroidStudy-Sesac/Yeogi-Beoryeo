package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickCategoryGridMetricsTest {
    @Test
    fun `일반 글자 크기에서는 300dp 미만만 좁은 폭 기준을 적용한다`() {
        val belowNarrow = quickCategoryGridMetricsSpec(
            maxWidth = 299.dp,
            fontScale = 1.0f,
        )
        val narrow = quickCategoryGridMetricsSpec(
            maxWidth = 300.dp,
            fontScale = 1.0f,
        )

        assertEquals(3, belowNarrow.columnCount)
        assertEquals(80.dp, belowNarrow.cellSize)
        assertEquals(72.dp, belowNarrow.tileSize)
        assertEquals(36.dp, belowNarrow.iconSize)

        assertEquals(3, narrow.columnCount)
        assertEquals(80.dp, narrow.cellSize)
        assertEquals(56.dp, narrow.tileSize)
        assertEquals(28.dp, narrow.iconSize)
        assertEquals(QuickCategoryGridLabelTextStyleType.LabelLarge, narrow.labelTextStyleType)
    }

    @Test
    fun `일반 글자 크기에서는 336dp부터 4열 wide phone 기준을 적용한다`() {
        val compact = quickCategoryGridMetricsSpec(
            maxWidth = 335.dp,
            fontScale = 1.0f,
        )
        val wide = quickCategoryGridMetricsSpec(
            maxWidth = 336.dp,
            fontScale = 1.0f,
        )

        assertEquals(4, compact.columnCount)
        assertEquals(80.dp, compact.cellSize)
        assertEquals(QuickCategoryGridLabelTextStyleType.LabelLarge, compact.labelTextStyleType)

        assertEquals(4, wide.columnCount)
        assertEquals(84.dp, wide.cellSize)
        assertEquals(64.dp, wide.tileSize)
        assertEquals(32.dp, wide.iconSize)
        assertEquals(QuickCategoryGridLabelTextStyleType.BodyLarge, wide.labelTextStyleType)
    }

    @Test
    fun `일반 글자 크기에서는 352dp와 360dp에서 넓은 폭 시각 기준을 단계적으로 키운다`() {
        val wide = quickCategoryGridMetricsSpec(
            maxWidth = 351.dp,
            fontScale = 1.0f,
        )
        val largePhone = quickCategoryGridMetricsSpec(
            maxWidth = 352.dp,
            fontScale = 1.0f,
        )
        val expanded = quickCategoryGridMetricsSpec(
            maxWidth = 360.dp,
            fontScale = 1.0f,
        )

        assertEquals(4, wide.columnCount)
        assertEquals(84.dp, wide.cellSize)

        assertEquals(4, largePhone.columnCount)
        assertEquals(88.dp, largePhone.cellSize)

        assertEquals(4, expanded.columnCount)
        assertEquals(90.dp, expanded.cellSize)
    }

    @Test
    fun `일반 글자 크기에서는 가로모드 폭에 맞춰 최대 6열까지 늘린다`() {
        mapOf(
            449.dp to 4,
            450.dp to 5,
            559.dp to 5,
            560.dp to 6,
            800.dp to 6,
        ).forEach { (maxWidth, expectedColumnCount) ->
            val metrics = quickCategoryGridMetricsSpec(
                maxWidth = maxWidth,
                fontScale = 1.0f,
            )

            assertEquals(expectedColumnCount, metrics.columnCount)
        }
    }

    @Test
    fun `큰 글자 크기에서는 font scale 1_3부터 최대 3열로 제한한다`() {
        val normalFont = quickCategoryGridMetricsSpec(
            maxWidth = 320.dp,
            fontScale = 1.29f,
        )
        val largeFont = quickCategoryGridMetricsSpec(
            maxWidth = 320.dp,
            fontScale = 1.3f,
        )

        assertEquals(4, normalFont.columnCount)
        assertEquals(80.dp, normalFont.cellSize)

        assertEquals(3, largeFont.columnCount)
        assertEquals(96.dp, largeFont.cellSize)
        assertEquals(72.dp, largeFont.tileSize)
        assertEquals(36.dp, largeFont.iconSize)
    }

    @Test
    fun `큰 글자 크기의 좁은 폭은 최대 2열로 제한한다`() {
        val narrowLargeFont = quickCategoryGridMetricsSpec(
            maxWidth = 299.dp,
            fontScale = 1.3f,
        )

        assertEquals(2, narrowLargeFont.columnCount)
        assertEquals(128.dp, narrowLargeFont.cellSize)
        assertEquals(72.dp, narrowLargeFont.tileSize)
        assertEquals(36.dp, narrowLargeFont.iconSize)
    }

    @Test
    fun `큰 글자 크기에서는 352dp부터 넓은 폭 cell 기준을 적용한다`() {
        val largeFont = quickCategoryGridMetricsSpec(
            maxWidth = 351.dp,
            fontScale = 1.3f,
        )
        val wideLargeFont = quickCategoryGridMetricsSpec(
            maxWidth = 352.dp,
            fontScale = 1.3f,
        )

        assertEquals(3, largeFont.columnCount)
        assertEquals(96.dp, largeFont.cellSize)

        assertEquals(3, wideLargeFont.columnCount)
        assertEquals(112.dp, wideLargeFont.cellSize)
    }

    @Test
    fun `큰 글자 크기에서도 가로모드 폭에 맞춰 최대 6열까지 늘린다`() {
        listOf(1.3f, 1.5f).forEach { fontScale ->
            mapOf(
                449.dp to 3,
                450.dp to 4,
                560.dp to 5,
                800.dp to 6,
            ).forEach { (maxWidth, expectedColumnCount) ->
                val metrics = quickCategoryGridMetricsSpec(
                    maxWidth = maxWidth,
                    fontScale = fontScale,
                )

                assertEquals(expectedColumnCount, metrics.columnCount)
            }
        }
    }
}
