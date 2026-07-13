package com.team.yeogibeoryeo.domain.regionalguide.model

import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteKey
import com.team.yeogibeoryeo.domain.region.model.Region
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionalGuideFavoriteCompatibilityPolicyTest {

    @Test
    fun `전남광주통합특별시 전남 시군 favorite key는 현재 전라남도 후보와 compatible 하다`() {
        val favoriteKey = favoriteKey(
            sido = "전남광주통합특별시",
            sigungu = "나주시",
            eupmyeondong = "노안면",
            targetRegionName = "노안면",
            managementZoneName = "노안면",
        )
        val candidate = regionalDisposalGuide(
            sido = "전라남도",
            sigungu = "나주시",
            eupmyeondong = "노안면",
            targetRegionName = "노안면",
            managementZoneName = "노안면",
        )

        assertTrue(
            RegionalGuideFavoriteCompatibilityPolicy.isSameFavoriteTarget(
                favoriteKey = favoriteKey,
                candidate = candidate,
            )
        )
    }

    @Test
    fun `전남광주통합특별시 광주 5개 구 favorite key는 현재 광주광역시 후보와 compatible 하다`() {
        val favoriteKey = favoriteKey(
            sido = "전남광주통합특별시",
            sigungu = "서구",
            eupmyeondong = "금호동",
            targetRegionName = "금호동",
            managementZoneName = "금호동",
        )
        val candidate = regionalDisposalGuide(
            sido = "광주광역시",
            sigungu = "서구",
            eupmyeondong = "금호동",
            targetRegionName = "금호동",
            managementZoneName = "금호동",
        )

        assertTrue(
            RegionalGuideFavoriteCompatibilityPolicy.isSameFavoriteTarget(
                favoriteKey = favoriteKey,
                candidate = candidate,
            )
        )
    }

    @Test
    fun `과거 강원도 favorite key는 현재 강원특별자치도 후보와 compatible 하다`() {
        val favoriteKey = favoriteKey(
            sido = "강원도",
            sigungu = "춘천시",
            targetRegionName = "춘천시 전체",
            managementZoneName = "춘천시 전체",
        )
        val candidate = regionalDisposalGuide(
            sido = "강원특별자치도",
            sigungu = "춘천시",
            targetRegionName = "춘천시 전체",
            managementZoneName = "춘천시 전체",
        )

        assertTrue(
            RegionalGuideFavoriteCompatibilityPolicy.isSameFavoriteTarget(
                favoriteKey = favoriteKey,
                candidate = candidate,
            )
        )
    }

    @Test
    fun `과거 전라북도 favorite key는 현재 전북특별자치도 후보와 compatible 하다`() {
        val favoriteKey = favoriteKey(
            sido = "전라북도",
            sigungu = "전주시",
            targetRegionName = "전주시 전체",
            managementZoneName = "전주시 전체",
        )
        val candidate = regionalDisposalGuide(
            sido = "전북특별자치도",
            sigungu = "전주시",
            targetRegionName = "전주시 전체",
            managementZoneName = "전주시 전체",
        )

        assertTrue(
            RegionalGuideFavoriteCompatibilityPolicy.isSameFavoriteTarget(
                favoriteKey = favoriteKey,
                candidate = candidate,
            )
        )
    }

    @Test
    fun `targetRegionName이 다르면 같은 지역이어도 incompatible 하다`() {
        val favoriteKey = favoriteKey(
            sido = "광주광역시",
            sigungu = "서구",
            eupmyeondong = "금호동",
            targetRegionName = "금호동",
            managementZoneName = "금호동",
        )
        val candidate = regionalDisposalGuide(
            sido = "광주광역시",
            sigungu = "서구",
            eupmyeondong = "금호동",
            targetRegionName = "풍암동",
            managementZoneName = "금호동",
        )

        assertFalse(
            RegionalGuideFavoriteCompatibilityPolicy.isSameFavoriteTarget(
                favoriteKey = favoriteKey,
                candidate = candidate,
            )
        )
    }

    @Test
    fun `sigungu가 다르면 같은 시도 alias 기준이어도 incompatible 하다`() {
        val favoriteKey = favoriteKey(
            sido = "전남광주통합특별시",
            sigungu = "나주시",
            eupmyeondong = "노안면",
            targetRegionName = "노안면",
            managementZoneName = "노안면",
        )
        val candidate = regionalDisposalGuide(
            sido = "전라남도",
            sigungu = "광양시",
            eupmyeondong = "노안면",
            targetRegionName = "노안면",
            managementZoneName = "노안면",
        )

        assertFalse(
            RegionalGuideFavoriteCompatibilityPolicy.isSameFavoriteTarget(
                favoriteKey = favoriteKey,
                candidate = candidate,
            )
        )
    }

    @Test
    fun `eupmyeondong이 다르면 같은 시군구와 대상지역이어도 incompatible 하다`() {
        val favoriteKey = favoriteKey(
            sido = "전라남도",
            sigungu = "나주시",
            eupmyeondong = "노안면",
            targetRegionName = "노안면",
            managementZoneName = "노안면",
        )
        val candidate = regionalDisposalGuide(
            sido = "전라남도",
            sigungu = "나주시",
            eupmyeondong = "금천면",
            targetRegionName = "노안면",
            managementZoneName = "노안면",
        )

        assertFalse(
            RegionalGuideFavoriteCompatibilityPolicy.isSameFavoriteTarget(
                favoriteKey = favoriteKey,
                candidate = candidate,
            )
        )
    }

    @Test
    fun `v2 key에서 managementZoneName이 다르면 incompatible 하다`() {
        val favoriteKey = favoriteKey(
            sido = "대전광역시",
            sigungu = "유성구",
            eupmyeondong = "반석동",
            targetRegionName = "반석동 일부지역",
            managementZoneName = "노은2동",
        )
        val candidate = regionalDisposalGuide(
            sido = "대전광역시",
            sigungu = "유성구",
            eupmyeondong = "반석동",
            targetRegionName = "반석동 일부지역",
            managementZoneName = "노은3동",
        )

        assertFalse(
            RegionalGuideFavoriteCompatibilityPolicy.isSameFavoriteTarget(
                favoriteKey = favoriteKey,
                candidate = candidate,
            )
        )
    }

    @Test
    fun `legacy key처럼 managementZoneName이 없으면 관리구역 후보와 compatible 하다`() {
        val legacyKey = RegionalGuideFavoriteKey.decodeOrNull(
            "regional-guide-v1|5:대전광역시3:유성구3:반석동8:반석동 일부지역"
        ) ?: error("legacy key should be decoded")
        val candidate = regionalDisposalGuide(
            sido = "대전광역시",
            sigungu = "유성구",
            eupmyeondong = "반석동",
            targetRegionName = "반석동 일부지역",
            managementZoneName = "노은2동",
        )

        assertTrue(
            RegionalGuideFavoriteCompatibilityPolicy.isSameFavoriteTarget(
                favoriteKey = legacyKey,
                candidate = candidate,
            )
        )
    }

    @Test
    fun `compatibility 비교는 favorite key 원본 encode 값을 바꾸지 않는다`() {
        val favoriteKey = favoriteKey(
            sido = "전남광주통합특별시",
            sigungu = "나주시",
            eupmyeondong = "노안면",
            targetRegionName = "노안면",
            managementZoneName = "노안면",
        )
        val originalEncodedKey = favoriteKey.encode()
        val candidate = regionalDisposalGuide(
            sido = "전라남도",
            sigungu = "나주시",
            eupmyeondong = "노안면",
            targetRegionName = "노안면",
            managementZoneName = "노안면",
        )

        RegionalGuideFavoriteCompatibilityPolicy.isSameFavoriteTarget(
            favoriteKey = favoriteKey,
            candidate = candidate,
        )

        assertEquals(originalEncodedKey, favoriteKey.encode())
    }

    private fun favoriteKey(
        sido: String?,
        sigungu: String?,
        eupmyeondong: String? = null,
        targetRegionName: String?,
        managementZoneName: String?,
    ): RegionalGuideFavoriteKey =
        RegionalGuideFavoriteKey(
            sido = sido,
            sigungu = sigungu,
            eupmyeondong = eupmyeondong,
            targetRegionName = targetRegionName,
            managementZoneName = managementZoneName,
        )

    private fun regionalDisposalGuide(
        sido: String?,
        sigungu: String?,
        eupmyeondong: String? = null,
        targetRegionName: String?,
        managementZoneName: String?,
    ): RegionalDisposalGuide =
        RegionalDisposalGuide(
            region = Region(
                sido = sido,
                sigungu = sigungu,
                eupmyeondong = eupmyeondong,
            ),
            targetRegionName = targetRegionName,
            managementZoneName = managementZoneName,
            schedules = emptyList(),
        )
}
