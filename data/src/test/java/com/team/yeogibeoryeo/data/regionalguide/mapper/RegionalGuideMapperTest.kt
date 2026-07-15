package com.team.yeogibeoryeo.data.regionalguide.mapper

import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemDto
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegionalGuideMapperTest {

    @Test
    fun `대상지역이 없음이면 Region 읍면동을 덮어쓰지 않는다`() {
        val result = RegionalGuideMapper.mapToDomain(
            baseRegion = Region(
                sigungu = "수원시",
            ),
            dto = RegionalGuideItemDto(
                sidoName = "경기도",
                sigunguName = "수원시",
                managementZoneName = "수원시",
                dongName = "없음",
            )
        )

        assertEquals("경기도", result.region.sido)
        assertEquals("수원시", result.region.sigungu)
        assertNull(result.region.eupmyeondong)
        assertEquals("없음", result.targetRegionName)
    }

    @Test
    fun `baseRegion에 시도가 없으면 DTO 시도명으로 보완한다`() {
        val result = RegionalGuideMapper.mapToDomain(
            baseRegion = Region(
                sigungu = "성남시",
            ),
            dto = RegionalGuideItemDto(
                sidoName = "경기도",
                sigunguName = "성남시",
                managementZoneName = "성남시",
                dongName = "성남시 전체",
            )
        )

        assertEquals("경기도", result.region.sido)
        assertEquals("성남시", result.region.sigungu)
        assertNull(result.region.eupmyeondong)
    }

    @Test
    fun `세종특별자치시는 DTO 시군구명이 없음이어도 시군구를 null로 유지한다`() {
        val result = RegionalGuideMapper.mapToDomain(
            baseRegion = Region(
                sido = "세종특별자치시",
                eupmyeondong = "조치원읍",
            ),
            dto = RegionalGuideItemDto(
                sidoName = "세종특별자치시",
                sigunguName = "없음",
                managementZoneName = "세종특별자치시",
                dongName = "없음",
            )
        )

        assertEquals("세종특별자치시", result.region.sido)
        assertNull(result.region.sigungu)
        assertEquals("조치원읍", result.region.eupmyeondong)
    }

    @Test
    fun `세종특별자치시는 읍면동이 없으면 시도만 유지한다`() {
        val result = RegionalGuideMapper.mapToDomain(
            baseRegion = Region(
                sido = "세종특별자치시",
            ),
            dto = RegionalGuideItemDto(
                sidoName = "세종특별자치시",
                sigunguName = "없음",
                managementZoneName = "세종특별자치시",
                dongName = "없음",
            )
        )

        assertEquals("세종특별자치시", result.region.sido)
        assertNull(result.region.sigungu)
        assertNull(result.region.eupmyeondong)
    }

    @Test
    fun `대상지역이 구체적인 읍면동이어도 Region 읍면동을 덮어쓰지 않는다`() {
        val result = RegionalGuideMapper.mapToDomain(
            baseRegion = Region(
                sido = "서울특별시",
                sigungu = "영등포구",
            ),
            dto = RegionalGuideItemDto(
                sidoName = "서울특별시",
                sigunguName = "영등포구",
                managementZoneName = "영등포구",
                dongName = "문래동",
            )
        )

        assertNull(result.region.eupmyeondong)
        assertEquals("문래동", result.targetRegionName)
    }

    @Test
    fun `기존 Region 읍면동이 있으면 대상지역 설명과 분리하여 유지한다`() {
        val result = RegionalGuideMapper.mapToDomain(
            baseRegion = Region(
                sido = "인천광역시",
                sigungu = "중구",
                eupmyeondong = "신흥동",
            ),
            dto = RegionalGuideItemDto(
                sidoName = "인천광역시",
                sigunguName = "중구",
                managementZoneName = "중구",
                dongName = "신흥동+율목동+영종동+영종1동+영종2동+용유동",
            )
        )

        assertEquals("신흥동", result.region.eupmyeondong)
        assertEquals("신흥동+율목동+영종동+영종1동+영종2동+용유동", result.targetRegionName)
    }

    @Test
    fun `대상지역이 시군구 설명이면 Region 읍면동으로 사용하지 않는다`() {
        val result = RegionalGuideMapper.mapToDomain(
            baseRegion = Region(
                sido = "서울특별시",
                sigungu = "중구",
            ),
            dto = RegionalGuideItemDto(
                sidoName = "서울특별시",
                sigunguName = "중구",
                managementZoneName = "서울시 중구",
                dongName = "서울시 중구",
            )
        )

        assertNull(result.region.eupmyeondong)
        assertEquals("서울시 중구", result.targetRegionName)
    }

    @Test
    fun `시간파싱_유효하지 않은 입력은 null 반환한다`() {
        assertNull(RegionalWasteScheduleMapper.parseTime("00:00"))
        assertNull(RegionalWasteScheduleMapper.parseTime("0000"))
        assertNull(RegionalWasteScheduleMapper.parseTime("   "))
        assertNull(RegionalWasteScheduleMapper.parseTime(null))

        assertEquals("18:00", RegionalWasteScheduleMapper.parseTime(" 18:00 "))
    }

    @Test
    fun `콜론 없는 시간은 시와 분을 구분해 표시한다`() {
        assertEquals("18:30", RegionalWasteScheduleMapper.parseTime("1830"))
        assertEquals("24:00", RegionalWasteScheduleMapper.parseTime("2400"))
        assertEquals("2460", RegionalWasteScheduleMapper.parseTime("2460"))
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

    @Test
    fun `관리 부서와 연락처는 도메인 모델로 전달한다`() {
        val result = RegionalGuideMapper.mapToDomain(
            baseRegion = Region(sido = "서울특별시", sigungu = "중구"),
            dto = RegionalGuideItemDto(
                departmentName = "청소행정과",
                departmentPhoneNumber = "02-1234-5678",
            )
        )

        assertEquals("청소행정과", result.departmentName)
        assertEquals("02-1234-5678", result.departmentPhoneNumber)
    }

    @Test
    fun `EMSN_PLC 배출장소는 이번 범위에서 도메인 표시 모델로 전달하지 않는다`() {
        val result = RegionalGuideMapper.mapToDomain(
            baseRegion = Region(sido = "서울특별시", sigungu = "중구"),
            dto = RegionalGuideItemDto(
                disposalPlaceType = "문전수거",
                disposalPlace = "문 앞 지정 장소",
            )
        )

        assertEquals("문전수거", result.disposalPlaceType)
        assertNull(result.disposalPlaceDescription)
    }

    @Test
    fun `대형폐기물 배출장소는 요일과 분리해 일정 장소로 전달한다`() {
        val result = RegionalGuideMapper.mapToDomain(
            baseRegion = Region(sido = "서울특별시", sigungu = "중구"),
            dto = RegionalGuideItemDto(
                largeItemDisposalPlace = "대형폐기물 지정 장소",
            )
        )

        val schedule = result.schedules.single()
        assertEquals(RegionalWasteType.LARGE_ITEM, schedule.wasteType)
        assertNull(schedule.disposalDays)
        assertNull(schedule.disposalMethod)
        assertEquals("대형폐기물 지정 장소", schedule.disposalPlace)
    }

    @Test
    fun `출처 메타데이터 필드를 도메인 메타데이터로 변환한다`() {
        val result = RegionalGuideMapper.mapToDomain(
            baseRegion = Region(sido = "Seoul", sigungu = "Junggu"),
            dto = RegionalGuideItemDto(
                managementNumber = " MNG-1 ",
                lastModifiedPoint = " 20240709010101 ",
                dataCriteriaDate = " 20240709 ",
                dataUpdatedPoint = " ",
                dataUpdateType = " U ",
            )
        )

        val metadata = checkNotNull(result.sourceMetadata)

        assertEquals("MNG-1", metadata.managementNumber)
        assertEquals("20240709010101", metadata.lastModifiedPoint)
        assertEquals("20240709", metadata.dataCriteriaDate)
        assertNull(metadata.dataUpdatedPoint)
        assertEquals("U", metadata.dataUpdateType)
    }

    @Test
    fun `출처 메타데이터 필드가 모두 비어 있으면 널로 반환한다`() {
        val result = RegionalGuideMapper.mapToDomain(
            baseRegion = Region(sido = "Seoul", sigungu = "Junggu"),
            dto = RegionalGuideItemDto(
                managementNumber = " ",
                lastModifiedPoint = " ",
                dataCriteriaDate = null,
                dataUpdatedPoint = " ",
                dataUpdateType = null,
            )
        )

        assertNull(result.sourceMetadata)
    }
}
