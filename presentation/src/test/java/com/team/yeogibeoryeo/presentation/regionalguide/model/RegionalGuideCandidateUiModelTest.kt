package com.team.yeogibeoryeo.presentation.regionalguide.model

import com.team.yeogibeoryeo.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RegionalGuideCandidateUiModelTest {

    @Test
    fun `관리구역명과 대상지역명이 있으면 후보 행 문구를 리소스로 조합한다`() {
        val candidate = candidate(
            managementZoneName = "1구역",
            targetRegionName = "반석동, 솔마을지역",
        )

        assertEquals(
            RegionalGuideCandidateDisplayText.Resource(
                resId = R.string.regional_guide_candidate_label_format,
                args = listOf("1구역", "반석동, 솔마을지역"),
            ),
            candidate.displayTextForRow,
        )
    }

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
    fun `관리구역명과 대상지역명이 모두 비어 있으면 시도 시군구 대체 문구를 표시한다`() {
        val candidate = candidate(
            regionName = "대전광역시 유성구",
            managementZoneName = " ",
            targetRegionName = ""
        )

        assertEquals("대전광역시 > 유성구", candidate.displayText)
    }

    @Test
    fun `관리구역명과 대상지역명이 모두 없음이면 시도 시군구 대체 문구를 표시한다`() {
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
    fun `관리구역명과 대상지역명이 모두 없음이면 배출장소 유형으로 대체 후보를 구분한다`() {
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
    fun `관리구역명과 대상지역명이 모두 없음이고 배출장소 유형이 있으면 전체 기준 수거 유형 후보로 본다`() {
        val candidate = candidate(
            sido = "강원특별자치도",
            sigungu = "양구군",
            managementZoneName = "없음",
            targetRegionName = "없음",
            disposalPlaceType = "거점수거"
        )

        assertEquals(true, candidate.isOverallCollectionTypeCandidate)
        assertEquals("거점수거", candidate.collectionTypeOptionText)
    }

    @Test
    fun `수거 유형명 후보는 수거 유형 선택 후보로 본다`() {
        val candidate = candidate(
            managementZoneName = "거점수거 지역",
            targetRegionName = "거점수거 지역",
            disposalPlaceType = "거점수거"
        )

        assertEquals(true, candidate.isCollectionTypeSelectionCandidate)
    }

    @Test
    fun `일반 권역 후보는 배출장소 유형이 있어도 수거 유형 선택 후보로 보지 않는다`() {
        val candidate = candidate(
            managementZoneName = "1권역",
            targetRegionName = "갑천면",
            disposalPlaceType = "거점수거"
        )

        assertEquals(false, candidate.isCollectionTypeSelectionCandidate)
    }

    @Test
    fun `동일 수거 유형 후보 구분 정보는 배출장소 설명을 우선 사용한다`() {
        val candidate = candidate(
            managementZoneName = "없음",
            targetRegionName = "없음",
            disposalPlaceType = "거점수거",
            disposalPlaceDescription = "지정 거점 장소 배출",
            uncollectedDays = "일요일",
        )

        assertEquals(
            RegionalGuideCandidateDistinguishingText(
                label = RegionalGuideCandidateDistinguishingLabel.DISPOSAL_PLACE,
                value = RegionalGuideCandidateDisplayText.Plain("지정 거점 장소 배출")
            ),
            candidate.collectionTypeDistinguishingText
        )
    }

    @Test
    fun `배출장소 설명이 없으면 미수거일로 동일 수거 유형 후보를 구분한다`() {
        val candidate = candidate(
            managementZoneName = "없음",
            targetRegionName = "없음",
            disposalPlaceType = "거점수거",
            uncollectedDays = "일요일"
        )

        assertEquals(
            RegionalGuideCandidateDistinguishingText(
                label = RegionalGuideCandidateDistinguishingLabel.UNCOLLECTED_DAYS,
                value = RegionalGuideCandidateDisplayText.Plain("일요일")
            ),
            candidate.collectionTypeDistinguishingText
        )
    }

    @Test
    fun `배출장소 유형이 없으면 배출장소 설명으로 대체 후보를 구분한다`() {
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
    fun `배출장소 정보가 없으면 첫 배출 일정 요약으로 대체 후보를 구분한다`() {
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
    fun `시간 형식 일정은 후보 구분 정보에 전체 시간 형식을 사용한다`() {
        listOf(
            RegionalWasteScheduleTimeFormat(
                resId = R.string.regional_waste_schedule_time_range_format,
                args = listOf("18:00", "23:00"),
            ),
            RegionalWasteScheduleTimeFormat(
                resId = R.string.regional_waste_schedule_time_after_format,
                args = listOf("18:00"),
            ),
            RegionalWasteScheduleTimeFormat(
                resId = R.string.regional_waste_schedule_time_before_format,
                args = listOf("23:00"),
            ),
        ).forEach { timeFormat ->
            val candidate = candidate(
                sido = "대구광역시",
                sigungu = "군위군",
                managementZoneName = "없음",
                targetRegionName = "없음",
                schedules = listOf(
                    RegionalWasteScheduleUiModel(
                        wasteTypeName = "일반쓰레기",
                        disposalTimeFormat = timeFormat,
                    )
                ),
            )

            assertEquals("대구광역시 > 군위군", candidate.displayText)
            assertEquals(
                RegionalGuideCandidateDisplayText.Resource(
                    resId = R.string.regional_guide_candidate_schedule_summary_format,
                    args = listOf(
                        "일반쓰레기",
                        RegionalGuideCandidateDisplayText.Resource(
                            resId = timeFormat.resId,
                            args = timeFormat.args,
                        ),
                    ),
                ),
                candidate.collectionTypeDistinguishingText?.value,
            )
        }
    }

    @Test
    fun `후보 fallback 지역명은 화면용 리소스로 조합한다`() {
        val candidate = candidate(
            sido = "경기도",
            sigungu = "성남시",
            managementZoneName = "없음",
            targetRegionName = "없음",
            disposalPlaceType = "문전수거",
        )

        assertEquals(
            RegionalGuideCandidateDisplayText.Resource(
                resId = R.string.regional_guide_candidate_label_format,
                args = listOf(
                    RegionalGuideCandidateDisplayText.Resource(
                        resId = R.string.regional_guide_region_two_name_format,
                        args = listOf("경기도", "성남시"),
                    ),
                    "문전수거",
                ),
            ),
            candidate.displayTextForRow,
        )
    }

    @Test
    fun `후보 지역명이 없으면 기본 지역 리소스를 표시한다`() {
        val candidate = candidate(
            sido = null,
            sigungu = null,
            eupmyeondong = null,
            managementZoneName = "없음",
            targetRegionName = "없음",
            disposalPlaceType = "문전수거",
        )

        assertEquals(
            RegionalGuideCandidateDisplayText.Resource(
                resId = R.string.regional_guide_candidate_label_format,
                args = listOf(
                    RegionalGuideCandidateDisplayText.Resource(
                        resId = R.string.regional_guide_default_region_name,
                        args = emptyList(),
                    ),
                    "문전수거",
                ),
            ),
            candidate.displayTextForRow,
        )
    }

    @Test
    fun `동일 관리구역 후보는 대상지역명 기준으로 정렬한다`() {
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
    fun `관리구역이 다른 후보는 관리구역명 기준으로 먼저 정렬한다`() {
        val candidates = listOf(
            candidate(managementZoneName = "노은3동", targetRegionName = "반석동"),
            candidate(managementZoneName = "노은2동", targetRegionName = "반석동 일부지역"),
            candidate(managementZoneName = "노은3동", targetRegionName = "반석동 일부지역"),
            candidate(managementZoneName = "노은2동", targetRegionName = "반석동+하기동"),
            candidate(managementZoneName = "노은2동", targetRegionName = "수남동+안산동+외삼동")
        )

        val sorted = candidates.sortedWith(regionalGuideCandidateDisplayComparator)

        assertEquals(
            listOf(
                "노은2동 / 반석동 일부지역",
                "노은2동 / 반석동+하기동",
                "노은2동 / 수남동+안산동+외삼동",
                "노은3동 / 반석동",
                "노은3동 / 반석동 일부지역"
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

    @Test
    fun `안정 키는 표시명이 같은 후보도 식별 가능한 값 조합으로 구분한다`() {
        val doorToDoorCandidate = candidate(
            sido = "대전광역시",
            sigungu = "서구",
            managementZoneName = "대전광역시",
            targetRegionName = "서구",
            disposalPlaceType = "문전수거"
        )
        val basePointCandidate = candidate(
            sido = "대전광역시",
            sigungu = "서구",
            managementZoneName = "대전광역시",
            targetRegionName = "서구",
            disposalPlaceType = "거점수거"
        )

        assertEquals(doorToDoorCandidate.displayText, basePointCandidate.displayText)
        assertNotEquals(doorToDoorCandidate.displayText, doorToDoorCandidate.stableKey)
        assertNotEquals(doorToDoorCandidate.stableKey, basePointCandidate.stableKey)
    }

    @Test
    fun `안정 키는 배출장소만 다른 일정 후보도 구분한다`() {
        val firstCandidate = candidate(
            managementZoneName = "1권역",
            targetRegionName = "신당동",
            schedules = listOf(
                RegionalWasteScheduleUiModel(
                    wasteTypeName = "대형폐기물",
                    disposalPlace = "1번 지정 장소",
                )
            )
        )
        val secondCandidate = candidate(
            managementZoneName = "1권역",
            targetRegionName = "신당동",
            schedules = listOf(
                RegionalWasteScheduleUiModel(
                    wasteTypeName = "대형폐기물",
                    disposalPlace = "2번 지정 장소",
                )
            )
        )

        assertEquals(firstCandidate.displayText, secondCandidate.displayText)
        assertNotEquals(firstCandidate.stableKey, secondCandidate.stableKey)
    }

    @Test
    fun `문전수거와 거점수거 원본 유형은 후보 보조 설명 힌트로 구분한다`() {
        val doorToDoorCandidate = candidate(
            managementZoneName = "문전수거 지역",
            targetRegionName = "문전수거 지역",
            disposalPlaceType = "문전수거"
        )
        val basePointCandidate = candidate(
            managementZoneName = "거점수거 지역",
            targetRegionName = "거점수거 지역",
            disposalPlaceType = "거점수거"
        )

        assertEquals(
            RegionalGuideCandidateCollectionTypeHint.DOOR_TO_DOOR,
            doorToDoorCandidate.collectionTypeHint
        )
        assertEquals(
            RegionalGuideCandidateCollectionTypeHint.BASE_POINT,
            basePointCandidate.collectionTypeHint
        )
    }

    @Test
    fun `수거 유형 보조 설명 힌트는 배출장소 설명만으로 임의 판정하지 않는다`() {
        val candidate = candidate(
            managementZoneName = "문전수거 지역",
            targetRegionName = "문전수거 지역",
            disposalPlaceType = null,
            disposalPlaceDescription = "문전수거 방식으로 배출"
        )

        assertEquals(null, candidate.collectionTypeHint)
    }

    private fun candidate(
        regionName: String = "대전광역시 유성구",
        sido: String? = "대전광역시",
        sigungu: String? = "유성구",
        eupmyeondong: String? = null,
        managementZoneName: String?,
        targetRegionName: String?,
        disposalPlaceType: String? = null,
        disposalPlaceDescription: String? = null,
        uncollectedDays: String? = null,
        departmentInfo: String? = null,
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
                uncollectedDays = uncollectedDays,
                departmentInfo = departmentInfo
            ),
            sido = sido,
            sigungu = sigungu,
            eupmyeondong = eupmyeondong
        )
}
