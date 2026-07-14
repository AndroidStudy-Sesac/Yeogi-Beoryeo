package com.team.yeogibeoryeo.data.regionalguide.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class RegionalGuideItemDtoTest {

    @Test
    fun `실제 info 응답 필드를 DTO 속성으로 파싱한다`() {
        val json =
            """
            {
              "EMSN_PLC": "문전 배출",
              "MNG_DEPT_NM": "청소행정과",
              "MNG_DEPT_TELNO": "02-1234-5678",
              "TMPRY_BULK_WASTE_EMSN_PLC": "신고 후 집 앞"
            }
            """.trimIndent()

        val dto = Json.decodeFromString<RegionalGuideItemDto>(json)

        assertEquals("문전 배출", dto.placeDescription)
        assertEquals("청소행정과", dto.departmentName)
        assertEquals("02-1234-5678", dto.departmentPhoneNumber)
        assertEquals("신고 후 집 앞", dto.largeItemDisposalPlace)
    }
}
