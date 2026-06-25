package com.team.yeogibeoryeo.presentation.search.components

import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickCategoryGridOrderTest {

    @Test
    fun `홈 표시 분류를 기존 순서보다 앞에 배치한다`() {
        val categories =
            orderedQuickCategories(
                listOf(
                    RepresentativeGuideCategory.ELECTRONICS,
                    RepresentativeGuideCategory.BATTERY,
                ),
            )

        assertEquals(RepresentativeGuideCategory.ELECTRONICS, categories[0])
        assertEquals(RepresentativeGuideCategory.BATTERY, categories[1])
        assertEquals(quickCategoryOrder.size, categories.size)
    }

    @Test
    fun `중복 홈 표시 분류는 한 번만 배치한다`() {
        val categories =
            orderedQuickCategories(
                listOf(
                    RepresentativeGuideCategory.BATTERY,
                    RepresentativeGuideCategory.BATTERY,
                ),
            )

        assertEquals(RepresentativeGuideCategory.BATTERY, categories[0])
        assertEquals(1, categories.count { it == RepresentativeGuideCategory.BATTERY })
        assertEquals(quickCategoryOrder.size, categories.size)
    }
}
