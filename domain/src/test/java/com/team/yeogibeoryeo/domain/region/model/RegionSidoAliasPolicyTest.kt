package com.team.yeogibeoryeo.domain.region.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionSidoAliasPolicyTest {

    @Test
    fun `전남광주통합특별시와 광주 5개 구 입력은 광주광역시로 정규화된다`() {
        val gwangjuSigunguNames = listOf("동구", "서구", "남구", "북구", "광산구")

        gwangjuSigunguNames.forEach { sigungu ->
            val result = RegionSidoAliasPolicy.normalizeSidoForInput(
                sido = "전남광주통합특별시",
                sigungu = sigungu,
            )

            assertEquals("광주광역시", result)
        }
    }

    @Test
    fun `전남광주통합특별시와 전남 시군 입력은 전라남도로 정규화된다`() {
        val result = RegionSidoAliasPolicy.normalizeSidoForInput(
            sido = "전남광주통합특별시",
            sigungu = "고흥군",
        )

        assertEquals("전라남도", result)
    }

    @Test
    fun `광주광역시는 전남광주통합특별시의 광주 5개 구와 같은 시도로 비교된다`() {
        val gwangjuSigunguNames = listOf("동구", "서구", "남구", "북구", "광산구")

        gwangjuSigunguNames.forEach { sigungu ->
            assertTrue(
                RegionSidoAliasPolicy.isSameSido(
                    requestedSido = "광주광역시",
                    candidateSido = "전남광주통합특별시",
                    candidateSigungu = sigungu,
                )
            )
        }
    }

    @Test
    fun `광주광역시는 전남광주통합특별시의 전남 시군과 같은 시도로 비교되지 않는다`() {
        assertFalse(
            RegionSidoAliasPolicy.isSameSido(
                requestedSido = "광주광역시",
                candidateSido = "전남광주통합특별시",
                candidateSigungu = "고흥군",
            )
        )
    }

    @Test
    fun `전라남도는 전남광주통합특별시의 전남 시군과 같은 시도로 비교된다`() {
        assertTrue(
            RegionSidoAliasPolicy.isSameSido(
                requestedSido = "전라남도",
                candidateSido = "전남광주통합특별시",
                candidateSigungu = "고흥군",
            )
        )
    }

    @Test
    fun `기존 동일 시도 비교는 유지된다`() {
        assertTrue(
            RegionSidoAliasPolicy.isSameSido(
                requestedSido = "서울특별시",
                candidateSido = "서울특별시",
                candidateSigungu = "중구",
            )
        )
        assertFalse(
            RegionSidoAliasPolicy.isSameSido(
                requestedSido = "서울특별시",
                candidateSido = "대구광역시",
                candidateSigungu = "중구",
            )
        )
    }
}
