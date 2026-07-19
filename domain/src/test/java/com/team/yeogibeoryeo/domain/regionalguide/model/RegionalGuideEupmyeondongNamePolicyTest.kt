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

    @Test
    fun `제공 범위 표현이 붙은 대상지역명은 읍면동과 일치한다`() {
        assertTrue(
            RegionalGuideEupmyeondongNamePolicy.containsSameNameOrGuideAreaName(
                regionName = "반석동 일부지역",
                eupmyeondong = "반석동",
            )
        )
        assertFalse(
            RegionalGuideEupmyeondongNamePolicy.containsSameName(
                regionName = "반석동 일부지역",
                eupmyeondong = "반석동",
            )
        )
    }

    @Test
    fun `번호 범위 대상지역명은 제 번호 행정동과 일치한다`() {
        assertTrue(
            RegionalGuideEupmyeondongNamePolicy.containsSameNameOrGuideAreaName(
                regionName = "괴정 1~3동",
                eupmyeondong = "괴정제1동",
            )
        )
        assertTrue(
            RegionalGuideEupmyeondongNamePolicy.containsSameNameOrGuideAreaName(
                regionName = "괴정 1~3동",
                eupmyeondong = "괴정제3동",
            )
        )
        assertFalse(
            RegionalGuideEupmyeondongNamePolicy.containsSameNameOrGuideAreaName(
                regionName = "괴정 1~3동",
                eupmyeondong = "괴정제4동",
            )
        )
        assertTrue(
            RegionalGuideEupmyeondongNamePolicy.containsSameNameOrGuideAreaName(
                regionName = "괴정4동",
                eupmyeondong = "괴정제4동",
            )
        )
    }

    @Test
    fun `접미사가 생략된 대상지역명은 읍면동과 일치한다`() {
        assertTrue(
            RegionalGuideEupmyeondongNamePolicy.containsSameNameOrGuideAreaName(
                regionName = "범서, 온양, 웅촌",
                eupmyeondong = "온양읍",
            )
        )
        assertTrue(
            RegionalGuideEupmyeondongNamePolicy.containsSameNameOrGuideAreaName(
                regionName = "사천",
                eupmyeondong = "사천면",
            )
        )
    }

    @Test
    fun `법정동 이름으로 시작하는 상세 대상지역명은 해당 법정동과 일치한다`() {
        assertTrue(
            RegionalGuideEupmyeondongNamePolicy.targetRegionStartsWithLegalDongName(
                targetRegionName = "부곡중앙북6길",
                eupmyeondong = "부곡동",
            )
        )
        assertFalse(
            RegionalGuideEupmyeondongNamePolicy.targetRegionStartsWithLegalDongName(
                targetRegionName = "가구단지길",
                eupmyeondong = "부곡동",
            )
        )
        assertFalse(
            RegionalGuideEupmyeondongNamePolicy.targetRegionStartsWithLegalDongName(
                targetRegionName = "사천로",
                eupmyeondong = "사천면",
            )
        )
        assertFalse(
            RegionalGuideEupmyeondongNamePolicy.targetRegionStartsWithLegalDongName(
                targetRegionName = "부곡1,4동",
                eupmyeondong = "부곡제2동",
            )
        )
    }

    @Test
    fun `법정동 접두어 뒤가 다른 행정구역명이면 일치하지 않는다`() {
        listOf(
            "장기본동" to "장기동",
            "장기본동 일부지역" to "장기동",
            "고덕면" to "고덕동",
            "개정면" to "개정동",
        ).forEach { (targetRegionName, eupmyeondong) ->
            assertFalse(
                RegionalGuideEupmyeondongNamePolicy.targetRegionStartsWithLegalDongName(
                    targetRegionName = targetRegionName,
                    eupmyeondong = eupmyeondong,
                )
            )
        }
    }

    @Test
    fun `읍면동을 식별할 수 없는 제공 지역명은 읍면동 제공 범위로 판단하지 않는다`() {
        assertTrue(
            RegionalGuideEupmyeondongNamePolicy.hasEupmyeondongCoverage(
                regionName = "반석동 일부지역",
            )
        )
        assertTrue(
            RegionalGuideEupmyeondongNamePolicy.hasEupmyeondongCoverage(
                regionName = "반석동 일부",
            )
        )
        assertFalse(
            RegionalGuideEupmyeondongNamePolicy.hasEupmyeondongCoverage(
                regionName = "문전수거 지역",
            )
        )
    }
}
