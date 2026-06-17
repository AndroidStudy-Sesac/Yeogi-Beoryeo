package com.team.yeogibeoryeo.presentation.map.components

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import org.junit.Assert.assertFalse
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
    fun `신규 내부 타입은 지도 chip에 자동 노출되지 않는다`() {
        val visibleTypes = MapSpotFilterChipPolicy.visibleTypes

        assertFalse(CollectionSpotType.MEDICINE_DROP_BOX in visibleTypes)
        assertFalse(CollectionSpotType.FLUORESCENT_LAMP_BIN in visibleTypes)
        assertFalse(CollectionSpotType.CLOTHING_BIN in visibleTypes)
        assertFalse(CollectionSpotType.ICE_PACK_BIN in visibleTypes)
        assertFalse(CollectionSpotType.WASTE_COOKING_OIL_BIN in visibleTypes)
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
}
