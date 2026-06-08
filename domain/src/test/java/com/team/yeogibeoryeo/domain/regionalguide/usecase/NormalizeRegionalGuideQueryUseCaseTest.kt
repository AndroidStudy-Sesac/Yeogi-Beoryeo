package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NormalizeRegionalGuideQueryUseCaseTest {

    private val useCase = NormalizeRegionalGuideQueryUseCase()

    @Test
    fun `일반 시군구는 그대로 info 조회 key로 사용한다`() {
        val result = useCase(
            Region(
                sido = "서울특별시",
                sigungu = "영등포구",
                eupmyeondong = "문래동"
            )
        )

        assertEquals("영등포구", result?.sigunguQuery)
        assertEquals("서울특별시", result?.displayRegion?.sido)
        assertEquals("영등포구", result?.displayRegion?.sigungu)
        assertEquals("문래동", result?.displayRegion?.eupmyeondong)
    }

    @Test
    fun `행정구가 포함된 시군구는 info 조회용 시 단위 key로 정규화한다`() {
        val result = useCase(
            Region(
                sido = "경기도",
                sigungu = "수원시 장안구",
                eupmyeondong = "파장동"
            )
        )

        assertEquals("수원시", result?.sigunguQuery)
        assertEquals("수원시 장안구", result?.displayRegion?.sigungu)
    }

    @Test
    fun `행정구가 포함된 시군구의 표시 지역은 유지하고 조회 key만 시 단위로 정규화한다`() {
        val result = useCase(
            Region(
                sido = "경기도",
                sigungu = "수원시 장안구",
                eupmyeondong = null
            )
        )

        assertEquals("수원시", result?.sigunguQuery)
        assertEquals("경기도", result?.displayRegion?.sido)
        assertEquals("수원시 장안구", result?.displayRegion?.sigungu)
        assertNull(result?.displayRegion?.eupmyeondong)
    }

    @Test
    fun `세종특별자치시는 없음 key로 조회하고 표시 region의 시군구는 비운다`() {
        val result = useCase(
            Region(
                sido = "세종특별자치시",
                sigungu = "세종특별자치시",
                eupmyeondong = "한솔동"
            )
        )

        assertEquals("없음", result?.sigunguQuery)
        assertEquals("세종특별자치시", result?.displayRegion?.sido)
        assertNull(result?.displayRegion?.sigungu)
        assertEquals("한솔동", result?.displayRegion?.eupmyeondong)
    }

    @Test
    fun `세종특별자치시가 아니고 시군구가 없으면 null을 반환한다`() {
        val result = useCase(
            Region(
                sido = "서울특별시",
                sigungu = null,
                eupmyeondong = "문래동"
            )
        )

        assertNull(result)
    }
}
