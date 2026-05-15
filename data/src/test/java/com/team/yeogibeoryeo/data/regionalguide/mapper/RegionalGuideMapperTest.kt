package com.team.yeogibeoryeo.data.regionalguide.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegionalGuideMapperTest {

    @Test
    fun `시간파싱_유효하지 않은 입력은 null 반환한다`() {
        assertNull(RegionalWasteScheduleMapper.parseTime("00:00"))
        assertNull(RegionalWasteScheduleMapper.parseTime("   "))
        assertNull(RegionalWasteScheduleMapper.parseTime(null))

        assertEquals("18:00", RegionalWasteScheduleMapper.parseTime(" 18:00 "))
    }

    @Test
    fun `요일 파싱_불필요한 요일 단어와 기호를 제거하여 정제한다`() {
        assertEquals("월, 화, 수", RegionalWasteScheduleMapper.parseDays("월요일/화요일+수요일,월요일"))
        assertEquals("명절, 공휴일", RegionalWasteScheduleMapper.parseDays("명절+공휴일"))
        assertEquals("미지정", RegionalWasteScheduleMapper.parseDays(null))
    }

    @Test
    fun `배출 방법 파싱_과도한 공백과 줄바꿈을 하나의 공백으로 치환한다`() {
        val rawMethod = "종량제 봉투에 담아 \n   지정된 장소에 배출"
        assertEquals("종량제 봉투에 담아 지정된 장소에 배출", RegionalWasteScheduleMapper.parseMethod(rawMethod))
    }
}