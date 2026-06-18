package com.team.yeogibeoryeo.data.spot.mapper

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SpotTypeMapperTest {

    @Test
    fun `장소명이 폐건전지 수거함이면 BATTERY_BIN을 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "폐건전지 수거함",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.BATTERY_BIN, result)
    }

    @Test
    fun `장소명이 폐휴대폰 배출처이면 PHONE_DROP_OFF를 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "폐휴대폰 배출처",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.PHONE_DROP_OFF, result)
    }

    @Test
    fun `장소명이 재활용센터이면 RECYCLING_CENTER를 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "구로 재활용센터",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.RECYCLING_CENTER, result)
    }

    @Test
    fun `장소명이 종량제봉투 판매소이면 STANDARD_BAG_STORE를 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "종량제봉투 판매소",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.STANDARD_BAG_STORE, result)
    }

    @Test
    fun `장소명이 중소형 수거함이면 SMALL_E_WASTE_BIN을 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "중소형 폐가전 수거함",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.SMALL_E_WASTE_BIN, result)
    }

    @Test
    fun `알 수 없는 장소명이면 OTHER를 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "임시 배출장소",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.OTHER, result)
    }

    @Test
    fun `폐건전지 수거함은 수거함보다 건전지 조건이 우선되어 BATTERY_BIN을 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "폐건전지 수거함",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.BATTERY_BIN, result)
    }

    @Test
    fun `폐의약품수거함이면 MEDICINE_DROP_BOX를 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "폐의약품수거함",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.MEDICINE_DROP_BOX, result)
    }

    @Test
    fun `의약품수거함이면 MEDICINE_DROP_BOX를 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "의약품수거함",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.MEDICINE_DROP_BOX, result)
    }

    @Test
    fun `폐형광등 수거함이면 FLUORESCENT_LAMP_BIN을 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "폐형광등 수거함",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.FLUORESCENT_LAMP_BIN, result)
    }

    @Test
    fun `의류 수거함이면 CLOTHING_BIN을 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "의류 수거함",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.CLOTHING_BIN, result)
    }

    @Test
    fun `아이스팩 수거함이면 ICE_PACK_BIN을 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "아이스팩 수거함",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.ICE_PACK_BIN, result)
    }

    @Test
    fun `폐식용유 수거함이면 WASTE_COOKING_OIL_BIN을 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "폐식용유 수거함",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.WASTE_COOKING_OIL_BIN, result)
    }

    @Test
    fun `소형가전 수거함이면 SMALL_E_WASTE_BIN을 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "소형가전 수거함",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.SMALL_E_WASTE_BIN, result)
    }

    @Test
    fun `소형가전수거함이면 SMALL_E_WASTE_BIN을 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "소형가전수거함",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.SMALL_E_WASTE_BIN, result)
    }

    @Test
    fun `소형전기전자제품수거함이면 SMALL_E_WASTE_BIN을 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "소형전기전자제품수거함",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.SMALL_E_WASTE_BIN, result)
    }

    @Test
    fun `폐가전 수거함이면 SMALL_E_WASTE_BIN을 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "폐가전 수거함",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.SMALL_E_WASTE_BIN, result)
    }

    @Test
    fun `폐가전수거함이면 SMALL_E_WASTE_BIN을 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "폐가전수거함",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.SMALL_E_WASTE_BIN, result)
    }

    @Test
    fun `수거함 단어만으로는 SMALL_E_WASTE_BIN을 반환하지 않는다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "마을 공용 수거함",
            detailAddress = null,
        )

        assertNotEquals(CollectionSpotType.SMALL_E_WASTE_BIN, result)
        assertEquals(CollectionSpotType.OTHER, result)
    }

    @Test
    fun `폐전지 수거함이면 BATTERY_BIN을 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "폐전지 수거함",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.BATTERY_BIN, result)
    }

    @Test
    fun `폐건전지와 폐형광등 복합 수거함은 현재 chip 정책상 BATTERY_BIN을 우선 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "폐건전지, 폐형광등 수거함",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.BATTERY_BIN, result)
    }

    @Test
    fun `폐형광등 전지수거함 복합 응답은 현재 chip 정책상 BATTERY_BIN을 우선 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "폐형광등,전지수거함",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.BATTERY_BIN, result)
    }

    @Test
    fun `재활용정거장이면 RECYCLING_CENTER를 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "재활용정거장",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.RECYCLING_CENTER, result)
    }

    @Test
    fun `재활용동네마당이면 RECYCLING_CENTER를 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "재활용동네마당",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.RECYCLING_CENTER, result)
    }

    @Test
    fun `재활용도움센터이면 RECYCLING_CENTER를 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "재활용도움센터",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.RECYCLING_CENTER, result)
    }

    @Test
    fun `클린하우스이면 RECYCLING_CENTER를 반환한다`() {
        val result = SpotTypeMapper.mapToType(
            spotName = "클린하우스",
            detailAddress = null,
        )

        assertEquals(CollectionSpotType.RECYCLING_CENTER, result)
    }
}
