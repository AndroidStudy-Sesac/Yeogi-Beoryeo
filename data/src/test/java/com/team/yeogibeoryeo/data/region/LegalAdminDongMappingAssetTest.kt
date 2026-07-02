package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.local.dto.LegalAdminDongMappingDto
import java.io.File
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class LegalAdminDongMappingAssetTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `legal admin mapping asset can be decoded and contains haggye mappings`() {
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
    fun `legal admin mapping dto requires lookup key fields`() {
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
            "src/main/assets/region/legal_to_admin_mappings.20260325.json"
    }
}
