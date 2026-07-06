package com.team.yeogibeoryeo.presentation.regionalguide.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RegionalGuideCandidateUiModelTest {

    @Test
    fun `관리구역명과 대상지역명이 다르면 두 값을 함께 표시한다`() {
        val candidate = candidate(
            managementZoneName = "노은2동",
            targetRegionName = "반석동 일부지역"
        )

        assertEquals("노은2동 / 반석동 일부지역", candidate.displayText)
    }

    @Test
    fun `관리구역명과 대상지역명이 같으면 한 번만 표시한다`() {
        val candidate = candidate(
            managementZoneName = "문전수거 지역",
            targetRegionName = "문전수거 지역"
        )

        assertEquals("문전수거 지역", candidate.displayText)
    }

    @Test
    fun `관리구역명만 있으면 관리구역명을 표시한다`() {
        val candidate = candidate(
            managementZoneName = "온천1동",
            targetRegionName = null
        )

        assertEquals("온천1동", candidate.displayText)
    }

    @Test
    fun `대상지역명만 있으면 대상지역명을 표시한다`() {
        val candidate = candidate(
            managementZoneName = null,
            targetRegionName = "반석동 일부지역"
        )

        assertEquals("반석동 일부지역", candidate.displayText)
    }

    @Test
    fun `관리구역명과 대상지역명이 모두 비어 있으면 시도 시군구 fallback을 표시한다`() {
        val candidate = candidate(
            regionName = "대전광역시 유성구",
            managementZoneName = " ",
            targetRegionName = ""
        )

        assertEquals("대전광역시 > 유성구", candidate.displayText)
    }

    @Test
    fun `관리구역명과 대상지역명이 모두 없음이면 시도 시군구 fallback을 표시한다`() {
        val candidate = candidate(
            sido = "경기도",
            sigungu = "성남시",
            managementZoneName = " 없음 ",
            targetRegionName = "없음"
        )

        assertEquals("경기도 > 성남시", candidate.displayText)
    }

    @Test
    fun `관리구역명만 없음이면 의미 있는 대상지역명을 표시한다`() {
        val candidate = candidate(
            managementZoneName = "없음",
            targetRegionName = "대가야읍"
        )

        assertEquals("대가야읍", candidate.displayText)
    }

    @Test
    fun `대상지역명만 없음이면 의미 있는 관리구역명을 표시한다`() {
        val candidate = candidate(
            managementZoneName = "온천1동",
            targetRegionName = "없음"
        )

        assertEquals("온천1동", candidate.displayText)
    }

    @Test
    fun `관리구역명과 대상지역명이 모두 없음이면 배출장소 유형으로 fallback 후보를 구분한다`() {
        val candidate = candidate(
            sido = "경기도",
            sigungu = "성남시",
            managementZoneName = "없음",
            targetRegionName = "없음",
            disposalPlaceType = "문전수거"
        )

        assertEquals("경기도 > 성남시 / 문전수거", candidate.displayText)
    }

    @Test
    fun `배출장소 유형이 없으면 배출장소 설명으로 fallback 후보를 구분한다`() {
        val candidate = candidate(
            sido = "경상북도",
            sigungu = "성주군",
            managementZoneName = "없음",
            targetRegionName = "없음",
            disposalPlaceDescription = "거점수거함 배출"
        )

        assertEquals("경상북도 > 성주군 / 거점수거함 배출", candidate.displayText)
    }

    @Test
    fun `배출장소 정보가 없으면 첫 배출 일정 요약으로 fallback 후보를 구분한다`() {
        val candidate = candidate(
            sido = "대구광역시",
            sigungu = "군위군",
            managementZoneName = "없음",
            targetRegionName = "없음",
            schedules = listOf(
                RegionalWasteScheduleUiModel(
                    wasteTypeName = "일반쓰레기",
                    disposalDays = "월, 수, 금",
                    disposalTime = "정보 없음",
                    disposalMethod = "종량제봉투 배출"
                )
            )
        )

        assertEquals("대구광역시 > 군위군 / 일반쓰레기 월, 수, 금", candidate.displayText)
    }

    @Test
    fun `관리구역명과 대상지역명이 함께 있으면 대상지역명 기준으로 정렬한다`() {
        val candidates = listOf(
            candidate(managementZoneName = "2권역", targetRegionName = "문덕2리"),
            candidate(managementZoneName = "2권역", targetRegionName = "고산리"),
            candidate(managementZoneName = "2권역", targetRegionName = "월곡2리"),
            candidate(managementZoneName = "2권역", targetRegionName = "월곡1리"),
            candidate(managementZoneName = "2권역", targetRegionName = "소성리")
        )

        val sorted = candidates.sortedWith(regionalGuideCandidateDisplayComparator)

        assertEquals(
            listOf(
                "2권역 / 고산리",
                "2권역 / 문덕2리",
                "2권역 / 소성리",
                "2권역 / 월곡1리",
                "2권역 / 월곡2리"
            ),
            sorted.map { candidate -> candidate.displayText }
        )
    }

    @Test
    fun `숫자가 포함된 대상지역명은 자연 순서로 정렬한다`() {
        val candidates = listOf(
            candidate(managementZoneName = "2권역", targetRegionName = "월곡10리"),
            candidate(managementZoneName = "2권역", targetRegionName = "월곡2리"),
            candidate(managementZoneName = "2권역", targetRegionName = "월곡1리")
        )

        val sorted = candidates.sortedWith(regionalGuideCandidateDisplayComparator)

        assertEquals(
            listOf("2권역 / 월곡1리", "2권역 / 월곡2리", "2권역 / 월곡10리"),
            sorted.map { candidate -> candidate.displayText }
        )
    }

    @Test
    fun `숫자 권역 후보는 권역 번호 기준으로 자연 순서 정렬한다`() {
        val candidates = listOf(
            candidate(managementZoneName = "3권역", targetRegionName = "와부읍, 조안면"),
            candidate(managementZoneName = "7권역", targetRegionName = "진건읍, 다산1동"),
            candidate(managementZoneName = "4권역", targetRegionName = "진접읍, 오남읍"),
            candidate(managementZoneName = "1권역", targetRegionName = "호평동, 평내동, 금곡동")
        )

        val sorted = candidates.sortedWith(regionalGuideCandidateDisplayComparator)

        assertEquals(
            listOf(
                "1권역 / 호평동, 평내동, 금곡동",
                "3권역 / 와부읍, 조안면",
                "4권역 / 진접읍, 오남읍",
                "7권역 / 진건읍, 다산1동"
            ),
            sorted.map { candidate -> candidate.displayText }
        )
    }

    @Test
    fun `숫자로 시작하는 관리구역 후보는 뒤 문구와 관계없이 자연 순서 정렬한다`() {
        val candidates = listOf(
            candidate(
                managementZoneName = "7구역(고양위생공사 031-962-0000)",
                targetRegionName = "능곡동+장항1동+장항2동+마두1동"
            ),
            candidate(
                managementZoneName = "12구역(서강기업 031-972-0000)",
                targetRegionName = "대화동+탄현1동+탄현2동"
            ),
            candidate(
                managementZoneName = "11구역(수창기업 031-978-0000)",
                targetRegionName = "덕이동+가좌동+송포동+주엽2동"
            )
        )

        val sorted = candidates.sortedWith(regionalGuideCandidateDisplayComparator)

        assertEquals(
            listOf(
                "7구역(고양위생공사 031-962-0000) / 능곡동+장항1동+장항2동+마두1동",
                "11구역(수창기업 031-978-0000) / 덕이동+가좌동+송포동+주엽2동",
                "12구역(서강기업 031-972-0000) / 대화동+탄현1동+탄현2동"
            ),
            sorted.map { candidate -> candidate.displayText }
        )
    }

    @Test
    fun `로마 숫자로 시작하는 관리구역 후보도 관리구역 기준으로 자연 순서 정렬한다`() {
        val candidates = listOf(
            candidate(managementZoneName = "II구역", targetRegionName = "갑천면"),
            candidate(managementZoneName = "I구역", targetRegionName = "강림면"),
            candidate(managementZoneName = "II구역", targetRegionName = "공근면"),
            candidate(managementZoneName = "I구역", targetRegionName = "둔내면"),
            candidate(managementZoneName = "II구역", targetRegionName = "서원면")
        )

        val sorted = candidates.sortedWith(regionalGuideCandidateDisplayComparator)

        assertEquals(
            listOf(
                "I구역 / 강림면",
                "I구역 / 둔내면",
                "II구역 / 갑천면",
                "II구역 / 공근면",
                "II구역 / 서원면"
            ),
            sorted.map { candidate -> candidate.displayText }
        )
    }

    @Test
    fun `유니코드 로마 숫자로 시작하는 관리구역 후보도 관리구역 기준으로 자연 순서 정렬한다`() {
        val candidates = listOf(
            candidate(managementZoneName = "Ⅱ구역", targetRegionName = "갑천면"),
            candidate(managementZoneName = "Ⅰ구역", targetRegionName = "강림면"),
            candidate(managementZoneName = "Ⅱ구역", targetRegionName = "공근면"),
            candidate(managementZoneName = "Ⅰ구역", targetRegionName = "둔내면"),
            candidate(managementZoneName = "Ⅱ구역", targetRegionName = "서원면")
        )

        val sorted = candidates.sortedWith(regionalGuideCandidateDisplayComparator)

        assertEquals(
            listOf(
                "Ⅰ구역 / 강림면",
                "Ⅰ구역 / 둔내면",
                "Ⅱ구역 / 갑천면",
                "Ⅱ구역 / 공근면",
                "Ⅱ구역 / 서원면"
            ),
            sorted.map { candidate -> candidate.displayText }
        )
    }

    @Test
    fun `동일한 표시명 후보가 여러 개이고 배출장소가 다르면 배출장소로 구분한다`() {
        val candidates = listOf(
            candidate(
                sido = "대전광역시",
                sigungu = "서구",
                managementZoneName = "대전광역시",
                targetRegionName = "서구",
                disposalPlaceType = "문전수거"
            ),
            candidate(
                sido = "대전광역시",
                sigungu = "서구",
                managementZoneName = "대전광역시",
                targetRegionName = "서구",
                disposalPlaceType = "기타"
            )
        ).withDuplicateDisplayDisambiguation()

        assertEquals(
            listOf("대전광역시 / 서구 / 문전수거", "대전광역시 / 서구 / 기타"),
            candidates.map { candidate -> candidate.displayText }
        )
    }

    @Test
    fun `동일한 표시명 후보의 배출장소가 같으면 보조 문구를 추가하지 않는다`() {
        val candidates = listOf(
            candidate(
                sido = "대전광역시",
                sigungu = "서구",
                managementZoneName = "대전광역시",
                targetRegionName = "서구",
                disposalPlaceType = "문전수거"
            ),
            candidate(
                sido = "대전광역시",
                sigungu = "서구",
                managementZoneName = "대전광역시",
                targetRegionName = "서구",
                disposalPlaceType = "문전수거"
            )
        ).withDuplicateDisplayDisambiguation()

        assertEquals(
            listOf("대전광역시 / 서구", "대전광역시 / 서구"),
            candidates.map { candidate -> candidate.displayText }
        )
    }

    @Test
    fun `동일한 표시명 후보의 배출장소가 없으면 기존 표시를 유지한다`() {
        val candidates = listOf(
            candidate(
                sido = "대전광역시",
                sigungu = "서구",
                managementZoneName = "대전광역시",
                targetRegionName = "서구"
            ),
            candidate(
                sido = "대전광역시",
                sigungu = "서구",
                managementZoneName = "대전광역시",
                targetRegionName = "서구"
            )
        ).withDuplicateDisplayDisambiguation()

        assertEquals(
            listOf("대전광역시 / 서구", "대전광역시 / 서구"),
            candidates.map { candidate -> candidate.displayText }
        )
    }

    private fun candidate(
        regionName: String = "대전광역시 유성구",
        sido: String = "대전광역시",
        sigungu: String = "유성구",
        managementZoneName: String?,
        targetRegionName: String?,
        disposalPlaceType: String? = null,
        disposalPlaceDescription: String? = null,
        schedules: List<RegionalWasteScheduleUiModel> = emptyList()
    ): RegionalGuideCandidateUiModel =
        RegionalGuideCandidateUiModel(
            guide = RegionalGuideUiModel(
                regionName = regionName,
                managementZoneName = managementZoneName,
                targetRegionName = targetRegionName,
                disposalPlaceType = disposalPlaceType,
                disposalPlaceDescription = disposalPlaceDescription,
                schedules = schedules,
                uncollectedDays = null,
                departmentInfo = null
            ),
            sido = sido,
            sigungu = sigungu,
            eupmyeondong = null
        )
}
