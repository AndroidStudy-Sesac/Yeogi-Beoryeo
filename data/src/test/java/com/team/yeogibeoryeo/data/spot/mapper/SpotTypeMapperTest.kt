package com.team.yeogibeoryeo.data.spot.mapper

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import org.junit.Assert.assertEquals
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
}