package com.team.yeogibeoryeo.domain.regionalguide.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionalGuideEupmyeondongNamePolicyTest {

    @Test
    fun `문자 점 묶음 행정동과 붙여쓴 행정동은 같은 이름으로 판단한다`() {
        assertTrue(
            RegionalGuideEupmyeondongNamePolicy.isSameName(
                first = "불로.봉무동",
                second = "불로봉무동",
            )
        )
        assertTrue(
            RegionalGuideEupmyeondongNamePolicy.isSameName(
                first = "성내.충인동",
                second = "성내충인동",
            )
        )
    }

    @Test
    fun `문자 점 묶음 행정동은 지역 가이드용 붙여쓴 표시명으로 변환한다`() {
        assertTrue(
            RegionalGuideEupmyeondongNamePolicy.matchesKeyword(
                eupmyeondongName = "성내.충인동",
                keyword = "성내충인동",
            )
        )
        assertEquals(
            "성내충인동",
            RegionalGuideEupmyeondongNamePolicy.toApiCompatibleDisplayName("성내.충인동")
        )
    }

    @Test
    fun `숫자 점 묶음 행정동은 분리된 행정동 표기와 같은 이름으로 판단한다`() {
        assertTrue(
            RegionalGuideEupmyeondongNamePolicy.containsSameName(
                regionName = "금호2가동+금호3가동",
                eupmyeondong = "금호2.3가동",
            )
        )
    }

    @Test
    fun `서로 다른 행정동은 같은 이름으로 판단하지 않는다`() {
        assertFalse(
            RegionalGuideEupmyeondongNamePolicy.isSameName(
                first = "불로.봉무동",
                second = "불로대곡동",
            )
        )
    }
}
