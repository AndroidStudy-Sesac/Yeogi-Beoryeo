package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.parser.RegionAddressParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class RegionRepositoryImplTest {

    private lateinit var repository: RegionRepositoryImpl

    @Before
    fun setUp() {
        repository = RegionRepositoryImpl(
            addressParser = RegionAddressParser()
        )
    }

    @Test
    fun `주소 기반 Region 추출이 정상 동작한다`() {
        val result = repository.extractRegionFromAddress(
            "서울특별시 영등포구 당산동"
        )

        assertEquals("서울특별시", result?.sido)
        assertEquals("영등포구", result?.sigungu)
        assertEquals("당산동", result?.eupmyeondong)
    }

    @Test
    fun `빈 주소는 null을 반환한다`() {
        val result = repository.extractRegionFromAddress("")

        assertNull(result)
    }

    @Test
    fun `지역 정보를 찾을 수 없는 주소는 null을 반환한다`() {
        val result = repository.extractRegionFromAddress(
            "주소 정보 없음"
        )

        assertNull(result)
    }

    @Test
    fun `검색어 기반 Region 변환이 정상 동작한다`() = runBlocking {
        val result = repository.resolveRegionFromKeyword(
            "서울 영등포구"
        )

        assertEquals("서울특별시", result?.sido)
        assertEquals("영등포구", result?.sigungu)
    }

    @Test
    fun `시 단독 검색어는 시군구로 변환된다`() = runBlocking {
        val result = repository.resolveRegionFromKeyword(
            "수원시"
        )

        assertNull(result?.sido)
        assertEquals("수원시", result?.sigungu)
        assertNull(result?.eupmyeondong)
    }

    @Test
    fun `광주시 단독 검색어는 광주광역시가 아닌 시군구로 변환된다`() = runBlocking {
        val result = repository.resolveRegionFromKeyword(
            "광주시"
        )

        assertNull(result?.sido)
        assertEquals("광주시", result?.sigungu)
        assertNull(result?.eupmyeondong)
    }

    @Test
    fun `광주 축약 시도명은 광주광역시로 변환된다`() = runBlocking {
        val result = repository.resolveRegionFromKeyword(
            "광주 북구"
        )

        assertEquals("광주광역시", result?.sido)
        assertEquals("북구", result?.sigungu)
    }

    @Test
    fun `과거 시도명은 현재 공식 시도명으로 변환된다`() = runBlocking {
        val result = repository.resolveRegionFromKeyword(
            "전라북도 전주시"
        )

        assertEquals("전북특별자치도", result?.sido)
        assertEquals("전주시", result?.sigungu)
    }
}
