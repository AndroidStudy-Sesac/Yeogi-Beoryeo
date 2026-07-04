package com.team.yeogibeoryeo.domain.spot.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class NormalizeCollectionSpotSearchKeywordUseCaseTest {

    private val useCase = NormalizeCollectionSpotSearchKeywordUseCase()

    @Test
    fun `동 단독 검색어는 그대로 유지한다`() {
        assertEquals("금호동", useCase("금호동"))
    }

    @Test
    fun `구 동 조합에서 동을 추출한다`() {
        assertEquals("금호동", useCase("성동구 금호동"))
    }

    @Test
    fun `시 구 동 조합에서 동을 추출한다`() {
        assertEquals("금호동", useCase("서울 성동구 금호동"))
    }

    @Test
    fun `정식 시도 구 동 조합에서 동을 추출한다`() {
        assertEquals("금호동", useCase("서울특별시 성동구 금호동"))
    }

    @Test
    fun `중구 명동 조합에서 명동을 추출한다`() {
        assertEquals("명동", useCase("중구 명동"))
    }

    @Test
    fun `서울 중구 명동 조합에서 명동을 추출한다`() {
        assertEquals("명동", useCase("서울 중구 명동"))
    }

    @Test
    fun `서울특별시 중구 명동 조합에서 명동을 추출한다`() {
        assertEquals("명동", useCase("서울특별시 중구 명동"))
    }

    @Test
    fun `숫자가 포함된 법정동 가 단독 검색어는 그대로 유지한다`() {
        assertEquals("금호동3가", useCase("금호동3가"))
    }

    @Test
    fun `구 법정동 가 조합에서 법정동을 추출한다`() {
        assertEquals("금호동3가", useCase("성동구 금호동3가"))
    }

    @Test
    fun `도로명 주소는 동으로 잘못 보정하지 않는다`() {
        assertEquals("성동구 독서당로 303-5", useCase("성동구 독서당로 303-5"))
    }

    @Test
    fun `시 구 도로명 주소는 동으로 잘못 보정하지 않는다`() {
        assertEquals("서울 성동구 독서당로 303-5", useCase("서울 성동구 독서당로 303-5"))
    }

    @Test
    fun `정식 시도 구 도로명 주소는 동으로 잘못 보정하지 않는다`() {
        assertEquals("서울특별시 성동구 독서당로 303-5", useCase("서울특별시 성동구 독서당로 303-5"))
    }

    @Test
    fun `도로명과 번지만 있는 입력은 그대로 유지한다`() {
        assertEquals("강남대로 123", useCase("강남대로 123"))
    }

    @Test
    fun `지번 상세 주소는 동으로 잘못 보정하지 않는다`() {
        assertEquals("역삼동 123-4", useCase("역삼동 123-4"))
    }

    @Test
    fun `빈 문자열과 공백 문자열을 안전하게 처리한다`() {
        assertEquals("", useCase(""))
        assertEquals("", useCase("   "))
    }
}
