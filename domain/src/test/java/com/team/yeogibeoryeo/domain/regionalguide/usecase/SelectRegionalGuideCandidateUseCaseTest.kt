package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupResult
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteSchedule
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectRegionalGuideDirectMatchUseCaseTest {

    private val useCase = SelectRegionalGuideCandidateUseCase()

    @Test
    fun `후보가 없으면 NotFound를 반환한다`() {
        val result = useCase(
            candidates = emptyList(),
            query = regionalGuideQuery(
                displayRegion = Region(sido = "서울특별시", sigungu = "중구"),
                sigunguQuery = "중구"
            )
        )

        assertEquals(RegionalGuideLookupResult.NotFound, result)
    }

    @Test
    fun `동일 시군구명이 여러 시도에 있으면 선택한 시도 기준으로 후보를 고른다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(sido = "대구광역시", sigungu = "중구", targetRegionName = "대봉2동"),
                regionalDisposalGuide(sido = "서울특별시", sigungu = "중구", targetRegionName = "서울시 중구")
            ),
            query = regionalGuideQuery(
                displayRegion = Region(sido = "서울특별시", sigungu = "중구"),
                sigunguQuery = "중구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("서울특별시", guide.region.sido)
        assertEquals("중구", guide.region.sigungu)
        assertEquals("서울시 중구", guide.targetRegionName)
    }

    @Test
    fun `선택한 시도와 일치하는 후보가 없으면 CandidateNotFound를 반환한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(sido = "대구광역시", sigungu = "중구", targetRegionName = "대봉2동")
            ),
            query = regionalGuideQuery(
                displayRegion = Region(sido = "서울특별시", sigungu = "중구"),
                sigunguQuery = "중구"
            )
        )

        assertEquals(RegionalGuideLookupResult.CandidateNotFound, result)
    }

    @Test
    fun `대상지역 설명에 선택 읍면동이 포함되면 해당 후보를 선택하고 Region 읍면동은 선택값으로 유지한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "인천광역시",
                    sigungu = "중구",
                    targetRegionName = "신흥동+율목동+영종동+영종1동+영종2동+용유동"
                ),
                regionalDisposalGuide(
                    sido = "인천광역시",
                    sigungu = "중구",
                    targetRegionName = "신포동+연안동+도원동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "인천광역시",
                    sigungu = "중구",
                    eupmyeondong = "신흥동"
                ),
                sigunguQuery = "중구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("신흥동", guide.region.eupmyeondong)
        assertEquals(
            "신흥동+율목동+영종동+영종1동+영종2동+용유동",
            guide.targetRegionName
        )
    }

    @Test
    fun `세종특별자치시 동지역에 포함된 동은 동지역 후보를 선택한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(sido = "세종특별자치시", sigungu = null, targetRegionName = "전의면, 전동면, 소정면"),
                regionalDisposalGuide(sido = "세종특별자치시", sigungu = null, targetRegionName = "동지역")
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "세종특별자치시",
                    eupmyeondong = "한솔동"
                ),
                sigunguQuery = "없음"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("한솔동", guide.region.eupmyeondong)
        assertEquals("동지역", guide.targetRegionName)
    }

    @Test
    fun `필터링된 후보가 1건이면 대상지역 값과 관계없이 선택한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(sido = "경기도", sigungu = "수원시", targetRegionName = "수원시 전체")
            ),
            query = regionalGuideQuery(
                displayRegion = Region(sido = "경기도", sigungu = "수원시"),
                sigunguQuery = "수원시"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("수원시 전체", guide.targetRegionName)
    }

    @Test
    fun `단일 후보의 대상지역이 없음이어도 해당 후보를 선택한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(sido = "경기도", sigungu = "수원시", targetRegionName = "없음")
            ),
            query = regionalGuideQuery(
                displayRegion = Region(sido = "경기도", sigungu = "수원시 장안구"),
                sigunguQuery = "수원시"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("없음", guide.targetRegionName)
        assertEquals("수원시 장안구", guide.region.sigungu)
    }

    @Test
    fun `단일 후보의 대상지역이 동 목록이어도 해당 후보를 선택한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "서울특별시",
                    sigungu = "강남구",
                    targetRegionName = "신사동+압구정동+논현1동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(sido = "서울특별시", sigungu = "강남구"),
                sigunguQuery = "강남구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("신사동+압구정동+논현1동", guide.targetRegionName)
    }

    @Test
    fun `읍면동 접미사가 없는 대상지역 토큰도 선택 읍면동과 매칭한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "울산광역시",
                    sigungu = "울주군",
                    targetRegionName = "범서, 온양, 웅촌, 언양, 삼남, 상북, 온산, 청량, 서생"
                ),
                regionalDisposalGuide(
                    sido = "울산광역시",
                    sigungu = "울주군",
                    targetRegionName = "두동, 두서, 삼동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "울산광역시",
                    sigungu = "울주군",
                    eupmyeondong = "온양읍"
                ),
                sigunguQuery = "울주군"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("온양읍", guide.region.eupmyeondong)
        assertEquals(
            "범서, 온양, 웅촌, 언양, 삼남, 상북, 온산, 청량, 서생",
            guide.targetRegionName
        )
    }
}

