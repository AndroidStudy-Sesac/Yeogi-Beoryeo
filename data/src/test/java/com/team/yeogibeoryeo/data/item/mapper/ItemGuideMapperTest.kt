package com.team.yeogibeoryeo.data.item.mapper

import com.team.yeogibeoryeo.data.item.local.ItemGuideDetail
import com.team.yeogibeoryeo.data.item.remote.dto.ItemGuideDto
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalSubCategory
import com.team.yeogibeoryeo.domain.item.model.RelatedSpotType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemGuideMapperTest {
    @Test
    fun `품목명이 상세 가이드와 직접 일치하면 원격 품목에 상세 정보가 병합된다`() {
        val result =
            ItemGuideDto(
                itemNm = "유리병",
                dschgMthd = "재활용폐기물, 보증금 환급",
            ).toDomain(
                categoryMap = mapOf("유리병" to (DisposalCategory.GLASS to DisposalSubCategory.GLASS_BOTTLE)),
                relatedSpotsMap = emptyMap(),
                guideDetailsMap =
                    mapOf(
                        "유리병" to
                            ItemGuideDetail(
                                steps = listOf("내용물을 비웁니다."),
                                cautions = listOf("깨진 유리는 따로 배출합니다."),
                                tip = "보증금 환급 대상은 환급처에 반환합니다.",
                                relatedSpotTypes = listOf(RelatedSpotType.RECYCLING_BIN),
                            ),
                    ),
                guideDetailAliases = emptyMap(),
            )

        assertEquals("유리병", result.name)
        assertEquals(DisposalCategory.GLASS, result.category)
        assertEquals(DisposalSubCategory.GLASS_BOTTLE, result.subCategory)
        assertEquals(listOf("내용물을 비웁니다."), result.steps)
        assertEquals(listOf("깨진 유리는 따로 배출합니다."), result.cautions)
        assertEquals("보증금 환급 대상은 환급처에 반환합니다.", result.tip)
        assertEquals(listOf(RelatedSpotType.RECYCLING_BIN), result.relatedSpotTypes)
        assertEquals(listOf("재활용폐기물", "보증금 환급"), result.instructions.map { it.method })
        assertTrue(result.isRecyclable)
    }

    @Test
    fun `가이드 별칭으로 구체 품목에 대표 가이드가 병합된다`() {
        val result =
            ItemGuideDto(
                itemNm = "골판지",
                dschgMthd = "재활용폐기물",
            ).toDomain(
                categoryMap = mapOf("골판지" to (DisposalCategory.PAPER to DisposalSubCategory.CARDBOARD)),
                relatedSpotsMap = emptyMap(),
                guideDetailsMap =
                    mapOf(
                        "종이" to
                            ItemGuideDetail(
                                steps = listOf("이물질을 제거합니다."),
                                cautions = listOf("오염된 종이는 종량제봉투로 배출합니다."),
                                tip = "상자는 펼쳐서 배출합니다.",
                                relatedSpotTypes = listOf(RelatedSpotType.RECYCLING_BIN),
                            ),
                    ),
                guideDetailAliases = mapOf("골판지" to "종이"),
            )

        assertEquals("골판지", result.name)
        assertEquals(DisposalCategory.PAPER, result.category)
        assertEquals(DisposalSubCategory.CARDBOARD, result.subCategory)
        assertEquals(listOf("이물질을 제거합니다."), result.steps)
        assertEquals(listOf("오염된 종이는 종량제봉투로 배출합니다."), result.cautions)
        assertEquals("상자는 펼쳐서 배출합니다.", result.tip)
        assertEquals(listOf(RelatedSpotType.RECYCLING_BIN), result.relatedSpotTypes)
        assertTrue(result.isRecyclable)
    }

    @Test
    fun `구체 품목의 카테고리 매핑이 없으면 가이드 별칭의 대표 카테고리를 사용한다`() {
        val result =
            ItemGuideDto(
                itemNm = "맥주캔",
                dschgMthd = "재활용폐기물",
            ).toDomain(
                categoryMap = mapOf("금속캔" to (DisposalCategory.METAL to DisposalSubCategory.ALUMINUM_CAN)),
                relatedSpotsMap = emptyMap(),
                guideDetailsMap =
                    mapOf(
                        "금속캔" to
                            ItemGuideDetail(
                                steps = listOf("내용물을 비우고 헹굽니다."),
                                cautions = emptyList(),
                                tip = null,
                                relatedSpotTypes = emptyList(),
                            ),
                    ),
                guideDetailAliases = mapOf("맥주캔" to "금속캔"),
            )

        assertEquals(DisposalCategory.METAL, result.category)
        assertEquals(DisposalSubCategory.ALUMINUM_CAN, result.subCategory)
        assertEquals(listOf("내용물을 비우고 헹굽니다."), result.steps)
    }

    @Test
    fun `상세 가이드가 없는 원격 품목은 API 배출방법만 유지한다`() {
        val result =
            ItemGuideDto(
                itemNm = "알 수 없는 품목",
                dschgMthd = "종량제봉투",
            ).toDomain(
                categoryMap = emptyMap(),
                relatedSpotsMap = emptyMap(),
                guideDetailsMap = emptyMap(),
                guideDetailAliases = emptyMap(),
            )

        assertEquals("알 수 없는 품목", result.name)
        assertEquals(DisposalCategory.OTHER, result.category)
        assertEquals(listOf("종량제봉투"), result.instructions.map { it.method })
        assertTrue(result.steps.isEmpty())
        assertTrue(result.cautions.isEmpty())
        assertEquals(null, result.tip)
        assertFalse(result.isRecyclable)
    }

    @Test
    fun `종량제봉투 단독 배출방법은 일반종량제폐기물 가이드로 보강된다`() {
        val result =
            ItemGuideDto(
                itemNm = "닭뼈",
                dschgMthd = "종량제봉투",
            ).toDomain(
                categoryMap = emptyMap(),
                relatedSpotsMap = emptyMap(),
                guideDetailsMap =
                    mapOf(
                        "일반종량제폐기물" to
                            ItemGuideDetail(
                                steps = listOf("물기를 제거한 뒤 종량제봉투에 담습니다."),
                                cautions = listOf("재활용품과 섞지 않습니다."),
                                tip = "음식물로 분류되지 않는 것은 일반쓰레기로 배출합니다.",
                                relatedSpotTypes = emptyList(),
                            ),
                    ),
                guideDetailAliases = emptyMap(),
            )

        assertEquals(listOf("물기를 제거한 뒤 종량제봉투에 담습니다."), result.steps)
        assertEquals(listOf("재활용품과 섞지 않습니다."), result.cautions)
        assertEquals("음식물로 분류되지 않는 것은 일반쓰레기로 배출합니다.", result.tip)
    }

    @Test
    fun `대형폐기물 단독 배출방법은 대형폐기물 가이드로 보강된다`() {
        val result =
            ItemGuideDto(
                itemNm = "세면대",
                dschgMthd = "대형폐기물",
            ).toDomain(
                categoryMap = emptyMap(),
                relatedSpotsMap = emptyMap(),
                guideDetailsMap =
                    mapOf(
                        "대형폐기물" to
                            ItemGuideDetail(
                                steps = listOf("지자체 대형폐기물 신고 후 배출합니다."),
                                cautions = emptyList(),
                                tip = null,
                                relatedSpotTypes = emptyList(),
                            ),
                    ),
                guideDetailAliases = emptyMap(),
            )

        assertEquals(listOf("지자체 대형폐기물 신고 후 배출합니다."), result.steps)
    }

    @Test
    fun `특수규격봉투 배출방법은 불연성종량제폐기물 가이드로 보강된다`() {
        val result =
            ItemGuideDto(
                itemNm = "깨진 유리컵",
                dschgMthd = "특수규격봉투",
            ).toDomain(
                categoryMap = emptyMap(),
                relatedSpotsMap = emptyMap(),
                guideDetailsMap =
                    mapOf(
                        "불연성종량제폐기물" to
                            ItemGuideDetail(
                                steps = listOf("불연성 전용 봉투에 담아 배출합니다."),
                                cautions = emptyList(),
                                tip = null,
                                relatedSpotTypes = emptyList(),
                            ),
                    ),
                guideDetailAliases = emptyMap(),
            )

        assertEquals(listOf("불연성 전용 봉투에 담아 배출합니다."), result.steps)
    }

    @Test
    fun `음식물류폐기물 배출방법은 음식물쓰레기 가이드로 보강된다`() {
        val result =
            ItemGuideDto(
                itemNm = "과일껍질",
                dschgMthd = "음식물류폐기물",
            ).toDomain(
                categoryMap = emptyMap(),
                relatedSpotsMap = emptyMap(),
                guideDetailsMap =
                    mapOf(
                        "음식물쓰레기" to
                            ItemGuideDetail(
                                steps = listOf("물기를 제거한 뒤 음식물 전용 수거 방식에 맞춰 배출합니다."),
                                cautions = emptyList(),
                                tip = null,
                                relatedSpotTypes = emptyList(),
                            ),
                    ),
                guideDetailAliases = emptyMap(),
            )

        assertEquals(listOf("물기를 제거한 뒤 음식물 전용 수거 방식에 맞춰 배출합니다."), result.steps)
    }

    @Test
    fun `복합 배출방법은 단일 배출방법 가이드로 보강하지 않는다`() {
        val result =
            ItemGuideDto(
                itemNm = "우산",
                dschgMthd = "종량제봉투, 대형폐기물",
            ).toDomain(
                categoryMap = emptyMap(),
                relatedSpotsMap = emptyMap(),
                guideDetailsMap =
                    mapOf(
                        "일반종량제폐기물" to
                            ItemGuideDetail(
                                steps = listOf("종량제봉투 상세 가이드"),
                                cautions = emptyList(),
                                tip = null,
                                relatedSpotTypes = emptyList(),
                            ),
                        "대형폐기물" to
                            ItemGuideDetail(
                                steps = listOf("대형폐기물 상세 가이드"),
                                cautions = emptyList(),
                                tip = null,
                                relatedSpotTypes = emptyList(),
                            ),
                    ),
                guideDetailAliases = emptyMap(),
            )

        assertTrue(result.steps.isEmpty())
        assertEquals(listOf("종량제봉투", "대형폐기물"), result.instructions.map { it.method })
    }

    @Test
    fun `쉼표로 구분된 여러 배출방법은 여러 안내 항목으로 변환된다`() {
        val result =
            ItemGuideDto(
                itemNm = "우산",
                dschgMthd = "종량제봉투, 대형폐기물, 재활용폐기물",
            ).toDomain(
                categoryMap = emptyMap(),
                relatedSpotsMap = emptyMap(),
                guideDetailsMap = emptyMap(),
                guideDetailAliases = emptyMap(),
            )

        assertEquals(
            listOf("종량제봉투", "대형폐기물", "재활용폐기물"),
            result.instructions.map { it.method },
        )
        assertTrue(result.isRecyclable)
    }

    @Test
    fun `배출방법 앞뒤 공백은 제거된다`() {
        val result =
            ItemGuideDto(
                itemNm = "알 수 없는 품목",
                dschgMthd = "  재활용폐기물  ,   종량제봉투 ",
            ).toDomain(
                categoryMap = emptyMap(),
                relatedSpotsMap = emptyMap(),
                guideDetailsMap = emptyMap(),
                guideDetailAliases = emptyMap(),
            )

        assertEquals(listOf("재활용폐기물", "종량제봉투"), result.instructions.map { it.method })
    }

    @Test
    fun `빈 배출방법은 빈 안내 항목과 재활용 불가로 변환된다`() {
        val result =
            ItemGuideDto(
                itemNm = "알 수 없는 품목",
                dschgMthd = "",
            ).toDomain(
                categoryMap = emptyMap(),
                relatedSpotsMap = emptyMap(),
                guideDetailsMap = emptyMap(),
                guideDetailAliases = emptyMap(),
            )

        assertTrue(result.instructions.isEmpty())
        assertFalse(result.isRecyclable)
    }

    @Test
    fun `상세 가이드에 관련 장소가 없으면 관련 장소 맵을 사용한다`() {
        val result =
            ItemGuideDto(
                itemNm = "냉장고",
                dschgMthd = "역회수",
            ).toDomain(
                categoryMap = mapOf("냉장고" to (DisposalCategory.ELECTRONICS to DisposalSubCategory.LARGE_APPLIANCE)),
                relatedSpotsMap = mapOf("냉장고" to listOf(RelatedSpotType.FREE_PICKUP, RelatedSpotType.E_WASTE_BIN)),
                guideDetailsMap = emptyMap(),
                guideDetailAliases = emptyMap(),
            )

        assertEquals(
            listOf(RelatedSpotType.FREE_PICKUP, RelatedSpotType.E_WASTE_BIN),
            result.relatedSpotTypes,
        )
        assertTrue(result.isRecyclable)
    }
}
