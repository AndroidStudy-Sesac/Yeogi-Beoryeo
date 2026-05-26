package com.team.yeogibeoryeo.data.item.mapper

import com.team.yeogibeoryeo.data.item.local.WasteDictionaryItem
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.RelatedSpotType
import org.junit.Assert.assertEquals
import org.junit.Test

class WasteDictionaryMapperTest {
    @Test
    fun `무색페트병 경로는 플라스틱이 아니라 무색페트병 카테고리로 매핑한다`() {
        val result =
            WasteDictionaryItem(
                name = "무색페트병(물병)",
                categoryPaths = listOf(listOf("재활용폐기물", "무색페트병")),
                similarItems = emptyList(),
                dischargeMethods = listOf("무색페트병 수거함으로 배출합니다."),
                features = emptyList(),
                notes = emptyList(),
            ).toDomain()

        assertEquals(DisposalCategory.COLORLESS_PET, result.category)
    }

    @Test
    fun `세분화된 원본 경로는 각각의 독립 카테고리로 매핑한다`() {
        val cases =
            listOf(
                listOf("재활용폐기물", "종이팩 일반팩") to DisposalCategory.PAPER_PACK,
                listOf("재활용폐기물", "전지류") to DisposalCategory.BATTERY,
                listOf("재활용폐기물", "조명제품") to DisposalCategory.LIGHTING,
                listOf("일반폐기물", "불연성종량제폐기물") to DisposalCategory.NON_COMBUSTIBLE,
                listOf("공사장 생활폐기물") to DisposalCategory.CONSTRUCTION_WASTE,
            )

        cases.forEach { (path, expectedCategory) ->
            val result =
                WasteDictionaryItem(
                    name = expectedCategory.name,
                    categoryPaths = listOf(path),
                    similarItems = emptyList(),
                    dischargeMethods = listOf("배출합니다."),
                    features = emptyList(),
                    notes = emptyList(),
                ).toDomain()

            assertEquals(expectedCategory, result.category)
        }
    }

    @Test
    fun `불연성 종량제봉투 품목은 일반 종량제봉투 장소로도 분류하지 않는다`() {
        val result =
            WasteDictionaryItem(
                name = "내열냄비",
                categoryPaths = listOf(listOf("일반폐기물", "불연성종량제폐기물")),
                similarItems = emptyList(),
                dischargeMethods = listOf("내열냄비는 불연성 종량제봉투(마대)로 배출합니다."),
                features = emptyList(),
                notes = emptyList(),
            ).toDomain()

        assertEquals(listOf(RelatedSpotType.SPECIAL_BAG), result.relatedSpotTypes)
    }

    @Test
    fun `서로 다른 배출 문장이 있으면 관련 장소를 모두 유지한다`() {
        val result =
            WasteDictionaryItem(
                name = "공사장 생활폐기물",
                categoryPaths = listOf(listOf("공사장 생활폐기물")),
                similarItems = emptyList(),
                dischargeMethods =
                    listOf(
                        "일반종량제폐기물은 종량제봉투로 배출합니다.",
                        "불연성종량제폐기물은 불연성종량제봉투(특수규격마대)로 배출합니다.",
                    ),
                features = emptyList(),
                notes = emptyList(),
            ).toDomain()

        assertEquals(
            listOf(RelatedSpotType.GENERAL_WASTE_BAG, RelatedSpotType.SPECIAL_BAG),
            result.relatedSpotTypes,
        )
    }

    @Test
    fun `재활용 목적의 전용 수거함 표현은 전용 수거함 장소로 분류하고 재활용 가능성은 유지한다`() {
        val cases =
            listOf(
                Triple(
                    "의류",
                    listOf(listOf("재활용폐기물", "의류 및 원단")),
                    "의류수거함에 배출합니다.",
                ),
                Triple(
                    "형광등",
                    listOf(listOf("재활용폐기물", "조명제품")),
                    "형광등수거함에 배출합니다.",
                ),
                Triple(
                    "건전지",
                    listOf(listOf("재활용폐기물", "전지류")),
                    "폐건전지수거함에 배출합니다.",
                ),
            )

        cases.forEach { (name, categoryPaths, dischargeMethod) ->
            val result =
                WasteDictionaryItem(
                    name = name,
                    categoryPaths = categoryPaths,
                    similarItems = emptyList(),
                    dischargeMethods = listOf(dischargeMethod),
                    features = emptyList(),
                    notes = emptyList(),
                ).toDomain()

            assertEquals(listOf(RelatedSpotType.DEDICATED_BIN), result.relatedSpotTypes)
            assertEquals(true, result.isRecyclable)
        }
    }

    @Test
    fun `유해폐기물 전용 수거함 표현은 전용 수거함 장소로 분류하고 재활용 가능으로 보지 않는다`() {
        val result =
            WasteDictionaryItem(
                name = "폐의약품",
                categoryPaths = listOf(listOf("생활계 유해폐기물", "폐의약품")),
                similarItems = emptyList(),
                dischargeMethods = listOf("폐의약품수거함에 배출합니다."),
                features = emptyList(),
                notes = emptyList(),
            ).toDomain()

        assertEquals(listOf(RelatedSpotType.DEDICATED_BIN), result.relatedSpotTypes)
        assertEquals(false, result.isRecyclable)
    }

    @Test
    fun `품목사전 배출방법 특징 유의사항은 상세 섹션으로 매핑한다`() {
        val result =
            WasteDictionaryItem(
                name = "우유팩",
                categoryPaths = listOf(listOf("재활용폐기물", "종이팩 일반팩")),
                similarItems = emptyList(),
                dischargeMethods = listOf("종이팩으로 배출합니다."),
                features = listOf("일반팩과 멸균팩이 있습니다."),
                notes = listOf("내용물을 헹군 뒤 배출합니다."),
            ).toDomain()

        assertEquals("배출방법", result.detailSections[0].title)
        assertEquals(listOf("종이팩으로 배출합니다."), result.detailSections[0].lines)
        assertEquals("특징", result.detailSections[1].title)
        assertEquals(listOf("일반팩과 멸균팩이 있습니다."), result.detailSections[1].lines)
        assertEquals("유의사항", result.detailSections[2].title)
        assertEquals(listOf("내용물을 헹군 뒤 배출합니다."), result.detailSections[2].lines)
    }
}
