package com.team.yeogibeoryeo.domain.region.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class ClassifyRegionSearchInputUseCaseTest {

    private val useCase = ClassifyRegionSearchInputUseCase()

    @Test
    fun `도로명 주소는 주소 입력으로 분류한다`() {
        val result = useCase("서울시 중구 남대문로 63")

        assertEquals(RegionSearchInputType.ADDRESS, result)
    }

    @Test
    fun `괄호와 건물명이 포함된 도로명 주소는 주소 입력으로 분류한다`() {
        val result = useCase("서울시 중구 남대문로 63 (남대문로2가한진빌딩)")

        assertEquals(RegionSearchInputType.ADDRESS, result)
    }

    @Test
    fun `읍면동 없이 시도와 시군구와 상세 주소가 있으면 주소 입력으로 분류한다`() {
        val result = useCase("서울특별시 중구 63")

        assertEquals(RegionSearchInputType.ADDRESS, result)
    }

    @Test
    fun `지번 주소 형태는 주소 입력으로 분류한다`() {
        val result = useCase("서울특별시 중구 남대문로2가 123-4")

        assertEquals(RegionSearchInputType.ADDRESS, result)
    }

    @Test
    fun `시도와 시군구만 있는 입력은 지역명 검색으로 분류한다`() {
        val result = useCase("경기도 성남시")

        assertEquals(RegionSearchInputType.REGION_KEYWORD, result)
    }

    @Test
    fun `행정구 포함 시군구만 있는 입력은 지역명 검색으로 분류한다`() {
        val result = useCase("수원시 장안구")

        assertEquals(RegionSearchInputType.REGION_KEYWORD, result)
    }

    @Test
    fun `숫자가 포함된 읍면동 이름은 주소로 분류하지 않는다`() {
        val result = useCase("온양1동")

        assertEquals(RegionSearchInputType.REGION_KEYWORD, result)
    }

    @Test
    fun `지역 범위 없이 숫자만 포함된 입력은 주소로 분류하지 않는다`() {
        val result = useCase("남대문로 63")

        assertEquals(RegionSearchInputType.REGION_KEYWORD, result)
    }
}
