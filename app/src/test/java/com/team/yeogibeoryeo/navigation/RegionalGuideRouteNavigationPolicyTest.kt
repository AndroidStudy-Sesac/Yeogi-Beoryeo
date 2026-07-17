package com.team.yeogibeoryeo.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionalGuideRouteNavigationPolicyTest {
    @Test
    fun `즐겨찾기 대상이 없는 지역 가이드 경로는 안내 탭 경로이다`() {
        val route = RegionalGuideRoute()

        assertFalse(route.isFavoriteReentryRoute())
        assertFalse(route.isMapReentryRoute())
    }

    @Test
    fun `즐겨찾기 대상이 있고 진입 출처가 없는 지역 가이드 경로는 안내 탭 경로이다`() {
        val route = RegionalGuideRoute(initialFavoriteTargetId = "regional-guide-v1|4:Sido")

        assertFalse(route.isFavoriteReentryRoute())
        assertFalse(route.isMapReentryRoute())
    }

    @Test
    fun `비어 있는 즐겨찾기 대상은 저장 탭 재진입 경로가 아니다`() {
        assertFalse(RegionalGuideRoute(initialFavoriteTargetId = " ").isFavoriteReentryRoute())
    }

    @Test
    fun `홈 요약에서 이동한 지역 가이드 경로는 안내 탭 경로로 유지된다`() {
        val route = RegionalGuideRoute(initialFavoriteTargetId = "regional-guide-v2|4:Sido")

        assertFalse(route.isFavoriteReentryRoute())
        assertFalse(route.isMapReentryRoute())
    }

    @Test
    fun `품목 안내 상세에서 이동한 지역 가이드 경로는 안내 탭 경로로 유지된다`() {
        val route = RegionalGuideRoute()

        assertFalse(route.isFavoriteReentryRoute())
        assertFalse(route.isMapReentryRoute())
    }

    @Test
    fun `저장 탭에서 이동한 지역 가이드 경로는 저장 탭 재진입 경로이다`() {
        val route = RegionalGuideRoute(
            initialFavoriteTargetId = "regional-guide-v2|4:Sido",
            entrySource = RegionalGuideEntrySource.FAVORITES,
        )

        assertTrue(route.isFavoriteReentryRoute())
        assertFalse(route.isMapReentryRoute())
    }

    @Test
    fun `장소 주소는 지역 가이드 주소 경로를 만든다`() {
        val address = "  서울특별시 중구 퇴계로 63 (남창동, 삼익패션타운)  "

        val route = address.toRegionalGuideAddressRouteOrNull()

        assertEquals("서울특별시 중구 퇴계로 63 (남창동, 삼익패션타운)", route?.initialAddress)
        assertFalse(route!!.isFavoriteReentryRoute())
        assertTrue(route.isMapReentryRoute())
    }

    @Test
    fun `빈 장소 주소는 지역 가이드 주소 경로를 만들지 않는다`() {
        assertNull(" ".toRegionalGuideAddressRouteOrNull())
    }

    @Test
    fun `주소와 즐겨찾기 대상이 함께 있으면 저장 탭 출처일 때만 저장 탭 재진입 경로이다`() {
        val route = RegionalGuideRoute(
            initialAddress = "서울특별시 중구 퇴계로 63 (남창동, 삼익패션타운)",
            initialFavoriteTargetId = "regional-guide-v1|4:Sido",
        )

        assertFalse(route.isFavoriteReentryRoute())
        assertFalse(route.isMapReentryRoute())
    }
}
