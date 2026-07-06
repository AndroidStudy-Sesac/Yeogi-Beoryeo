package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.presentation.R
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
}
