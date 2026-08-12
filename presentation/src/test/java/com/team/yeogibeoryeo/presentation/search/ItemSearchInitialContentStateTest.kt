package com.team.yeogibeoryeo.presentation.search

import org.junit.Assert.assertEquals
import org.junit.Test

class ItemSearchInitialContentStateTest {

    @Test
    fun `운영 공지가 없으면 앱 가이드 기본 item index를 사용한다`() {
        assertEquals(1, ItemSearchGuideTarget.USEFUL_GUIDE.toLazyListItemIndex(hasOperationNotice = false))
        assertEquals(3, ItemSearchGuideTarget.SEARCH.toLazyListItemIndex(hasOperationNotice = false))
        assertEquals(4, ItemSearchGuideTarget.QUICK_CATEGORY.toLazyListItemIndex(hasOperationNotice = false))
    }

    @Test
    fun `운영 공지가 있으면 앱 가이드 item index를 한 칸 보정한다`() {
        assertEquals(2, ItemSearchGuideTarget.USEFUL_GUIDE.toLazyListItemIndex(hasOperationNotice = true))
        assertEquals(4, ItemSearchGuideTarget.SEARCH.toLazyListItemIndex(hasOperationNotice = true))
        assertEquals(5, ItemSearchGuideTarget.QUICK_CATEGORY.toLazyListItemIndex(hasOperationNotice = true))
    }

    @Test
    fun `앱 가이드 중 운영 공지가 나타나도 헤더 복원 item index는 유지한다`() {
        assertEquals(
            0,
            restoredAppGuideScrollIndex(
                storedIndex = 0,
                hadOperationNotice = false,
                hasOperationNotice = true,
            ),
        )
    }

    @Test
    fun `앱 가이드 중 운영 공지가 나타나면 공지 뒤 항목 복원 item index를 한 칸 뒤로 보정한다`() {
        assertEquals(
            4,
            restoredAppGuideScrollIndex(
                storedIndex = 3,
                hadOperationNotice = false,
                hasOperationNotice = true,
            ),
        )
    }

    @Test
    fun `앱 가이드 중 운영 공지가 사라져도 공지 item 복원 index는 유지한다`() {
        assertEquals(
            1,
            restoredAppGuideScrollIndex(
                storedIndex = 1,
                hadOperationNotice = true,
                hasOperationNotice = false,
            ),
        )
    }

    @Test
    fun `앱 가이드 중 운영 공지가 사라지면 공지 뒤 항목 복원 item index를 한 칸 앞으로 보정한다`() {
        assertEquals(
            2,
            restoredAppGuideScrollIndex(
                storedIndex = 3,
                hadOperationNotice = true,
                hasOperationNotice = false,
            ),
        )
    }

    @Test
    fun `복원 item index 보정 결과는 음수가 되지 않는다`() {
        assertEquals(
            0,
            restoredAppGuideScrollIndex(
                storedIndex = 0,
                hadOperationNotice = true,
                hasOperationNotice = false,
            ),
        )
    }
}
