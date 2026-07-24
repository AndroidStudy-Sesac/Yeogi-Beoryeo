package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideCandidateLookupReason
import org.junit.Assert.assertEquals
import org.junit.Test

class RegionalGuideCandidatePresentationPolicyTest {

    @Test
    fun `즐겨찾기 복원 후보는 원본 조회 사유 대신 복원 안내 사유를 사용한다`() {
        val result = RegionalGuideCandidatePresentationPolicy.toUiState(
            query = "수원시",
            guides = listOf(guide(managementZoneName = "A Zone")),
            lookupReason = RegionalGuideCandidateLookupReason.MULTIPLE_CANDIDATES,
            isFavoriteRestore = true,
            canRestoreCandidates = false,
        )

        assertEquals(RegionalGuideCandidateReason.FAVORITE_RESTORE_AMBIGUOUS, result.reason)
    }

    @Test
    fun `후보는 표시 영역 이름 기준으로 정렬한다`() {
        val result = RegionalGuideCandidatePresentationPolicy.toUiState(
            query = "수원시",
            guides = listOf(
                guide(managementZoneName = "B Zone"),
                guide(managementZoneName = "A Zone"),
            ),
            lookupReason = RegionalGuideCandidateLookupReason.MULTIPLE_CANDIDATES,
            isFavoriteRestore = false,
            canRestoreCandidates = false,
        )

        assertEquals(
            listOf("A Zone", "B Zone"),
            result.candidates.map { candidate -> candidate.guide.managementZoneName },
        )
    }

    private fun guide(managementZoneName: String): RegionalDisposalGuide =
        RegionalDisposalGuide(
            region = Region(sido = "경기도", sigungu = "수원시"),
            managementZoneName = managementZoneName,
            targetRegionName = "Target",
            schedules = emptyList(),
        )
}
