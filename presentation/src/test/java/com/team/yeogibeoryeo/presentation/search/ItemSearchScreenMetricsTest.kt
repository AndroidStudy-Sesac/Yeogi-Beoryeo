package com.team.yeogibeoryeo.presentation.search

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemSearchScreenMetricsTest {
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
