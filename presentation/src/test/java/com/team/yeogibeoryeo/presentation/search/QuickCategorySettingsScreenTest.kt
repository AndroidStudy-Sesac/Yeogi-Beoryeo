package com.team.yeogibeoryeo.presentation.search

import com.team.yeogibeoryeo.presentation.search.components.quickCategoryOrder
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickCategorySettingsScreenTest {
    @Test
    fun 전지와_건전지_및_부분_별칭으로_같은_분류를_찾는다() {
        for (keyword in listOf("전지", "건전지", "건전", "건 전 지")) {
            assertEquals(
                keyword,
                listOf(RepresentativeGuideCategory.BATTERY),
                filterQuickCategorySettingsCategories(keyword),
            )
        }
    }

    @Test
    fun 선택한_순서와_관계없이_두_그룹_안에서는_기존_분류_순서를_유지한다() {
        val state = ItemSearchUiState(
            homeQuickCategoriesAtEntry = listOf(
                RepresentativeGuideCategory.ELECTRONICS,
                RepresentativeGuideCategory.BATTERY,
            ),
        )
        val selected = listOf(RepresentativeGuideCategory.BATTERY, RepresentativeGuideCategory.ELECTRONICS)

        assertEquals(
            selected + quickCategoryOrder.filterNot { it in selected },
            state.quickCategorySettingsOrder(2),
        )
        assertEquals(
            listOf(RepresentativeGuideCategory.ELECTRONICS) +
                quickCategoryOrder.filterNot { it == RepresentativeGuideCategory.ELECTRONICS },
            state.quickCategorySettingsOrder(1),
        )
    }

    @Test
    fun 첫_응답_전에는_목록을_확정하지_않고_선택_한도가_없으면_기본_순서를_사용한다() {
        assertEquals(emptyList<RepresentativeGuideCategory>(), ItemSearchUiState().quickCategorySettingsOrder(2))

        val state = ItemSearchUiState(homeQuickCategoriesAtEntry = listOf(RepresentativeGuideCategory.BATTERY))
        assertEquals(quickCategoryOrder, state.quickCategorySettingsOrder(0))
        assertEquals(quickCategoryOrder, state.quickCategorySettingsOrder(-1))
        assertEquals(
            quickCategoryOrder,
            ItemSearchUiState(homeQuickCategoriesAtEntry = emptyList()).quickCategorySettingsOrder(2),
        )
    }

    @Test
    fun 검색해도_진입할_때_정한_순서를_유지한다() {
        val entryOrder = listOf(
            RepresentativeGuideCategory.BATTERY,
            RepresentativeGuideCategory.PAPER_PACK,
            RepresentativeGuideCategory.PAPER,
        )

        assertEquals(
            listOf(RepresentativeGuideCategory.PAPER_PACK, RepresentativeGuideCategory.PAPER),
            filterQuickCategorySettingsCategories("종이", entryOrder),
        )
        assertEquals(entryOrder, filterQuickCategorySettingsCategories("", entryOrder))
    }

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

    @Test
    fun 공백_유무가_달라도_분류명으로_검색할_수_있다() {
        assertEquals(
            listOf(RepresentativeGuideCategory.CLOTHING),
            filterQuickCategorySettingsCategories("의류및원단"),
        )
        assertEquals(
            listOf(RepresentativeGuideCategory.CONSTRUCTION_WASTE),
            filterQuickCategorySettingsCategories("공사장생활폐기물"),
        )
        assertEquals(
            listOf(RepresentativeGuideCategory.HAZARDOUS),
            filterQuickCategorySettingsCategories("생활계유해폐기물"),
        )
    }
}
