package com.team.yeogibeoryeo.presentation.theme

import android.graphics.Typeface
import androidx.compose.ui.text.font.FontWeight
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.team.yeogibeoryeo.common.R
import com.team.yeogibeoryeo.common.design.theme.Typography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PretendardFontTest {
    @Test
    fun 유지한_글꼴_파일이_기대하는_굵기를_가진다() {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val expectedWeights = mapOf(
            R.font.pretendard_regular to FontWeight.Normal.weight,
            R.font.pretendard_medium to FontWeight.Medium.weight,
            R.font.pretendard_semibold to FontWeight.SemiBold.weight,
            R.font.pretendard_bold to FontWeight.Bold.weight,
            R.font.pretendard_extrabold to FontWeight.ExtraBold.weight,
        )

        expectedWeights.forEach { (fontResource, expectedWeight) ->
            val typeface = resources.getFont(fontResource)

            assertEquals(expectedWeight, typeface.weight)
            assertEquals(Typeface.NORMAL, typeface.style and Typeface.ITALIC)
        }
    }

    @Test
    fun 기본_타이포그래피가_유지한_글꼴_굵기만_사용한다() {
        val retainedWeights = setOf(FontWeight.Normal, FontWeight.Medium)
        val typographyWeights = listOf(
            Typography.displayLarge,
            Typography.displayMedium,
            Typography.displaySmall,
            Typography.headlineLarge,
            Typography.headlineMedium,
            Typography.headlineSmall,
            Typography.titleLarge,
            Typography.titleMedium,
            Typography.titleSmall,
            Typography.bodyLarge,
            Typography.bodyMedium,
            Typography.bodySmall,
            Typography.labelLarge,
            Typography.labelMedium,
            Typography.labelSmall,
        ).map { it.fontWeight }

        assertTrue(typographyWeights.all(retainedWeights::contains))
    }
}
