package com.team.yeogibeoryeo.data.spot.mapper

import com.team.yeogibeoryeo.data.spot.remote.dto.SpotItemDto
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SpotMapperTest {

    private val spotMapper = SpotMapper()

    @Test
    fun `SpotItemDto를 CollectionSpot으로 변환한다`() {
        val dto = SpotItemDto(
            spotNm = "폐건전지 수거함",
            addrBase = "서울특별시 영등포구 문래동",
            addrDtl = "주민센터 앞",
        )

        val result = spotMapper.mapToDomain(dto)

        assertEquals("폐건전지 수거함", result.name)
        assertEquals("서울특별시 영등포구 문래동", result.address)
        assertEquals("주민센터 앞", result.detailLocation)
        assertEquals(CollectionSpotType.BATTERY_BIN, result.type)
        assertNull(result.coordinate)
        assertNull(result.distanceMeter)
        assertFalse(result.isBookmarked)
    }

    @Test
    fun `SpotItemDto 변환 시 id는 장소명 주소 상세주소를 기반으로 생성된다`() {
        val dto = SpotItemDto(
            spotNm = "폐건전지 수거함",
            addrBase = "서울특별시 영등포구 문래동",
            addrDtl = "주민센터 앞",
        )

        val result = spotMapper.mapToDomain(dto)

        assertEquals(
            "폐건전지 수거함_서울특별시 영등포구 문래동_주민센터 앞",
            result.id,
        )
    }

    @Test
    fun `addrDtl이 빈 문자열이면 detailLocation은 null로 변환된다`() {
        val dto = SpotItemDto(
            spotNm = "중소형 폐가전 수거함",
            addrBase = "서울특별시 구로구 구로동",
            addrDtl = "",
        )

        val result = spotMapper.mapToDomain(dto)

        assertNull(result.detailLocation)
    }

    @Test
    fun `SpotItemDto 리스트를 CollectionSpot 리스트로 변환한다`() {
        val dtoList = listOf(
            SpotItemDto(
                spotNm = "폐건전지 수거함",
                addrBase = "서울특별시 영등포구 문래동",
                addrDtl = "주민센터 앞",
            ),
            SpotItemDto(
                spotNm = "재활용센터",
                addrBase = "서울특별시 구로구 구로동",
                addrDtl = "센터 입구",
            ),
        )

        val result = spotMapper.mapToDomainList(dtoList)

        assertEquals(2, result.size)
        assertEquals(CollectionSpotType.BATTERY_BIN, result[0].type)
        assertEquals(CollectionSpotType.RECYCLING_CENTER, result[1].type)
    }
}