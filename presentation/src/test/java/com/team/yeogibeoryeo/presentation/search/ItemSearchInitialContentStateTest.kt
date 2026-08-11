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
}
