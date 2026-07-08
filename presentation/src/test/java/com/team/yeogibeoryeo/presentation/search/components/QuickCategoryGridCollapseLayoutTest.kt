package com.team.yeogibeoryeo.presentation.search.components

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickCategoryGridCollapseLayoutTest {
    @Test
    fun `접힌 퀵 카테고리는 adaptive 열 수 기준 마지막 슬롯에 더보기를 둔다`() {
        mapOf(
            2 to 8,
            3 to 9,
            4 to 8,
            5 to 10,
            6 to 12,
        ).forEach { (columnCount, expectedCollapsedItemCount) ->
            val layout = quickCategoryGridCollapseLayout(
                categoryCount = 20,
                columnCount = columnCount,
                isExpanded = false,
            )

            assertEquals(expectedCollapsedItemCount, layout.collapsedItemCount)
            assertEquals(expectedCollapsedItemCount - 1, layout.visibleCategoryCount)
            assertEquals(true, layout.showsMore)
            assertEquals(false, layout.showsCollapse)
        }
    }

    @Test
    fun `접힌 퀵 카테고리는 화면에 들어가는 행 수 기준 마지막 슬롯에 더보기를 둔다`() {
        val layout = quickCategoryGridCollapseLayout(
            categoryCount = 20,
            columnCount = 4,
            availableHeightPx = 230,
            rowHeightPx = 70,
            rowSpacingPx = 10,
            isExpanded = false,
        )

        assertEquals(12, layout.collapsedItemCount)
        assertEquals(11, layout.visibleCategoryCount)
        assertEquals(true, layout.showsMore)
    }

    @Test
    fun `화면에 한 행만 들어가면 마지막 칸 하나만 더보기로 남긴다`() {
        val layout = quickCategoryGridCollapseLayout(
            categoryCount = 20,
            columnCount = 3,
            availableHeightPx = 70,
            rowHeightPx = 70,
            rowSpacingPx = 10,
            isExpanded = false,
        )

        assertEquals(3, layout.collapsedItemCount)
        assertEquals(2, layout.visibleCategoryCount)
        assertEquals(true, layout.showsMore)
    }

    @Test
    fun `퀵 카테고리가 접힌 슬롯 안에 들어오면 더보기를 만들지 않는다`() {
        val layout = quickCategoryGridCollapseLayout(
            categoryCount = 8,
            columnCount = 4,
            isExpanded = false,
        )

        assertEquals(8, layout.visibleCategoryCount)
        assertEquals(false, layout.showsMore)
        assertEquals(false, layout.showsCollapse)
    }

    @Test
    fun `펼친 퀵 카테고리는 전체 카테고리와 접기를 보여준다`() {
        val layout = quickCategoryGridCollapseLayout(
            categoryCount = 20,
            columnCount = 4,
            isExpanded = true,
        )

        assertEquals(20, layout.visibleCategoryCount)
        assertEquals(false, layout.showsMore)
        assertEquals(true, layout.showsCollapse)
    }

    @Test
    fun `펼친 퀵 카테고리는 스크롤 복귀 후 큰 가용 높이로 재측정돼도 접기를 유지한다`() {
        val layout = quickCategoryGridCollapseLayout(
            categoryCount = 20,
            columnCount = 4,
            availableHeightPx = 2000,
            rowHeightPx = 70,
            rowSpacingPx = 10,
            isExpanded = true,
        )

        assertEquals(20, layout.visibleCategoryCount)
        assertEquals(false, layout.showsMore)
        assertEquals(true, layout.showsCollapse)
    }

    @Test
    fun `고정된 접힘 슬롯이 있으면 현재 가용 높이보다 우선한다`() {
        val layout = quickCategoryGridCollapseLayout(
            categoryCount = 20,
            columnCount = 4,
            availableHeightPx = 2000,
            rowHeightPx = 70,
            rowSpacingPx = 10,
            fixedCollapsedItemCount = 8,
            isExpanded = false,
        )

        assertEquals(8, layout.collapsedItemCount)
        assertEquals(7, layout.visibleCategoryCount)
        assertEquals(true, layout.showsMore)
    }

    @Test
    fun `고정된 접힘 슬롯이 현재 열 수와 맞지 않으면 다시 계산한다`() {
        val layout = quickCategoryGridCollapseLayout(
            categoryCount = 20,
            columnCount = 6,
            fixedCollapsedItemCount = 8,
            isExpanded = false,
        )

        assertEquals(12, layout.collapsedItemCount)
        assertEquals(11, layout.visibleCategoryCount)
        assertEquals(true, layout.showsMore)
    }
}