class SelectRegionalGuideCandidateIdentityUseCaseTest {

    private val useCase = SelectRegionalGuideCandidateUseCase()

    @Test
    fun `동일 대상지역명이어도 관리구역명이 다르면 첫 후보를 임의 선택하지 않고 후보 목록을 반환한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    managementZoneName = "노은2동",
                    targetRegionName = "반석동 일부지역"
                ),
                regionalDisposalGuide(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    managementZoneName = "노은3동",
                    targetRegionName = "반석동 일부지역"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    eupmyeondong = "반석동"
                ),
                sigunguQuery = "유성구"
            )
        )

        val candidates = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(2, candidates.size)
        assertEquals("노은2동", candidates[0].managementZoneName)
        assertEquals("노은3동", candidates[1].managementZoneName)
    }

    @Test
    fun `관리구역명이 선택 읍면동과 정확히 일치하면 직접 매칭 후보로 선택한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    managementZoneName = "온천1동",
                    targetRegionName = "봉명동 호텔주변"
                ),
                regionalDisposalGuide(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    managementZoneName = "온천2동",
                    targetRegionName = "장대동+죽동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    eupmyeondong = "온천1동"
                ),
                sigunguQuery = "유성구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("온천1동", guide.managementZoneName)
        assertEquals("봉명동 호텔주변", guide.targetRegionName)
    }

    @Test
    fun `관리구역명으로 직접 매칭되는 후보가 여러 개면 후보 목록을 반환한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    managementZoneName = "온천1동",
                    targetRegionName = "봉명동 호텔주변"
                ),
                regionalDisposalGuide(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    managementZoneName = "온천1동",
                    targetRegionName = "구암동+덕명동+복용동+봉명동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    eupmyeondong = "온천1동"
                ),
                sigunguQuery = "유성구"
            )
        )

        val candidates = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(2, candidates.size)
        assertEquals("봉명동 호텔주변", candidates[0].targetRegionName)
        assertEquals("구암동+덕명동+복용동+봉명동", candidates[1].targetRegionName)
    }

    @Test
    fun `법정동 매핑 후보와 일치하는 행정동 후보가 여러 개면 후보 목록을 반환한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "서울특별시",
                    sigungu = "노원구",
                    managementZoneName = "하계1동",
                    targetRegionName = "하계1동"
                ),
                regionalDisposalGuide(
                    sido = "서울특별시",
                    sigungu = "노원구",
                    managementZoneName = "하계2동",
                    targetRegionName = "하계2동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "서울특별시",
                    sigungu = "노원구",
                    eupmyeondong = "하계동"
                ),
                sigunguQuery = "노원구"
            ),
            mappedAdminDongCandidates = listOf(
                Region(sido = "서울특별시", sigungu = "노원구", eupmyeondong = "하계1동"),
                Region(sido = "서울특별시", sigungu = "노원구", eupmyeondong = "하계2동")
            )
        )

        val candidates = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(2, candidates.size)
        assertEquals("하계1동", candidates[0].managementZoneName)
        assertEquals("하계2동", candidates[1].managementZoneName)
    }

    @Test
    fun `기존 직접 매칭 후보가 있으면 법정동 매핑 후보보다 우선한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "서울특별시",
                    sigungu = "노원구",
                    managementZoneName = "하계동",
                    targetRegionName = "하계동"
                ),
                regionalDisposalGuide(
                    sido = "서울특별시",
                    sigungu = "노원구",
                    managementZoneName = "하계1동",
                    targetRegionName = "하계1동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "서울특별시",
                    sigungu = "노원구",
                    eupmyeondong = "하계동"
                ),
                sigunguQuery = "노원구"
            ),
            mappedAdminDongCandidates = listOf(
                Region(sido = "서울특별시", sigungu = "노원구", eupmyeondong = "하계1동")
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("하계동", guide.managementZoneName)
    }

    @Test
    fun `법정동 매핑 후보와 일치하는 행정동 후보가 하나면 해당 후보를 선택한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "서울특별시",
                    sigungu = "노원구",
                    managementZoneName = "상계1동",
                    targetRegionName = "상계1동"
                ),
                regionalDisposalGuide(
                    sido = "서울특별시",
                    sigungu = "노원구",
                    managementZoneName = "하계1동",
                    targetRegionName = "하계1동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "서울특별시",
                    sigungu = "노원구",
                    eupmyeondong = "하계동"
                ),
                sigunguQuery = "노원구"
            ),
            mappedAdminDongCandidates = listOf(
                Region(sido = "서울특별시", sigungu = "노원구", eupmyeondong = "하계1동")
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("하계1동", guide.managementZoneName)
    }

    @Test
    fun `제 표기가 있는 행정동 매핑 후보는 숫자 범위 축약 후보와 매칭된다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "부산광역시",
                    sigungu = "사하구",
                    managementZoneName = "1권역",
                    targetRegionName = "괴정 1~3동, 하단 1~2동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "부산광역시",
                    sigungu = "사하구",
                    eupmyeondong = "괴정동"
                ),
                sigunguQuery = "사하구"
            ),
            mappedAdminDongCandidates = listOf(
                Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정제1동")
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("괴정 1~3동, 하단 1~2동", guide.targetRegionName)
    }

    @Test
    fun `제 표기가 있는 행정동 매핑 후보는 쉼표 묶음 축약 후보와 매칭된다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "부산광역시",
                    sigungu = "금정구",
                    managementZoneName = "1권역",
                    targetRegionName = "부곡1,4동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "부산광역시",
                    sigungu = "금정구",
                    eupmyeondong = "부곡동"
                ),
                sigunguQuery = "금정구"
            ),
            mappedAdminDongCandidates = listOf(
                Region(sido = "부산광역시", sigungu = "금정구", eupmyeondong = "부곡제1동")
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("부곡1,4동", guide.targetRegionName)
    }

    @Test
    fun `제 표기가 있는 행정동 매핑 후보는 공백이 있는 쉼표 묶음 축약 후보와 매칭된다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "부산광역시",
                    sigungu = "금정구",
                    managementZoneName = "1권역",
                    targetRegionName = "부곡 1, 4동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "부산광역시",
                    sigungu = "금정구",
                    eupmyeondong = "부곡동"
                ),
                sigunguQuery = "금정구"
            ),
            mappedAdminDongCandidates = listOf(
                Region(sido = "부산광역시", sigungu = "금정구", eupmyeondong = "부곡제4동")
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("부곡 1, 4동", guide.targetRegionName)
    }

    @Test
    fun `제 표기가 있는 행정동 매핑 후보는 공백이 있는 숫자 범위 축약 후보와 매칭된다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "부산광역시",
                    sigungu = "사하구",
                    managementZoneName = "2권역",
                    targetRegionName = "다대 1~2동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "부산광역시",
                    sigungu = "사하구",
                    eupmyeondong = "다대동"
                ),
                sigunguQuery = "사하구"
            ),
            mappedAdminDongCandidates = listOf(
                Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "다대제1동")
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("다대 1~2동", guide.targetRegionName)
    }

    @Test
    fun `숫자 범위 밖 행정동 매핑 후보는 축약 후보와 매칭되지 않는다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "부산광역시",
                    sigungu = "사하구",
                    managementZoneName = "1권역",
                    targetRegionName = "괴정 1~3동, 하단 1~2동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "부산광역시",
                    sigungu = "사하구",
                    eupmyeondong = "괴정동"
                ),
                sigunguQuery = "사하구"
            ),
            mappedAdminDongCandidates = listOf(
                Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정제4동")
            )
        )

        assertEquals(RegionalGuideLookupResult.CandidateNotFound, result)
    }

    @Test
    fun `쉼표 묶음 밖 행정동 매핑 후보는 축약 후보와 매칭되지 않는다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "부산광역시",
                    sigungu = "금정구",
                    managementZoneName = "1권역",
                    targetRegionName = "부곡1,4동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "부산광역시",
                    sigungu = "금정구",
                    eupmyeondong = "부곡동"
                ),
                sigunguQuery = "금정구"
            ),
            mappedAdminDongCandidates = listOf(
                Region(sido = "부산광역시", sigungu = "금정구", eupmyeondong = "부곡제2동")
            )
        )

        assertEquals(RegionalGuideLookupResult.CandidateNotFound, result)
    }

    @Test
    fun `직접 선택한 괴정제1동은 숫자 범위 축약 후보와 매칭된다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "부산광역시",
                    sigungu = "사하구",
                    managementZoneName = "1권역",
                    targetRegionName = "괴정 1~3동, 하단 1~2동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "부산광역시",
                    sigungu = "사하구",
                    eupmyeondong = "괴정제1동"
                ),
                sigunguQuery = "사하구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("괴정제1동", guide.region.eupmyeondong)
        assertEquals("괴정 1~3동, 하단 1~2동", guide.targetRegionName)
    }

    @Test
    fun `직접 선택한 부곡제1동은 쉼표 묶음 축약 후보와 매칭된다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "부산광역시",
                    sigungu = "금정구",
                    managementZoneName = "1권역",
                    targetRegionName = "부곡1,4동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "부산광역시",
                    sigungu = "금정구",
                    eupmyeondong = "부곡제1동"
                ),
                sigunguQuery = "금정구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("부곡제1동", guide.region.eupmyeondong)
        assertEquals("부곡1,4동", guide.targetRegionName)
    }

    @Test
    fun `직접 선택한 부곡제4동은 쉼표 묶음 축약 후보와 매칭된다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "부산광역시",
                    sigungu = "금정구",
                    managementZoneName = "1권역",
                    targetRegionName = "부곡1,4동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "부산광역시",
                    sigungu = "금정구",
                    eupmyeondong = "부곡제4동"
                ),
                sigunguQuery = "금정구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("부곡제4동", guide.region.eupmyeondong)
        assertEquals("부곡1,4동", guide.targetRegionName)
    }

    @Test
    fun `직접 선택한 부곡제1동은 공백이 있는 쉼표 묶음 축약 후보와 매칭된다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "부산광역시",
                    sigungu = "금정구",
                    managementZoneName = "1권역",
                    targetRegionName = "부곡 1, 4동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "부산광역시",
                    sigungu = "금정구",
                    eupmyeondong = "부곡제1동"
                ),
                sigunguQuery = "금정구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("부곡제1동", guide.region.eupmyeondong)
        assertEquals("부곡 1, 4동", guide.targetRegionName)
    }

    @Test
    fun `직접 선택한 부곡제2동은 쉼표 묶음 축약 후보와 매칭되지 않는다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "부산광역시",
                    sigungu = "금정구",
                    managementZoneName = "1권역",
                    targetRegionName = "부곡1,4동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "부산광역시",
                    sigungu = "금정구",
                    eupmyeondong = "부곡제2동"
                ),
                sigunguQuery = "금정구"
            )
        )

        assertEquals(RegionalGuideLookupResult.CandidateNotFound, result)
    }

    @Test
    fun `직접 선택한 부곡제3동은 쉼표 묶음 축약 후보와 매칭되지 않는다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "부산광역시",
                    sigungu = "금정구",
                    managementZoneName = "1권역",
                    targetRegionName = "부곡1,4동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "부산광역시",
                    sigungu = "금정구",
                    eupmyeondong = "부곡제3동"
                ),
                sigunguQuery = "금정구"
            )
        )

        assertEquals(RegionalGuideLookupResult.CandidateNotFound, result)
    }

    @Test
    fun `직접 선택한 다대제1동은 공백이 있는 숫자 범위 축약 후보와 매칭된다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "부산광역시",
                    sigungu = "사하구",
                    managementZoneName = "2권역",
                    targetRegionName = "다대 1~2동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "부산광역시",
                    sigungu = "사하구",
                    eupmyeondong = "다대제1동"
                ),
                sigunguQuery = "사하구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("다대제1동", guide.region.eupmyeondong)
        assertEquals("다대 1~2동", guide.targetRegionName)
    }

    @Test
    fun `직접 선택한 괴정제4동은 숫자 범위 밖 축약 후보와 매칭되지 않는다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "부산광역시",
                    sigungu = "사하구",
                    managementZoneName = "1권역",
                    targetRegionName = "괴정 1~3동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "부산광역시",
                    sigungu = "사하구",
                    eupmyeondong = "괴정제4동"
                ),
                sigunguQuery = "사하구"
            )
        )

        assertEquals(RegionalGuideLookupResult.CandidateNotFound, result)
    }

    @Test
    fun `정확 매칭 후보가 있으면 축약 정규화 후보보다 우선한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "부산광역시",
                    sigungu = "사하구",
                    managementZoneName = "직접관리",
                    targetRegionName = "괴정제1동"
                ),
                regionalDisposalGuide(
                    sido = "부산광역시",
                    sigungu = "사하구",
                    managementZoneName = "1권역",
                    targetRegionName = "괴정 1~3동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "부산광역시",
                    sigungu = "사하구",
                    eupmyeondong = "괴정제1동"
                ),
                sigunguQuery = "사하구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("직접관리", guide.managementZoneName)
        assertEquals("괴정제1동", guide.targetRegionName)
    }

    @Test
    fun `직접 선택한 행정동이 여러 축약 후보와 매칭되면 후보 목록을 반환한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "부산광역시",
                    sigungu = "사하구",
                    managementZoneName = "1권역",
                    targetRegionName = "괴정 1~3동"
                ),
                regionalDisposalGuide(
                    sido = "부산광역시",
                    sigungu = "사하구",
                    managementZoneName = "별도관리",
                    targetRegionName = "괴정1동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "부산광역시",
                    sigungu = "사하구",
                    eupmyeondong = "괴정제1동"
                ),
                sigunguQuery = "사하구"
            )
        )

        val candidates = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(2, candidates.size)
        assertEquals("1권역", candidates[0].managementZoneName)
        assertEquals("별도관리", candidates[1].managementZoneName)
    }

    @Test
    fun `법정동 매핑 후보가 있어도 info 후보와 교집합이 없으면 기존 CandidateNotFound 흐름을 유지한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "서울특별시",
                    sigungu = "노원구",
                    managementZoneName = "상계1동",
                    targetRegionName = "상계1동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "서울특별시",
                    sigungu = "노원구",
                    eupmyeondong = "하계동"
                ),
                sigunguQuery = "노원구"
            ),
            mappedAdminDongCandidates = listOf(
                Region(sido = "서울특별시", sigungu = "노원구", eupmyeondong = "하계1동")
            )
        )

        assertEquals(RegionalGuideLookupResult.CandidateNotFound, result)
    }

    @Test
    fun `대상지역명과 관리구역명 기준에서 서로 다른 후보가 잡히면 후보 목록을 반환한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    managementZoneName = "관리구역A",
                    targetRegionName = "온천1동"
                ),
                regionalDisposalGuide(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    managementZoneName = "온천1동",
                    targetRegionName = "별도 대상지역"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    eupmyeondong = "온천1동"
                ),
                sigunguQuery = "유성구"
            )
        )

        val candidates = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(2, candidates.size)
        assertEquals("관리구역A", candidates[0].managementZoneName)
        assertEquals("온천1동", candidates[1].managementZoneName)
    }
}

