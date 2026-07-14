package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideCandidateUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class RegionalGuideCandidateMessageTest {

    @Test
    fun `대체 후보 사유는 대체 안내 문구 리소스로 매핑된다`() {
        assertEquals(
            R.string.regional_guide_candidate_fallback_message,
            RegionalGuideCandidateReason.FALLBACK_BECAUSE_DIRECT_MATCH_NOT_FOUND.messageResId()
        )
    }

    @Test
    fun `즐겨찾기 복원 모호 사유는 즐겨찾기 복원 안내 문구 리소스로 매핑된다`() {
        assertEquals(
            R.string.regional_guide_candidate_favorite_restore_ambiguous_message,
            RegionalGuideCandidateReason.FAVORITE_RESTORE_AMBIGUOUS.messageResId()
        )
    }

    @Test
    fun `대체 후보 안내는 선택 읍면동과 시군구를 문구 인자로 전달한다`() {
        val state = RegionalGuideUiState.GuideCandidates(
            query = "사천면",
            reason = RegionalGuideCandidateReason.FALLBACK_BECAUSE_DIRECT_MATCH_NOT_FOUND,
            candidates = listOf(
                candidate(
                    sido = "강원특별자치도",
                    sigungu = "강릉시",
                    eupmyeondong = "사천면",
                )
            )
        )

        val spec = state.candidateMessageSpec()

        assertEquals(R.string.regional_guide_candidate_fallback_panel_title, spec.titleResId)
        assertEquals(emptyList<String>(), spec.titleArgs)
        assertEquals(
            R.string.regional_guide_candidate_fallback_panel_description,
            spec.descriptionResId
        )
        assertEquals(listOf("사천면", "강릉시"), spec.descriptionArgs)
        assertEquals(R.string.regional_guide_candidate_fallback_section_title, spec.sectionTitleResId)
    }

    @Test
    fun `대체 후보에 지역명이 없으면 기본 안내 문구를 사용한다`() {
        val state = RegionalGuideUiState.GuideCandidates(
            query = "검색어",
            reason = RegionalGuideCandidateReason.FALLBACK_BECAUSE_DIRECT_MATCH_NOT_FOUND,
            candidates = listOf(candidate())
        )

        val spec = state.candidateMessageSpec()

        assertEquals(R.string.regional_guide_candidate_fallback_panel_title, spec.titleResId)
        assertEquals(emptyList<String>(), spec.titleArgs)
        assertEquals(
            R.string.regional_guide_candidate_fallback_panel_description_without_region,
            spec.descriptionResId
        )
        assertEquals(emptyList<String>(), spec.descriptionArgs)
    }

    @Test
    fun `시군구 전체 기준 수거 유형 후보는 수거 유형 선택 안내 문구를 사용한다`() {
        val state = RegionalGuideUiState.GuideCandidates(
            query = "양구",
            reason = RegionalGuideCandidateReason.MULTIPLE_CANDIDATES,
            candidates = listOf(
                candidate(sigungu = "양구군"),
                candidate(sigungu = "양구군")
            )
        )

        val spec = state.collectionTypeSelectionMessageSpec()

        assertEquals(R.string.regional_guide_candidate_collection_type_panel_title, spec.titleResId)
        assertEquals(
            R.string.regional_guide_candidate_collection_type_panel_description,
            spec.descriptionResId
        )
        assertEquals(listOf("양구군"), spec.descriptionArgs)
        assertEquals(R.string.regional_guide_candidate_fallback_section_title, spec.sectionTitleResId)
    }

    private fun candidate(
        sido: String? = null,
        sigungu: String? = null,
        eupmyeondong: String? = null,
    ): RegionalGuideCandidateUiModel =
        RegionalGuideCandidateUiModel(
            guide = RegionalGuideUiModel(
                regionName = "지역",
                managementZoneName = "관리 권역",
                targetRegionName = "대상 지역",
                disposalPlaceType = "문전수거",
                disposalPlaceDescription = "문전",
                schedules = emptyList(),
                uncollectedDays = null,
                departmentInfo = null,
            ),
            sido = sido,
            sigungu = sigungu,
            eupmyeondong = eupmyeondong,
        )
}
