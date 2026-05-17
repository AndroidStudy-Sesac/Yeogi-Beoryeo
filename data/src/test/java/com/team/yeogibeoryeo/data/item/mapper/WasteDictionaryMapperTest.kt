package com.team.yeogibeoryeo.data.item.mapper

import com.team.yeogibeoryeo.data.item.local.WasteDictionaryItem
import com.team.yeogibeoryeo.domain.item.model.RelatedSpotType
import org.junit.Assert.assertEquals
import org.junit.Test

class WasteDictionaryMapperTest {
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
}
