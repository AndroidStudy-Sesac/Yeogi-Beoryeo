package com.team.yeogibeoryeo.presentation.search

import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalGuideSection
import com.team.yeogibeoryeo.domain.item.model.DisposalGuideSectionRow
import com.team.yeogibeoryeo.domain.item.model.DisposalInstruction
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.model.RelatedSpotType
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.presentation.search.model.ItemGuideDetailAction
import com.team.yeogibeoryeo.presentation.search.model.toDetailActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemGuideDetailActionTest {
    @Test
    fun `전지류는 폐건전지 수거함 CTA를 만든다`() {
        val actions = sampleGuide(
            name = "건전지",
            category = DisposalCategory.BATTERY,
        ).toDetailActions()

        assertMapTypes(actions, CollectionSpotType.BATTERY_BIN)
    }

    @Test
    fun `핸드폰 본품은 중소형 수거함과 폐휴대폰 수거처 CTA를 함께 만든다`() {
        val phoneActions = sampleGuide(
            name = "핸드폰",
            features = listOf("핸드폰은 소형전기전자제품으로 다량배출품목에 해당됩니다."),
        ).toDetailActions()
        val caseActions = sampleGuide(name = "핸드폰 케이스").toDetailActions()

        assertMapTypes(
            phoneActions,
            CollectionSpotType.SMALL_E_WASTE_BIN,
            CollectionSpotType.PHONE_DROP_OFF,
        )
        assertTrue(caseActions.isEmpty())
    }

    @Test
    fun `소형가전과 무상방문수거가 함께 있으면 지도 CTA와 공식 안내 CTA를 분리한다`() {
        val actions = sampleGuide(
            name = "전기밥솥",
            category = DisposalCategory.ELECTRONICS,
            relatedSpotTypes = listOf(
                RelatedSpotType.SMALL_E_WASTE_BIN,
                RelatedSpotType.FREE_PICKUP,
            ),
        ).toDetailActions()

        assertMapTypes(actions, CollectionSpotType.SMALL_E_WASTE_BIN)
        assertEquals(1, actions.filterIsInstance<ItemGuideDetailAction.OfficialGuide>().size)
    }

    @Test
    fun `전기전자제품 대표 가이드는 중소형 수거함과 폐휴대폰 수거처 CTA를 함께 만든다`() {
        val actions = sampleGuide(
            name = "전기전자제품",
            category = DisposalCategory.ELECTRONICS,
            features = listOf("소형전기전자제품은 전용 수거함으로 배출할 수 있습니다."),
        ).toDetailActions()

        assertMapTypes(
            actions,
            CollectionSpotType.SMALL_E_WASTE_BIN,
            CollectionSpotType.PHONE_DROP_OFF,
        )
    }

    @Test
    fun `일반종량제폐기물은 종량제봉투 판매처 CTA를 만든다`() {
        val actions = sampleGuide(
            name = "아이스박스",
            category = DisposalCategory.GENERAL,
            relatedSpotTypes = listOf(RelatedSpotType.GENERAL_WASTE_BAG),
        ).toDetailActions()

        assertMapTypes(actions, CollectionSpotType.STANDARD_BAG_STORE)
    }

    @Test
    fun `불연성종량제폐기물은 일반 종량제봉투 판매처 CTA를 만들지 않는다`() {
        val actions = sampleGuide(
            name = "내열냄비",
            category = DisposalCategory.NON_COMBUSTIBLE,
            relatedSpotTypes = listOf(RelatedSpotType.SPECIAL_BAG),
        ).toDetailActions()

        assertTrue(actions.isEmpty())
    }

    @Test
    fun `명확한 신규 수거함 품목은 해당 지도 타입 CTA를 만든다`() {
        assertMapTypes(
            sampleGuide(
                name = "형광등",
                features = listOf("직관형 형광등은 형광등 수거함으로 배출합니다."),
            ).toDetailActions(),
            CollectionSpotType.FLUORESCENT_LAMP_BIN,
        )
        assertMapTypes(
            sampleGuide(
                name = "감기약",
                detailSections = listOf(
                    DisposalGuideSection(
                        title = "배출방법",
                        lines = listOf("감기약은 폐의약품수거함으로 배출합니다."),
                    ),
                ),
            ).toDetailActions(),
            CollectionSpotType.MEDICINE_DROP_BOX,
        )
        assertMapTypes(
            sampleGuide(
                name = "아이스팩",
                features = listOf("젤아이스팩은 아이스팩 그대로 전용수거함 또는 종량제봉투로 배출합니다."),
            ).toDetailActions(),
            CollectionSpotType.ICE_PACK_BIN,
        )
        assertMapTypes(
            sampleGuide(
                name = "식용유",
                features = listOf("식용유는 폐식용유 수거함으로 배출합니다."),
            ).toDetailActions(),
            CollectionSpotType.WASTE_COOKING_OIL_BIN,
        )
    }

    @Test
    fun `용기와 포장재는 내용물 수거함 CTA를 만들지 않는다`() {
        assertTrue(
            sampleGuide(
                name = "약통 용기",
                features = listOf("내용물인 약은 폐의약품이므로 가까운 약국 등에 비치된 폐의약품전용수거함으로 배출합니다."),
            ).toDetailActions().isEmpty(),
        )
        assertTrue(
            sampleGuide(
                name = "식용유 용기",
                features = listOf("식용유 용기는 내용물을 비우고 플라스틱으로 배출합니다."),
            ).toDetailActions().isEmpty(),
        )
    }

    @Test
    fun `조건부 폐의약품 문구와 폐농약 설명은 폐의약품 CTA를 만들지 않는다`() {
        assertMapTypes(
            sampleGuide(
                name = "살균제",
                category = DisposalCategory.HAZARDOUS,
                features = listOf("생활계유해폐기물이란 폐농약, 폐의약품 등 피해를 유발할 수 있는 폐기물을 말합니다."),
                detailSections = listOf(
                    DisposalGuideSection(
                        title = "배출방법",
                        lines = listOf("살균제는 전용수거함으로 배출합니다."),
                    ),
                ),
            ).toDetailActions(),
            CollectionSpotType.HAZARDOUS_WASTE_BIN,
        )
        assertTrue(
            sampleGuide(
                name = "영양제",
                features = listOf("영양제가 의약품인 경우 폐의약품 수거함으로 배출합니다."),
            ).toDetailActions().isEmpty(),
        )
    }

    @Test
    fun `생활계 유해폐기물 대표 가이드의 폐의약품 행은 폐의약품 수거함 CTA를 만든다`() {
        val actions = sampleGuide(
            name = "생활계 유해폐기물",
            category = DisposalCategory.HAZARDOUS,
            features = listOf("생활계 유해폐기물 전용수거함에 배출합니다."),
            detailSections = listOf(
                DisposalGuideSection(
                    title = "품목별 배출 방법",
                    lines = emptyList(),
                    rows = listOf(
                        DisposalGuideSectionRow(
                            label = "폐의약품",
                            value = "약국, 보건소, 주민센터 등 전용수거함에 배출하거나 우체통에 배출",
                        ),
                    ),
                ),
            ),
        ).toDetailActions()

        assertMapTypes(
            actions,
            CollectionSpotType.MEDICINE_DROP_BOX,
            CollectionSpotType.HAZARDOUS_WASTE_BIN,
            CollectionSpotType.ICE_PACK_BIN,
            CollectionSpotType.WASTE_COOKING_OIL_BIN,
        )
    }

    @Test
    fun `전용 지도 타입이 없는 생활계 유해폐기물 하위 품목은 기타 CTA를 만들지 않는다`() {
        assertTrue(
            sampleGuide(
                name = "엔진오일",
                category = DisposalCategory.HAZARDOUS,
                features = listOf("엔진오일은 전용수거함으로 배출하거나 자동차 정비소 등 역회수 루트를 통하여 배출합니다."),
            ).toDetailActions().isEmpty(),
        )
        assertTrue(
            sampleGuide(
                name = "빈 농약용기",
                category = DisposalCategory.HAZARDOUS,
                features = listOf("영농 후 배출되는 폐비닐과 농약용기는 영농폐기물에 해당됩니다."),
            ).toDetailActions().isEmpty(),
        )
    }

    private fun assertMapTypes(
        actions: List<ItemGuideDetailAction>,
        vararg types: CollectionSpotType,
    ) {
        assertEquals(
            types.toList(),
            actions.filterIsInstance<ItemGuideDetailAction.MapSpot>().map { it.type },
        )
    }

    private fun sampleGuide(
        name: String,
        category: DisposalCategory = DisposalCategory.OTHER,
        relatedSpotTypes: List<RelatedSpotType> = emptyList(),
        features: List<String> = emptyList(),
        detailSections: List<DisposalGuideSection> = emptyList(),
    ): DisposalItemGuide =
        DisposalItemGuide(
            id = name,
            name = name,
            category = category,
            subCategory = null,
            instructions = listOf(DisposalInstruction(method = "분리배출")),
            features = features,
            detailSections = detailSections,
            tip = null,
            isRecyclable = true,
            relatedSpotTypes = relatedSpotTypes,
        )
}
