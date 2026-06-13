package com.team.yeogibeoryeo.presentation.common.text

import org.junit.Assert.assertEquals
import org.junit.Test

class KoreanTextTest {
    @Test
    fun `한글 음절 뒤에 줄바꿈 기회를 추가한다`() {
        val result = "공사장 생활폐기물".withKoreanSyllableBreakOpportunities()

        assertEquals(
            "공\u200B사\u200B장\u200B 생\u200B활\u200B폐\u200B기\u200B물\u200B",
            result,
        )
    }

    @Test
    fun `한글이 아닌 문자는 그대로 둔다`() {
        val result = "PET 500ml".withKoreanSyllableBreakOpportunities()

        assertEquals("PET 500ml", result)
    }
}
