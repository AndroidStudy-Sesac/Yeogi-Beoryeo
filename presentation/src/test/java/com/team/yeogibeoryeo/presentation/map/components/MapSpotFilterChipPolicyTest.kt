package com.team.yeogibeoryeo.presentation.map.components

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapSpotFilterChipPolicyTest {

    @Test
    fun `지도 chip 노출 목록은 CollectionSpotType entries 전체를 그대로 사용하지 않는다`() {
        assertNotEquals(
            CollectionSpotType.entries.toList(),
            MapSpotFilterChipPolicy.visibleTypes,
        )
    }

    @Test
    fun `품목 CTA 연동에 필요한 신규 타입은 지도 chip에 명시적으로 노출된다`() {
        val visibleTypes = MapSpotFilterChipPolicy.visibleTypes

        assertTrue(CollectionSpotType.MEDICINE_DROP_BOX in visibleTypes)
        assertTrue(CollectionSpotType.FLUORESCENT_LAMP_BIN in visibleTypes)
        assertTrue(CollectionSpotType.CLOTHING_BIN in visibleTypes)
        assertTrue(CollectionSpotType.ICE_PACK_BIN in visibleTypes)
        assertTrue(CollectionSpotType.WASTE_COOKING_OIL_BIN in visibleTypes)
        assertTrue(CollectionSpotType.HAZARDOUS_WASTE_BIN in visibleTypes)
    }

    @Test
    fun `기존 주요 지도 chip 타입은 계속 노출된다`() {
        val visibleTypes = MapSpotFilterChipPolicy.visibleTypes

        assertTrue(CollectionSpotType.SMALL_E_WASTE_BIN in visibleTypes)
        assertTrue(CollectionSpotType.BATTERY_BIN in visibleTypes)
        assertTrue(CollectionSpotType.PHONE_DROP_OFF in visibleTypes)
        assertTrue(CollectionSpotType.RECYCLING_CENTER in visibleTypes)
        assertTrue(CollectionSpotType.STANDARD_BAG_STORE in visibleTypes)
        assertTrue(CollectionSpotType.OTHER in visibleTypes)
    }

    @Test
    fun `지도 chip 노출 순서는 명시된 정책 순서를 유지한다`() {
        assertEquals(
            listOf(
                CollectionSpotType.SMALL_E_WASTE_BIN,
                CollectionSpotType.BATTERY_BIN,
                CollectionSpotType.PHONE_DROP_OFF,
                CollectionSpotType.FLUORESCENT_LAMP_BIN,
                CollectionSpotType.MEDICINE_DROP_BOX,
                CollectionSpotType.CLOTHING_BIN,
                CollectionSpotType.ICE_PACK_BIN,
                CollectionSpotType.WASTE_COOKING_OIL_BIN,
                CollectionSpotType.HAZARDOUS_WASTE_BIN,
                CollectionSpotType.RECYCLING_CENTER,
                CollectionSpotType.STANDARD_BAG_STORE,
                CollectionSpotType.OTHER,
            ),
            MapSpotFilterChipPolicy.visibleTypes,
        )
    }
}
