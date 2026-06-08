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
    fun `표준 주소에서 시도 시군구 읍면동을 추출한다`() {
        val result = parser.parse(
            "서울특별시 강남구 역삼동 123-45"
        )

        assertEquals("서울특별시", result.sido)
        assertEquals("강남구", result.sigungu)
        assertEquals("역삼동", result.eupmyeondong)
    }

    @Test
    fun `getSpot 도로명 주소에서 시도와 시군구를 추출하고 읍면동은 null 처리한다`() {
        val result = parser.parse(
            "서울특별시 영등포구 당산로 42"
        )

        assertEquals("서울특별시", result.sido)
        assertEquals("영등포구", result.sigungu)
        assertNull(result.eupmyeondong)
    }

    @Test
    fun `괄호 안 동명이 있는 getSpot 주소에서 읍면동을 추출한다`() {
        val result = parser.parse(
            "서울특별시 영등포구 문래로 110 (문래동)"
        )

        assertEquals("서울특별시", result.sido)
        assertEquals("영등포구", result.sigungu)
        assertEquals("문래동", result.eupmyeondong)
    }

    @Test
    fun `일반 주소에서 시도 시군구 읍면동을 추출한다`() {
        val result = parser.parse(
            "서울특별시 영등포구 문래동 55-1"
        )

        assertEquals("서울특별시", result.sido)
        assertEquals("영등포구", result.sigungu)
        assertEquals("문래동", result.eupmyeondong)
    }

    @Test
    fun `세종특별자치시는 sigungu가 null 처리되고 읍면동을 추출한다`() {
        val result = parser.parse(
            "세종특별자치시 조치원읍 새내로 122"
        )

        assertEquals("세종특별자치시", result.sido)
        assertNull(result.sigungu)
        assertEquals("조치원읍", result.eupmyeondong)
    }

    @Test
    fun `축약 시도명을 정식 시도명으로 정규화한다`() {
        val result = parser.parse(
            "서울 영등포구 문래동"
        )

        assertEquals("서울특별시", result.sido)
        assertEquals("영등포구", result.sigungu)
        assertEquals("문래동", result.eupmyeondong)
    }

    @Test
    fun `시 단독 검색어는 시도가 아닌 시군구로 파싱된다`() {
        val result = parser.parse(
            "수원시"
        )

        assertNull(result.sido)
        assertEquals("수원시", result.sigungu)
        assertNull(result.eupmyeondong)
    }

    @Test
    fun `광주시 단독 검색어는 광주광역시가 아닌 시군구로 파싱된다`() {
        val result = parser.parse(
            "광주시"
        )

        assertNull(result.sido)
        assertEquals("광주시", result.sigungu)
        assertNull(result.eupmyeondong)
    }

    @Test
    fun `광주 축약 시도명은 광주광역시로 정규화된다`() {
        val result = parser.parse(
            "광주 북구 문흥동"
        )

        assertEquals("광주광역시", result.sido)
        assertEquals("북구", result.sigungu)
        assertEquals("문흥동", result.eupmyeondong)
    }

    @Test
    fun `과거 시도명은 현재 공식 시도명으로 정규화된다`() {
        val result = parser.parse(
            "강원도 춘천시 후평동"
        )

        assertEquals("강원특별자치도", result.sido)
        assertEquals("춘천시", result.sigungu)
        assertEquals("후평동", result.eupmyeondong)
    }

    @Test
    fun `불규칙한 공백이 포함되어도 정상 파싱된다`() {
        val result = parser.parse(
            "  경기도   수원시  영통구 망포동   "
        )

        assertEquals("경기도", result.sido)
        assertEquals("수원시 영통구", result.sigungu)
        assertEquals("망포동", result.eupmyeondong)
    }

    @Test
    fun `시와 구가 함께 입력되면 시군구를 행정구 포함 형태로 파싱한다`() {
        val result = parser.parse(
            "경기도 수원시 장안구 파장동"
        )

        assertEquals("경기도", result.sido)
        assertEquals("수원시 장안구", result.sigungu)
        assertEquals("파장동", result.eupmyeondong)
    }

    @Test
    fun `시도 없이 시와 구만 입력되어도 시군구를 행정구 포함 형태로 파싱한다`() {
        val result = parser.parse(
            "수원시 장안구"
        )

        assertNull(result.sido)
        assertEquals("수원시 장안구", result.sigungu)
        assertNull(result.eupmyeondong)
    }
}
