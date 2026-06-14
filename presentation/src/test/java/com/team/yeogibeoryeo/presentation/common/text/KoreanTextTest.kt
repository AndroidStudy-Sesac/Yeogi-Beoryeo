package com.team.yeogibeoryeo.presentation.common.text

import org.junit.Assert.assertEquals
import org.junit.Test

class KoreanTextTest {
    @Test
    fun `한글 음절 뒤에 줄바꿈 기회를 추가한다`() {
        val result = "공사장 생활폐기물".withKoreanLineBreakOpportunities()

        assertEquals(
            "공\u200B사\u200B장\u200B 생\u200B활\u200B폐\u200B기\u200B물\u200B",
            result,
        )
    }

    @Test
    fun `한글이 아닌 문자는 그대로 둔다`() {
        val result = "PET 500ml".withKoreanLineBreakOpportunities()

        assertEquals("PET 500ml", result)
    }

    @Test
    fun `발포합성수지 소재 약어는 TalkBack 읽기용 한글 발음으로 변환한다`() {
        val result =
            "발포합성수지는 PP, PE, PS 등의 합성수지이며 EPP, EPS, EPE 등은 EPR 대상품목입니다."
                .toTalkBackReadableText()

        assertEquals(
            "발포합성수지는 피피, 피이, 피에스 등의 합성수지이며 이피피, 이피에스, 이피이 등은 이피알 대상품목입니다.",
            result,
        )
    }
}
