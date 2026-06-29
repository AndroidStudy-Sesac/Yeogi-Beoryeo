package com.team.yeogibeoryeo.presentation.map.formatter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DistanceFormatterTest {
    @Test
    fun `1000m 미만은 m 단위로 표시한다`() {
        assertEquals("120m", DistanceFormatter.format(120))
        assertEquals("850m", DistanceFormatter.format(850))
    }

    @Test
    fun `1000m 이상은 km 단위 한 자리로 표시한다`() {
        assertEquals("1.2km", DistanceFormatter.format(1_200))
        assertEquals("2.5km", DistanceFormatter.format(2_500))
    }

    @Test
    fun `km 소수점이 0이면 정수 km로 표시한다`() {
        assertEquals("1km", DistanceFormatter.format(1_000))
        assertEquals("2km", DistanceFormatter.format(2_000))
    }

    @Test
    fun `0m는 거리 값으로 표시한다`() {
        assertEquals("0m", DistanceFormatter.format(0))
    }

    @Test
    fun `null 또는 음수 거리는 표시하지 않는다`() {
        assertNull(DistanceFormatter.format(null))
        assertNull(DistanceFormatter.format(-1))
    }
}