class SelectRegionalGuideCandidateMergeUseCaseTest {

    private val useCase = SelectRegionalGuideCandidateUseCase()

    @Test
    fun `완전 중복 후보는 하나의 후보로 정리하고 상세 일정은 유지한다`() {
        val firstSchedule = RegionalWasteSchedule(
            wasteType = RegionalWasteType.GENERAL,
            disposalDays = "월"
        )
        val secondSchedule = RegionalWasteSchedule(
            wasteType = RegionalWasteType.FOOD,
            disposalDays = "화"
        )

        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    managementZoneName = "노은2동",
                    targetRegionName = "반석동 일부지역",
                    schedules = listOf(firstSchedule)
                ),
                regionalDisposalGuide(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    managementZoneName = "노은2동",
                    targetRegionName = "반석동 일부지역",
                    schedules = listOf(secondSchedule)
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    eupmyeondong = "반석동"
                ),
                sigunguQuery = "유성구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("노은2동", guide.managementZoneName)
        assertEquals(listOf(firstSchedule, secondSchedule), guide.schedules)
    }

    @Test
    fun `same candidate names with different detail fields remain separate candidates`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    managementZoneName = "노은2동",
                    targetRegionName = "반석동 일부지역",
                    disposalPlaceDescription = "A구역"
                ),
                regionalDisposalGuide(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    managementZoneName = "노은2동",
                    targetRegionName = "반석동 일부지역",
                    disposalPlaceDescription = "B구역"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    eupmyeondong = "반석동"
                ),
                sigunguQuery = "유성구"
            )
        )

        val candidates = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(2, candidates.size)
        assertEquals("A구역", candidates[0].disposalPlaceDescription)
        assertEquals("B구역", candidates[1].disposalPlaceDescription)
    }
}

