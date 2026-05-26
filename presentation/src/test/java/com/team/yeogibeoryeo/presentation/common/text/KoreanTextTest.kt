package com.team.yeogibeoryeo.presentation.common.text

import org.junit.Assert.assertEquals
import org.junit.Test

class KoreanTextTest {
    @Test
    fun `한글 음절 뒤에 줄바꿈 기회를 추가한다`() {
        assertEquals(
            "특\u200B수\u200B규\u200B격\u200B마\u200B대\u200B",
            "특수규격마대".withKoreanLineBreakOpportunities(),
        )
    }

    @Test
    fun `영문 숫자 공백은 그대로 둔다`() {
        assertEquals(
            "PET 500ml",
            "PET 500ml".withKoreanLineBreakOpportunities(),
        )
    }
}
