package com.team.yeogibeoryeo.presentation.search

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemSearchScreenMetricsTest {
    @Test
    fun 홈은_창_폭에_따라_여백을_늘리고_검색_결과의_여백은_유지한다() {
        listOf(
            Triple(360.dp, 16.dp, 16.dp),
            Triple(384.dp, 16.dp, 16.dp),
            Triple(385.dp, 24.dp, 24.dp),
            Triple(599.dp, 24.dp, 24.dp),
            Triple(600.dp, 32.dp, 24.dp),
            Triple(720.dp, 32.dp, 24.dp),
            Triple(952.dp, 32.dp, 24.dp),
            Triple(1200.dp, 32.dp, 24.dp),
        ).forEach { (width, homePadding, searchPadding) ->
            listOf(800.dp, 1200.dp).forEach { height ->
                val metrics = itemSearchScreenMetricsSpec(width, height)

                assertEquals(homePadding, metrics.homeHorizontalPadding)
                assertEquals(searchPadding, metrics.horizontalPadding)
            }
        }
    }

    @Test
    fun 낮은_가로_창은_넓은_폭에서도_홈_여백을_24dp로_유지한다() {
        listOf(385.dp, 599.dp, 600.dp, 720.dp, 952.dp, 1200.dp).forEach { width ->
            val metrics = itemSearchScreenMetricsSpec(width, 320.dp)

            assertTrue(metrics.isCompactLandscape)
            assertEquals(24.dp, metrics.homeHorizontalPadding)
            assertEquals(24.dp, metrics.horizontalPadding)
        }
        val narrowMetrics = itemSearchScreenMetricsSpec(384.dp, 320.dp)
        assertEquals(16.dp, narrowMetrics.homeHorizontalPadding)
    }

    @Test
    fun 홈_여백은_가로_창_높이_480dp_경계에서_변경된다() {
        assertEquals(24.dp, itemSearchScreenMetricsSpec(900.dp, 480.dp).homeHorizontalPadding)
        assertEquals(32.dp, itemSearchScreenMetricsSpec(900.dp, 481.dp).homeHorizontalPadding)
        assertEquals(32.dp, itemSearchScreenMetricsSpec(900.dp, 900.dp).homeHorizontalPadding)
    }

    @Test
    fun `높이가 480dp 이하인 가로 화면은 compact landscape로 본다`() {
        listOf(
            480.dp to 320.dp,
            568.dp to 320.dp,
            640.dp to 360.dp,
            800.dp to 480.dp,
        ).forEach { (maxWidth, maxHeight) ->
            val metrics = itemSearchScreenMetricsSpec(
                maxWidth = maxWidth,
                maxHeight = maxHeight,
            )

            assertTrue(metrics.isCompactLandscape)
            assertEquals(
                ItemSearchLayoutDefaults.fraction.USEFUL_GUIDE_LANDSCAPE_BANNER_WIDTH,
                metrics.usefulGuideBannerWidthFraction,
            )
            assertEquals(ItemSearchLayoutDefaults.spacing.md, metrics.screenVerticalSpace)
        }
    }

    @Test
    fun `세로 화면이나 높이가 480dp보다 큰 가로 화면은 일반 간격을 유지한다`() {
        listOf(
            Triple(360.dp, 640.dp, ItemSearchLayoutDefaults.spacing.lg),
            Triple(800.dp, 481.dp, ItemSearchLayoutDefaults.spacing.xl),
            Triple(900.dp, 600.dp, ItemSearchLayoutDefaults.spacing.xl),
        ).forEach { (maxWidth, maxHeight, expectedScreenVerticalSpace) ->
            val metrics = itemSearchScreenMetricsSpec(
                maxWidth = maxWidth,
                maxHeight = maxHeight,
            )

            assertFalse(metrics.isCompactLandscape)
            assertEquals(
                ItemSearchLayoutDefaults.fraction.USEFUL_GUIDE_BANNER_WIDTH,
                metrics.usefulGuideBannerWidthFraction,
            )
            assertEquals(expectedScreenVerticalSpace, metrics.screenVerticalSpace)
        }
    }
}
