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
    fun `대표 분리배출 분류의 표시명과 상세 조회명은 같다`() {
        RepresentativeGuideCategory.entries.forEach { category ->
            assertEquals(category.displayName, category.representativeGuideName)
        }
    }

    @Test
    fun `대표 분리배출 분류는 공식 분류 순서로 노출된다`() {
        val expectedDisplayNames =
            listOf(
                "종이",
                "종이팩",
                "무색페트병",
                "플라스틱류",
                "비닐류",
                "발포합성수지",
                "유리병",
                "금속류",
                "의류 및 원단",
                "전지",
                "조명제품",
                "전기전자제품",
                "음식물류폐기물",
                "일반종량제폐기물",
                "불연성종량제폐기물",
                "대형폐기물",
                "공사장 생활폐기물",
                "생활계 유해폐기물",
                "기타",
            )

        assertEquals(expectedDisplayNames, RepresentativeGuideCategory.entries.map { it.displayName })
    }

    @Test
    fun `도메인 카테고리를 대표 카테고리로 매핑한다`() {
        RepresentativeGuideCategory.entries.forEach { category ->
            assertEquals(
                category,
                RepresentativeGuideCategory.fromDisposalCategory(category.disposalCategory),
            )
        }
    }
}
