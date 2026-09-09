package com.team.yeogibeoryeo.navigation

import org.junit.Assert.assertNull
import org.junit.Test

class CollectionSpotDiscoveryNavigationPolicyTest {
    @Test
    fun `즐겨찾기 빈 상태에서는 선택이나 필터가 없는 지도 루트를 만든다`() {
        val route = collectionSpotDiscoveryRoute()

        assertNull(route.initialSpotType)
        assertNull(route.favoriteSpotRequestId)
        assertNull(route.favoriteSpotTargetId)
        assertNull(route.favoriteSpotName)
        assertNull(route.favoriteSpotType)
        assertNull(route.favoriteSpotAddress)
        assertNull(route.favoriteSpotDetailLocation)
        assertNull(route.favoriteSpotLatitude)
        assertNull(route.favoriteSpotLongitude)
        assertNull(route.toInitialCollectionSpotTypeOrNull())
        assertNull(route.toFavoriteSpotMapMoveRequest())
    }
}
