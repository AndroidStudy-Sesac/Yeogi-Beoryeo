package com.team.yeogibeoryeo.presentation.regionalguide.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RegionSearchCandidateUiModelTest {

    @Test
    fun `검색 후보 안정 키는 표시 문구가 아니라 지역 값 조합으로 만든다`() {
        val candidate = RegionSearchCandidateUiModel(
            sido = "경기도",
            sigungu = "성남시",
            eupmyeondong = "정자동"
        )

        assertEquals("경기도 > 성남시 > 정자동", candidate.displayText)
        assertEquals("sido=경기도|sigungu=성남시|eupmyeondong=정자동", candidate.stableKey)
        assertNotEquals(candidate.displayText, candidate.stableKey)
    }

    @Test
    fun `검색 후보 안정 키는 빈 지역 값을 명시적으로 구분한다`() {
        val sigunguCandidate = RegionSearchCandidateUiModel(
            sido = "서울특별시",
            sigungu = "중구",
            eupmyeondong = null
        )
        val eupmyeondongCandidate = RegionSearchCandidateUiModel(
            sido = "서울특별시",
            sigungu = "중구",
            eupmyeondong = "중구"
        )

        assertNotEquals(sigunguCandidate.stableKey, eupmyeondongCandidate.stableKey)
    }
}
