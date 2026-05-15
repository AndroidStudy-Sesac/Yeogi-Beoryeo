package com.team.yeogibeoryeo.data.region.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class RegionAddressParserTest {

    private lateinit var parser: RegionAddressParser

    @Before
    fun setUp() {
        parser = RegionAddressParser()
    }

    @Test
    fun `표준 주소가 정상 파싱된다`() {
        val result = parser.parse(
            "서울특별시 강남구 역삼동 123-45"
        )

        assertEquals("서울특별시", result.sido)
        assertEquals("강남구", result.sigungu)
        assertEquals("역삼동", result.eupmyeondong)
    }

    @Test
    fun `getSpot addrBase 포맷이 정상 파싱된다`() {
        val result = parser.parse(
            "서울특별시 영등포구 당산로 42"
        )

        assertEquals("서울특별시", result.sido)
        assertEquals("영등포구", result.sigungu)

        // 도로명 주소이므로 읍면동 없음
        assertNull(result.eupmyeondong)
    }

    @Test
    fun `세종특별자치시는 sigungu가 null 처리된다`() {
        val result = parser.parse(
            "세종특별자치시 한솔동 123"
        )

        assertEquals("세종특별자치시", result.sido)
        assertNull(result.sigungu)
        assertEquals("한솔동", result.eupmyeondong)
    }

    @Test
    fun `축약 시도명이 정상 정규화된다`() {
        val result = parser.parse(
            "서울 강남구 역삼동"
        )

        assertEquals("서울특별시", result.sido)
        assertEquals("강남구", result.sigungu)
        assertEquals("역삼동", result.eupmyeondong)
    }

    @Test
    fun `불규칙한 공백이 포함되어도 정상 파싱된다`() {
        val result = parser.parse(
            "  경기도   수원시  영통구 망포동   "
        )

        assertEquals("경기도", result.sido)
        assertEquals("수원시", result.sigungu)
        assertEquals("망포동", result.eupmyeondong)
    }
}