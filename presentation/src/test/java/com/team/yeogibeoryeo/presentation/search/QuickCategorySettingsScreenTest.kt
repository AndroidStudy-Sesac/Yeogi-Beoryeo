package com.team.yeogibeoryeo.presentation.search

import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickCategorySettingsScreenTest {
    @Test
    fun 존재하지_않는_검색어는_빈_목록을_반환한다() {
        val result = filterQuickCategorySettingsCategories("zzznotfound")

        assertTrue(result.isEmpty())
    }

    @Test
    fun 분류명과_대표_가이드명으로_검색할_수_있다() {
        assertEquals(
            listOf(RepresentativeGuideCategory.VINYL),
            filterQuickCategorySettingsCategories("비닐류"),
        )
        assertEquals(
            listOf(RepresentativeGuideCategory.ELECTRONICS),
            filterQuickCategorySettingsCategories("전기전자제품"),
        )
    }
}
