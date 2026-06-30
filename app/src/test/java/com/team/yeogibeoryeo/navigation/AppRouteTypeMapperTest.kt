package com.team.yeogibeoryeo.navigation

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.presentation.search.model.ItemUsefulGuideType
import org.junit.Assert.assertEquals
import org.junit.Test

class AppRouteTypeMapperTest {
    @Test
    fun `수거 장소 타입은 route 타입으로 변환한 뒤 다시 복원된다`() {
        CollectionSpotType.entries.forEach { type ->
            assertEquals(type, type.toRouteType().toCollectionSpotType())
        }
    }

    @Test
    fun `유용한 안내 타입은 route 타입으로 변환한 뒤 다시 복원된다`() {
        ItemUsefulGuideType.entries.forEach { type ->
            assertEquals(type, type.toRouteType().toItemUsefulGuideType())
        }
    }
}
