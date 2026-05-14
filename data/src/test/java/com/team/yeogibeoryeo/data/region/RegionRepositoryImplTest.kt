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
    }

    @Test
    fun `빈 주소는 null 반환`() {
        val result = repository.extractRegionFromAddress("")

        assertNull(result)
    }

    @Test
    fun `검색어 기반 Region 변환이 정상 동작한다`() = runBlocking {
        val result = repository.resolveRegionFromKeyword(
            "서울 영등포구"
        )

        assertEquals("영등포구", result?.sigungu)
    }
}