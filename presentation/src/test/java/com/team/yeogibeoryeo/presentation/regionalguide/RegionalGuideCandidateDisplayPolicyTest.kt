package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideCandidateUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideUiModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionalGuideCandidateDisplayPolicyTest {

    @Test
    fun `직접 매칭 실패 후보가 수거 유형 후보이면 수거 유형 패널을 표시한다`() {
        val state = guideCandidates(
            reason = RegionalGuideCandidateReason.FALLBACK_BECAUSE_DIRECT_MATCH_NOT_FOUND,
            candidates = listOf(
                candidate(
                    managementZoneName = "문전수거 지역",
                    targetRegionName = "문전수거 지역",
                    disposalPlaceType = "문전수거"
                ),
                candidate(
                    managementZoneName = "거점수거 지역",
                    targetRegionName = "거점수거 지역",
                    disposalPlaceType = "거점수거"
                )
            )
        )

        assertTrue(state.shouldShowCollectionTypeSelectionPanel())
    }

    @Test
    fun `직접 매칭 실패 후보라도 일반 권역 후보이면 기존 리스트를 유지한다`() {
        val state = guideCandidates(
            reason = RegionalGuideCandidateReason.FALLBACK_BECAUSE_DIRECT_MATCH_NOT_FOUND,
            candidates = listOf(
                candidate(
                    managementZoneName = "1권역",
                    targetRegionName = "갑천면",
                    disposalPlaceType = "문전수거"
                ),
                candidate(
                    managementZoneName = "2권역",
                    targetRegionName = "공근면",
                    disposalPlaceType = "거점수거"
                )
            )
        )

        assertFalse(state.shouldShowCollectionTypeSelectionPanel())
    }

    @Test
    fun `시군구 전체 기준 수거 유형 후보만 있으면 수거 유형 패널을 표시한다`() {
        val state = guideCandidates(
            reason = RegionalGuideCandidateReason.MULTIPLE_CANDIDATES,
            candidates = listOf(
                candidate(
                    managementZoneName = "없음",
                    targetRegionName = "없음",
                    disposalPlaceType = "거점수거"
                ),
                candidate(
                    managementZoneName = "없음",
                    targetRegionName = "없음",
                    disposalPlaceType = "문전수거"
                )
            )
        )

        assertTrue(state.shouldShowCollectionTypeSelectionPanel())
    }

    @Test
    fun `복수 정확 매칭 후보는 수거 유형 패널로 표시하지 않는다`() {
        val state = guideCandidates(
            reason = RegionalGuideCandidateReason.MULTIPLE_EXACT_MATCHES,
            candidates = listOf(
                candidate(
                    managementZoneName = "노은2동",
                    targetRegionName = "반석동",
                    disposalPlaceType = "문전수거"
                ),
                candidate(
                    managementZoneName = "노은3동",
                    targetRegionName = "반석동",
                    disposalPlaceType = "문전수거"
                )
            )
        )

        assertFalse(state.shouldShowCollectionTypeSelectionPanel())
    }

    @Test
    fun `즐겨찾기 복원 모호 후보는 수거 유형 패널로 표시하지 않는다`() {
        val state = guideCandidates(
            reason = RegionalGuideCandidateReason.FAVORITE_RESTORE_AMBIGUOUS,
            candidates = listOf(
                candidate(
                    managementZoneName = "문전수거 지역",
                    targetRegionName = "문전수거 지역",
                    disposalPlaceType = "문전수거"
                ),
                candidate(
                    managementZoneName = "거점수거 지역",
                    targetRegionName = "거점수거 지역",
                    disposalPlaceType = "거점수거"
                )
            )
        )

        assertFalse(state.shouldShowCollectionTypeSelectionPanel())
    }

    private fun guideCandidates(
        reason: RegionalGuideCandidateReason,
        candidates: List<RegionalGuideCandidateUiModel>
    ): RegionalGuideUiState.GuideCandidates =
        RegionalGuideUiState.GuideCandidates(
            query = "검색어",
            reason = reason,
            candidates = candidates,
        )

    private fun candidate(
        managementZoneName: String?,
        targetRegionName: String?,
        disposalPlaceType: String?
    ): RegionalGuideCandidateUiModel =
        RegionalGuideCandidateUiModel(
            guide = RegionalGuideUiModel(
                regionName = "강원특별자치도 양구군",
                managementZoneName = managementZoneName,
                targetRegionName = targetRegionName,
                disposalPlaceType = disposalPlaceType,
                disposalPlaceDescription = null,
                schedules = emptyList(),
                uncollectedDays = null,
                departmentInfo = null,
            ),
            sido = "강원특별자치도",
            sigungu = "양구군",
            eupmyeondong = null,
        )
}
