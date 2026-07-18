package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.common.design.theme.YeogiBeoryeoTheme
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QuickCategoryGridLabelLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 발포합성수지_라벨은_일반_글자_크기_넓은_폭에서_한_줄로_표시된다() {
        var lineCount: Int? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 1f)) {
                YeogiBeoryeoTheme {
                    val spec = quickCategoryGridMetricsSpec(maxWidth = 360.dp, fontScale = 1f)
                    CompositionLocalProvider(
                        LocalQuickCategoryGridMetrics provides spec.toMetrics(),
                    ) {
                        QuickCategoryItem(
                            category = RepresentativeGuideCategory.STYROFOAM,
                            name = RepresentativeGuideCategory.STYROFOAM.quickCategoryLabel,
                            onClick = {},
                            onLabelTextLayout = { lineCount = it.lineCount },
                        )
                    }
                }
            }
        }

        composeTestRule.waitUntil { lineCount != null }

        assertEquals(1, lineCount)
    }

    @Test
    fun 줄바꿈을_의도한_긴_라벨은_두_줄로_표시된다() {
        var lineCount: Int? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 1f)) {
                YeogiBeoryeoTheme {
                    val spec = quickCategoryGridMetricsSpec(maxWidth = 360.dp, fontScale = 1f)
                    CompositionLocalProvider(
                        LocalQuickCategoryGridMetrics provides spec.toMetrics(),
                    ) {
                        QuickCategoryItem(
                            category = RepresentativeGuideCategory.CONSTRUCTION_WASTE,
                            name = RepresentativeGuideCategory.CONSTRUCTION_WASTE.quickCategoryLabel,
                            onClick = {},
                            onLabelTextLayout = { lineCount = it.lineCount },
                        )
                    }
                }
            }
        }

        composeTestRule.waitUntil { lineCount != null }

        assertEquals(2, lineCount)
    }

    @Test
    fun 불연성종량제폐기물_라벨은_폭과_글자_크기_경계값에서_두_줄_이하로_표시된다() {
        val cases =
            listOf(
                LabelLineCountCase(maxWidthDp = 299, fontScale = 1.0f),
                LabelLineCountCase(maxWidthDp = 300, fontScale = 1.0f),
                LabelLineCountCase(maxWidthDp = 335, fontScale = 1.0f),
                LabelLineCountCase(maxWidthDp = 336, fontScale = 1.0f),
                LabelLineCountCase(maxWidthDp = 351, fontScale = 1.0f),
                LabelLineCountCase(maxWidthDp = 352, fontScale = 1.0f),
                LabelLineCountCase(maxWidthDp = 359, fontScale = 1.0f),
                LabelLineCountCase(maxWidthDp = 360, fontScale = 1.0f),
                LabelLineCountCase(maxWidthDp = 299, fontScale = 1.3f),
                LabelLineCountCase(maxWidthDp = 300, fontScale = 1.3f),
                LabelLineCountCase(maxWidthDp = 351, fontScale = 1.3f),
                LabelLineCountCase(maxWidthDp = 352, fontScale = 1.3f),
            )
        val lineCounts = mutableMapOf<LabelLineCountCase, Int>()

        composeTestRule.setContent {
            YeogiBeoryeoTheme {
                Column {
                    cases.forEach { case ->
                        CompositionLocalProvider(
                            LocalDensity provides Density(density = 1f, fontScale = case.fontScale),
                        ) {
                            val spec = quickCategoryGridMetricsSpec(
                                maxWidth = case.maxWidthDp.dp,
                                fontScale = case.fontScale,
                            )
                            CompositionLocalProvider(
                                LocalQuickCategoryGridMetrics provides spec.toMetrics(),
                            ) {
                                QuickCategoryItem(
                                    category = RepresentativeGuideCategory.NON_COMBUSTIBLE,
                                    name = RepresentativeGuideCategory.NON_COMBUSTIBLE.quickCategoryLabel,
                                    onClick = {},
                                    onLabelTextLayout = { lineCounts[case] = it.lineCount },
                                )
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.waitUntil { cases.all { it in lineCounts } }

        cases.forEach { case ->
            val lineCount = lineCounts.getValue(case)
            assertTrue(
                "case=$case expected lineCount <= 2 but was $lineCount",
                lineCount <= 2,
            )
        }
    }

    @Composable
    private fun QuickCategoryGridMetricsSpec.toMetrics(): QuickCategoryGridMetrics =
        QuickCategoryGridMetrics(
            columnCount = columnCount,
            cellSize = cellSize,
            tileSize = tileSize,
            iconSize = iconSize,
            verticalSpace = verticalSpace,
            labelSpacing = labelSpacing,
            labelTextStyle = when (labelTextStyleType) {
                QuickCategoryGridLabelTextStyleType.BodyLarge -> MaterialTheme.typography.bodyLarge
                QuickCategoryGridLabelTextStyleType.LabelLarge -> MaterialTheme.typography.labelLarge
            },
        )

    private data class LabelLineCountCase(
        val maxWidthDp: Int,
        val fontScale: Float,
    )
}
