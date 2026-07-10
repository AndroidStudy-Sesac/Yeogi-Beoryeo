package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.local.dto.LegalAdminDongMappingDto
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LegalAdminDongMappingAssetTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `법정동 행정동 매핑 자산을 파싱하고 하계 매핑을 확인할 수 있다`() {
        val mappings = json.decodeFromString<List<LegalAdminDongMappingDto>>(
            File(ASSET_PATH).readText()
        )

        val haggyeAdminDongNames = mappings
            .filter { mapping ->
                mapping.sidoName == "서울특별시" &&
                    mapping.sigunguName == "노원구" &&
                    mapping.legalDongName == "하계동"
            }
            .map { mapping -> mapping.adminDongName }
            .toSet()

        assertTrue(mappings.isNotEmpty())
        assertTrue("하계1동" in haggyeAdminDongNames)
        assertTrue("하계2동" in haggyeAdminDongNames)
    }

    @Test
    fun `법정동 행정동 매핑 dto는 조회 키 필드를 유지한다`() {
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<List<LegalAdminDongMappingDto>>(
                """
                [
                  {
                    "legalCode": "1100000000"
                  }
                ]
                """.trimIndent()
            )
        }
    }

    private companion object {
        const val ASSET_PATH =
            "src/main/assets/region/legal_to_admin_mappings.20260701.json"
    }
}
