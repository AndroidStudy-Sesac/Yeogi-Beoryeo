package com.team.yeogibeoryeo.presentation.search.model

import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RepresentativeGuideCategoryTest {
    @Test
    fun `대표 가이드명으로 카테고리를 찾는다`() {
        assertEquals(
            RepresentativeGuideCategory.PAPER_PACK,
            RepresentativeGuideCategory.fromGuideName("종이팩"),
        )
    }

    @Test
    fun `없는 대표 가이드명은 null을 반환한다`() {
        assertNull(RepresentativeGuideCategory.fromGuideName("없는 가이드"))
    }

    @Test
    fun `도메인 카테고리를 대표 카테고리로 매핑한다`() {
        assertEquals(
            RepresentativeGuideCategory.ELECTRONICS,
            RepresentativeGuideCategory.fromDisposalCategory(DisposalCategory.ELECTRONICS),
        )
        assertEquals(
            RepresentativeGuideCategory.GENERAL,
            RepresentativeGuideCategory.fromDisposalCategory(DisposalCategory.GENERAL),
        )
        assertEquals(
            RepresentativeGuideCategory.PAPER_PACK,
            RepresentativeGuideCategory.fromDisposalCategory(DisposalCategory.PAPER_PACK),
        )
        assertEquals(
            RepresentativeGuideCategory.BATTERY,
            RepresentativeGuideCategory.fromDisposalCategory(DisposalCategory.BATTERY),
        )
        assertEquals(
            RepresentativeGuideCategory.LIGHTING,
            RepresentativeGuideCategory.fromDisposalCategory(DisposalCategory.LIGHTING),
        )
        assertEquals(
            RepresentativeGuideCategory.NON_COMBUSTIBLE,
            RepresentativeGuideCategory.fromDisposalCategory(DisposalCategory.NON_COMBUSTIBLE),
        )
        assertEquals(
            RepresentativeGuideCategory.CONSTRUCTION_WASTE,
            RepresentativeGuideCategory.fromDisposalCategory(DisposalCategory.CONSTRUCTION_WASTE),
        )
    }
}