class SelectRegionalGuideWithoutEupmyeondongUseCaseTest {

    private val useCase = SelectRegionalGuideCandidateUseCase()

    @Test
    fun `읍면동 없이 복수 후보가 있으면 전체 적용 후보가 있어도 후보 목록을 반환한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(sido = "경기도", sigungu = "수원시", targetRegionName = "수원시 전체"),
                regionalDisposalGuide(sido = "경기도", sigungu = "수원시", targetRegionName = "일부 권역")
            ),
            query = regionalGuideQuery(
                displayRegion = Region(sido = "경기도", sigungu = "수원시"),
                sigunguQuery = "수원시"
            )
        )

        val candidates = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(2, candidates.size)
        assertEquals("수원시 전체", candidates[0].targetRegionName)
        assertEquals("일부 권역", candidates[1].targetRegionName)
    }

    @Test
    fun `읍면동 없이 복수 후보의 대상지역이 모두 전체 적용이면 대표 후보를 선택한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(sido = "경기도", sigungu = "성남시", targetRegionName = "없음"),
                regionalDisposalGuide(sido = "경기도", sigungu = "성남시", targetRegionName = "없음")
            ),
            query = regionalGuideQuery(
                displayRegion = Region(sido = "경기도", sigungu = "성남시"),
                sigunguQuery = "성남시"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("성남시", guide.region.sigungu)
        assertEquals("없음", guide.targetRegionName)
    }

    @Test
    fun `조회 key와 일치하는 시군구 후보가 없으면 CandidateNotFound를 반환한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(sido = "경기도", sigungu = "수원시 장안구", targetRegionName = "수원시 전체")
            ),
            query = regionalGuideQuery(
                displayRegion = Region(sido = "경기도", sigungu = "수원시 장안구"),
                sigunguQuery = "수원시"
            )
        )

        assertTrue(result is RegionalGuideLookupResult.CandidateNotFound)
    }

    @Test
    fun `읍면동 없이 복수 권역 후보만 있으면 후보 목록을 반환한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(sido = "인천광역시", sigungu = "중구", targetRegionName = "신흥동+율목동"),
                regionalDisposalGuide(sido = "인천광역시", sigungu = "중구", targetRegionName = "신포동+연안동")
            ),
            query = regionalGuideQuery(
                displayRegion = Region(sido = "인천광역시", sigungu = "중구"),
                sigunguQuery = "중구"
            )
        )

        val candidates = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(2, candidates.size)
        assertEquals("신흥동+율목동", candidates[0].targetRegionName)
        assertEquals("신포동+연안동", candidates[1].targetRegionName)
    }

    @Test
    fun `읍면동 없이 단일 권역 후보만 있으면 해당 후보를 선택한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(sido = "인천광역시", sigungu = "중구", targetRegionName = "신흥동+율목동")
            ),
            query = regionalGuideQuery(
                displayRegion = Region(sido = "인천광역시", sigungu = "중구"),
                sigunguQuery = "중구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("신흥동+율목동", guide.targetRegionName)
    }
}

class SelectRegionalGuidePreferredCandidateUseCaseTest {

    private val useCase = SelectRegionalGuideCandidateUseCase()

    @Test
    fun `preferred 대상지역이 있으면 해당 후보를 우선 선택한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(sido = "인천광역시", sigungu = "중구", targetRegionName = "신흥동+율목동"),
                regionalDisposalGuide(sido = "인천광역시", sigungu = "중구", targetRegionName = "신포동+연안동")
            ),
            query = regionalGuideQuery(
                displayRegion = Region(sido = "인천광역시", sigungu = "중구"),
                sigunguQuery = "중구"
            ),
            preferredTargetRegionName = "신포동+연안동"
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("신포동+연안동", guide.targetRegionName)
    }

    @Test
    fun `preferred 대상지역이 후보에 없으면 임의 후보를 선택하지 않는다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(sido = "인천광역시", sigungu = "중구", targetRegionName = "신흥동+율목동"),
                regionalDisposalGuide(sido = "인천광역시", sigungu = "중구", targetRegionName = "신포동+연안동")
            ),
            query = regionalGuideQuery(
                displayRegion = Region(sido = "인천광역시", sigungu = "중구"),
                sigunguQuery = "중구"
            ),
            preferredTargetRegionName = "사라진 권역"
        )

        assertEquals(RegionalGuideLookupResult.CandidateNotFound, result)
    }
}
