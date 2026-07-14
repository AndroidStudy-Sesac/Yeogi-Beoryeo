package com.team.yeogibeoryeo.domain.region.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionSidoAliasPolicyTest {

    @Test
    fun `시도 축약명과 과거 명칭은 현재 공식 명칭으로 정규화된다`() {
        val aliases = mapOf(
            "서울" to "서울특별시",
            "광주" to "광주광역시",
            "전남" to "전라남도",
            "전북" to "전북특별자치도",
            "강원도" to "강원특별자치도",
            "전라북도" to "전북특별자치도",
        )

        aliases.forEach { (input, expected) ->
            assertEquals(expected, RegionSidoAliasPolicy.normalizeSidoName(input))
        }
    }

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
                    requestedSigungu = sigungu,
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
                requestedSigungu = "고흥군",
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
                requestedSigungu = "고흥군",
                candidateSido = "전남광주통합특별시",
                candidateSigungu = "고흥군",
            )
        )
    }

    @Test
    fun `시도 축약명은 전남광주통합특별시 후보와 같은 기준으로 비교된다`() {
        assertTrue(
            RegionSidoAliasPolicy.isSameSido(
                requestedSido = "광주",
                requestedSigungu = "서구",
                candidateSido = "전남광주통합특별시",
                candidateSigungu = "서구",
            )
        )
        assertTrue(
            RegionSidoAliasPolicy.isSameSido(
                requestedSido = "전남",
                requestedSigungu = "광양시",
                candidateSido = "전남광주통합특별시",
                candidateSigungu = "광양시",
            )
        )
    }

    @Test
    fun `전남광주통합특별시 광주 요청은 현재 광주광역시 후보와 같은 시도로 비교된다`() {
        assertTrue(
            RegionSidoAliasPolicy.isSameSido(
                requestedSido = "전남광주통합특별시",
                requestedSigungu = "서구",
                candidateSido = "광주광역시",
                candidateSigungu = "서구",
            )
        )
    }

    @Test
    fun `전남광주통합특별시 전남 요청은 현재 전라남도 후보와 같은 시도로 비교된다`() {
        assertTrue(
            RegionSidoAliasPolicy.isSameSido(
                requestedSido = "전남광주통합특별시",
                requestedSigungu = "나주시",
                candidateSido = "전라남도",
                candidateSigungu = "나주시",
            )
        )
    }

    @Test
    fun `기존 동일 시도 비교는 유지된다`() {
        assertTrue(
            RegionSidoAliasPolicy.isSameSido(
                requestedSido = "서울특별시",
                requestedSigungu = "중구",
                candidateSido = "서울특별시",
                candidateSigungu = "중구",
            )
        )
        assertFalse(
            RegionSidoAliasPolicy.isSameSido(
                requestedSido = "서울특별시",
                requestedSigungu = "중구",
                candidateSido = "대구광역시",
                candidateSigungu = "중구",
            )
        )
    }
}
