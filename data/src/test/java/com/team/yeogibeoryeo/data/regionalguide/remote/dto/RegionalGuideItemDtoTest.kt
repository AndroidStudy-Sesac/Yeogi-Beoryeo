package com.team.yeogibeoryeo.data.regionalguide.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class RegionalGuideItemDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `실제 info 응답 필드로 배출장소 관리부서 연락처 대형폐기물 배출장소를 파싱한다`() {
        val dto = json.decodeFromString<RegionalGuideItemDto>(
            """
            {
              "EMSN_PLC": "문 앞",
              "EMSN_PLC_TYPE": "문전수거",
              "MNG_DEPT_NM": "청소행정과",
              "MNG_DEPT_TELNO": "02-1234-5678",
              "TMPRY_BULK_WASTE_EMSN_PLC": "대형폐기물 지정 장소"
            }
            """.trimIndent()
        )

        assertEquals("문 앞", dto.disposalPlace)
        assertEquals("문전수거", dto.disposalPlaceType)
        assertEquals("청소행정과", dto.departmentName)
        assertEquals("02-1234-5678", dto.departmentPhoneNumber)
        assertEquals("대형폐기물 지정 장소", dto.largeItemDisposalPlace)
    }

    @Test
    fun `응답 출처 메타데이터 필드를 파싱한다`() {
        val dto = json.decodeFromString<RegionalGuideItemDto>(
            """
            {
              "MNG_NO": "MNG-1",
              "LAST_MDFCN_PNT": "20240709010101",
              "DAT_CRTR_YMD": "20240709",
              "DAT_UPDT_PNT": "20240710000000",
              "DAT_UPDT_SE": "U"
            }
            """.trimIndent()
        )

        assertEquals("MNG-1", dto.managementNumber)
        assertEquals("20240709010101", dto.lastModifiedPoint)
        assertEquals("20240709", dto.dataCriteriaDate)
        assertEquals("20240710000000", dto.dataUpdatedPoint)
        assertEquals("U", dto.dataUpdateType)
    }

    @Test
    fun `대형폐기물 배출 요일 없이도 나머지 필드를 파싱한다`() {
        val dto = json.decodeFromString<RegionalGuideItemDto>(
            """
            {
              "TMPRY_BULK_WASTE_EMSN_PLC": "대형폐기물 지정 장소",
              "TMPRY_BULK_WASTE_EMSN_BGNG_TM": "1800",
              "TMPRY_BULK_WASTE_EMSN_MTHD": "사전 신청 후 배출"
            }
            """.trimIndent()
        )

        assertEquals("대형폐기물 지정 장소", dto.largeItemDisposalPlace)
        assertEquals("1800", dto.largeItemStartTime)
        assertEquals("사전 신청 후 배출", dto.largeItemMethod)
    }
}
