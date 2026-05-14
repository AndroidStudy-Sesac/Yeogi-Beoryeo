package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.domain.region.model.Region
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegionNormalizerTest {

    @Test
    fun `서울 축약명이 공식 명칭으로 변환된다`() {
        val region = Region(
            sido = "서울",
            sigungu = "강남구"
        )

        val result = RegionNormalizer.normalize(region)

        assertEquals("서울특별시", result.sido)
    }

    @Test
    fun `세종특별자치시는 sigungu가 제거된다`() {
        val region = Region(
            sido = "세종",
            sigungu = "세종시"
        )

        val result = RegionNormalizer.normalize(region)

        assertEquals("세종특별자치시", result.sido)
        assertNull(result.sigungu)
    }
}