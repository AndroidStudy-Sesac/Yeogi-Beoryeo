package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteKey
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideCandidateLookupReason
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupResult
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideSourceMetadata
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteSchedule
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectRegionalGuideDirectMatchUseCaseTest {

    private val useCase = SelectRegionalGuideCandidateUseCase()

    @Test
    fun `후보가 없으면 찾지 못함을 반환한다`() {
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
    fun `선택한 시도와 일치하는 후보가 없으면 후보 없음으로 반환한다`() {
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
    fun `광주광역시 선택값은 전남광주통합특별시 광주 후보를 필터링하지 않는다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "전남광주통합특별시",
                    sigungu = "광산구",
                    targetRegionName = "광산구 전체"
                ),
                regionalDisposalGuide(
                    sido = "대구광역시",
                    sigungu = "광산구",
                    targetRegionName = "다른 시도 후보"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(sido = "광주광역시", sigungu = "광산구"),
                sigunguQuery = "광산구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("광주광역시", guide.region.sido)
        assertEquals("광산구", guide.region.sigungu)
        assertEquals("광산구 전체", guide.targetRegionName)
    }

    @Test
    fun `광주광역시 5개 구 선택값은 전남광주통합특별시 후보와 매칭된다`() {
        val gwangjuSigunguNames = listOf("동구", "서구", "남구", "북구", "광산구")

        gwangjuSigunguNames.forEach { sigungu ->
            val result = useCase(
                candidates = listOf(
                    regionalDisposalGuide(
                        sido = "전남광주통합특별시",
                        sigungu = sigungu,
                        targetRegionName = "$sigungu 전체"
                    )
                ),
                query = regionalGuideQuery(
                    displayRegion = Region(sido = "광주광역시", sigungu = sigungu),
                    sigunguQuery = sigungu
                )
            )

            val guide = (result as RegionalGuideLookupResult.Success).guide

            assertEquals(sigungu, guide.region.sigungu)
            assertEquals("$sigungu 전체", guide.targetRegionName)
        }
    }

    @Test
    fun `광주광역시 선택값은 전남광주통합특별시 전남 시군 후보를 선택하지 않는다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "전남광주통합특별시",
                    sigungu = "고흥군",
                    targetRegionName = "고흥군 전체"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(sido = "광주광역시", sigungu = "고흥군"),
                sigunguQuery = "고흥군"
            )
        )

        assertEquals(RegionalGuideLookupResult.CandidateNotFound, result)
    }

    @Test
    fun `전라남도 선택값은 전남광주통합특별시 전남 시군 후보와 매칭된다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "전남광주통합특별시",
                    sigungu = "고흥군",
                    targetRegionName = "고흥군 전체"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(sido = "전라남도", sigungu = "고흥군"),
                sigunguQuery = "고흥군"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("전라남도", guide.region.sido)
        assertEquals("고흥군", guide.region.sigungu)
        assertEquals("고흥군 전체", guide.targetRegionName)
    }

    @Test
    fun `전남광주통합특별시 전남 선택값은 전라남도 후보와 매칭된다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "전라남도",
                    sigungu = "나주시",
                    targetRegionName = "노안면"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "전남광주통합특별시",
                    sigungu = "나주시",
                    eupmyeondong = "노안면"
                ),
                sigunguQuery = "나주시"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("전남광주통합특별시", guide.region.sido)
        assertEquals("나주시", guide.region.sigungu)
        assertEquals("노안면", guide.region.eupmyeondong)
        assertEquals("노안면", guide.targetRegionName)
    }

    @Test
    fun `전남광주통합특별시 광주 선택값은 광주광역시 후보와 매칭된다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "광주광역시",
                    sigungu = "서구",
                    targetRegionName = "금호동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "전남광주통합특별시",
                    sigungu = "서구",
                    eupmyeondong = "금호동"
                ),
                sigunguQuery = "서구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("전남광주통합특별시", guide.region.sido)
        assertEquals("서구", guide.region.sigungu)
        assertEquals("금호동", guide.region.eupmyeondong)
        assertEquals("금호동", guide.targetRegionName)
    }

    @Test
    fun `대상지역 설명에 선택 읍면동이 포함되면 해당 후보를 선택하고 지역 읍면동은 선택값으로 유지한다`() {
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

    @Test
    fun `법정동 이름으로 시작하는 상세 대상지역 후보만 선택한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "경기도",
                    sigungu = "의왕시",
                    managementZoneName = "의왕시",
                    targetRegionName = "부곡중앙북6길",
                ),
                regionalDisposalGuide(
                    sido = "경기도",
                    sigungu = "의왕시",
                    managementZoneName = "의왕시",
                    targetRegionName = "가구단지길",
                ),
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "경기도",
                    sigungu = "의왕시",
                    eupmyeondong = "부곡동",
                ),
                sigunguQuery = "의왕시",
            ),
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("부곡중앙북6길", guide.targetRegionName)
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
    fun `법정동 매핑 관리구역 후보가 있으면 단일 대상지역명 정확 매칭보다 후보 목록을 우선한다`() {
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
                    managementZoneName = "노은2동",
                    targetRegionName = "죽동"
                ),
                regionalDisposalGuide(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    managementZoneName = "노은3동",
                    targetRegionName = "반석동"
                ),
                regionalDisposalGuide(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    managementZoneName = "노은3동",
                    targetRegionName = "지족동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    eupmyeondong = "반석동"
                ),
                sigunguQuery = "유성구"
            ),
            mappedAdminDongCandidates = listOf(
                Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "노은2동"),
                Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "노은3동")
            )
        )

        val candidates = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(
            listOf("노은2동", "노은3동"),
            candidates.map { guide -> guide.managementZoneName }
        )
        assertEquals(
            listOf("반석동 일부지역", "반석동"),
            candidates.map { guide -> guide.targetRegionName }
        )
    }

    @Test
    fun `출장소 행정동 후보는 부모 읍면동 가이드로 연결한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "대구광역시",
                    sigungu = "달성군",
                    managementZoneName = "다사읍",
                    targetRegionName = "다사읍"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "대구광역시",
                    sigungu = "달성군",
                    eupmyeondong = "다사읍서재출장소"
                ),
                sigunguQuery = "달성군"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("다사읍", guide.managementZoneName)
    }

    @Test
    fun `번호가 생략된 동 검색어는 번호가 붙은 관리구역명 후보와 매칭된다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    managementZoneName = "노은1동",
                    targetRegionName = "노은1동"
                ),
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
                ),
                regionalDisposalGuide(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    managementZoneName = "온천2동",
                    targetRegionName = "봉명동 일부지역"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    eupmyeondong = "노은동"
                ),
                sigunguQuery = "유성구"
            )
        )

        val candidates = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(3, candidates.size)
        assertEquals(
            listOf("노은1동", "노은2동", "노은3동"),
            candidates.map { guide -> guide.managementZoneName }
        )
    }

    @Test
    fun `번호가 생략된 동 검색어는 대상지역명보다 번호 관리구역명 후보를 우선한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    managementZoneName = "노은1동",
                    targetRegionName = "노은1동"
                ),
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
                ),
                regionalDisposalGuide(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    managementZoneName = "상대동",
                    targetRegionName = "노은동 일부지역"
                ),
                regionalDisposalGuide(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    managementZoneName = "신성동",
                    targetRegionName = "노은동 일부지역"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    eupmyeondong = "노은동"
                ),
                sigunguQuery = "유성구"
            )
        )

        val candidates = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(
            listOf("노은1동", "노은2동", "노은3동"),
            candidates.map { guide -> guide.managementZoneName }
        )
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
    fun `정확 매칭 후보가 있으면 넓은 행정동명 후보보다 우선한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    managementZoneName = "직접관리",
                    targetRegionName = "두류1.2동"
                ),
                regionalDisposalGuide(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    managementZoneName = "권역관리",
                    targetRegionName = "두류동+감삼동+신당동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    eupmyeondong = "두류1.2동"
                ),
                sigunguQuery = "달서구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("직접관리", guide.managementZoneName)
        assertEquals("두류1.2동", guide.targetRegionName)
    }

    @Test
    fun `세분화 행정동은 공공데이터의 넓은 동명 관리구역과 매칭된다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    managementZoneName = "두류동+감삼동+신당동",
                    targetRegionName = "두류동+감삼동+신당동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    eupmyeondong = "두류1.2동"
                ),
                sigunguQuery = "달서구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("두류1.2동", guide.region.eupmyeondong)
        assertEquals("두류동+감삼동+신당동", guide.managementZoneName)
    }

    @Test
    fun `점 대신 가운데점으로 묶인 세분화 행정동도 넓은 동명 관리구역과 매칭된다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    managementZoneName = "두류동",
                    targetRegionName = "두류동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    eupmyeondong = "두류1·2동"
                ),
                sigunguQuery = "달서구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("두류1·2동", guide.region.eupmyeondong)
        assertEquals("두류동", guide.targetRegionName)
    }

    @Test
    fun `가동 점 묶음 행정동은 분리된 응답 관리구역명과 매칭된다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "서울특별시",
                    sigungu = "성동구",
                    managementZoneName = "금호2가동+금호3가동",
                    targetRegionName = "금호2가동+금호3가동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "서울특별시",
                    sigungu = "성동구",
                    eupmyeondong = "금호2.3가동"
                ),
                sigunguQuery = "성동구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("금호2.3가동", guide.region.eupmyeondong)
        assertEquals("금호2가동+금호3가동", guide.targetRegionName)
    }

    @Test
    fun `점 묶음 행정동은 가운데점으로 묶인 응답 관리구역명과 매칭된다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "인천광역시",
                    sigungu = "미추홀구",
                    managementZoneName = "도화2·3동",
                    targetRegionName = "도화2·3동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "인천광역시",
                    sigungu = "미추홀구",
                    eupmyeondong = "도화2.3동"
                ),
                sigunguQuery = "미추홀구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("도화2.3동", guide.region.eupmyeondong)
        assertEquals("도화2·3동", guide.targetRegionName)
    }

    @Test
    fun `이름 점 묶음 행정동은 붙여쓴 응답 관리구역명과 매칭된다`() {
        listOf(
            "불로.봉무동" to "불로봉무동",
            "성내.충인동" to "성내충인동"
        ).forEach { (requestedEupmyeondong, apiRegionName) ->
            val result = useCase(
                candidates = listOf(
                    regionalDisposalGuide(
                        sido = "대구광역시",
                        sigungu = "동구",
                        managementZoneName = apiRegionName,
                        targetRegionName = apiRegionName
                    )
                ),
                query = regionalGuideQuery(
                    displayRegion = Region(
                        sido = "대구광역시",
                        sigungu = "동구",
                        eupmyeondong = requestedEupmyeondong
                    ),
                    sigunguQuery = "동구"
                )
            )

            val guide = (result as RegionalGuideLookupResult.Success).guide

            assertEquals(requestedEupmyeondong, guide.region.eupmyeondong)
            assertEquals(apiRegionName, guide.targetRegionName)
        }
    }

    @Test
    fun `점 묶음 행정동 확장은 범위 밖 행정동을 매칭하지 않는다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "서울특별시",
                    sigungu = "성동구",
                    managementZoneName = "금호4가동",
                    targetRegionName = "금호4가동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "서울특별시",
                    sigungu = "성동구",
                    eupmyeondong = "금호2.3가동"
                ),
                sigunguQuery = "성동구"
            )
        )

        assertEquals(RegionalGuideLookupResult.CandidateNotFound, result)
    }

    @Test
    fun `일동 계열 행정동은 공공데이터의 넓은 동명 관리구역과 매칭된다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    managementZoneName = "상인동",
                    targetRegionName = "상인동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    eupmyeondong = "상인1동"
                ),
                sigunguQuery = "달서구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("상인1동", guide.region.eupmyeondong)
        assertEquals("상인동", guide.managementZoneName)
    }

    @Test
    fun `전역 일부 괄호 설명이 붙은 관리구역명도 비교 가능한 동명 토큰으로 매칭한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    managementZoneName = "송현동(공동주택 일부)",
                    targetRegionName = "송현동 전역+상인1동 일부"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    eupmyeondong = "송현1동"
                ),
                sigunguQuery = "달서구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("송현1동", guide.region.eupmyeondong)
        assertEquals("송현동(공동주택 일부)", guide.managementZoneName)
    }

    @Test
    fun `넓은 동명 완화 매칭 후보가 여러 개이면 후보 목록을 반환한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    managementZoneName = "1권역",
                    targetRegionName = "성당동+두류동+본리동+감삼동+죽전동+장기동+용산동+이곡동+신당동"
                ),
                regionalDisposalGuide(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    managementZoneName = "2권역",
                    targetRegionName = "두류동+감삼동+신당동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    eupmyeondong = "두류1.2동"
                ),
                sigunguQuery = "달서구"
            )
        )

        val candidates = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(2, candidates.size)
        assertEquals(listOf("1권역", "2권역"), candidates.map { guide -> guide.managementZoneName })
    }

    @Test
    fun `다른 세부 행정동 토큰은 넓은 동명 완화 매칭으로 선택하지 않는다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    managementZoneName = "상인3동",
                    targetRegionName = "상인3동 일부"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    eupmyeondong = "상인1동"
                ),
                sigunguQuery = "달서구"
            )
        )

        assertEquals(RegionalGuideLookupResult.CandidateNotFound, result)
    }

    @Test
    fun `동지역 관리구역은 동으로 끝나는 행정동과 매칭된다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "경상북도",
                    sigungu = "김천시",
                    managementZoneName = "김천시",
                    targetRegionName = "동지역"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "경상북도",
                    sigungu = "김천시",
                    eupmyeondong = "율곡동"
                ),
                sigunguQuery = "김천시"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("율곡동", guide.region.eupmyeondong)
        assertEquals("동지역", guide.targetRegionName)
    }

    @Test
    fun `시군구명이 붙은 동지역 관리구역도 동으로 끝나는 행정동과 매칭된다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "원주시",
                    managementZoneName = "원주시",
                    targetRegionName = "원주시 동지역"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "강원특별자치도",
                    sigungu = "원주시",
                    eupmyeondong = "중앙동"
                ),
                sigunguQuery = "원주시"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("중앙동", guide.region.eupmyeondong)
        assertEquals("원주시 동지역", guide.targetRegionName)
    }

    @Test
    fun `띄어쓴 동 지역 관리구역도 동으로 끝나는 행정동과 매칭된다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "원주시",
                    managementZoneName = "원주시",
                    targetRegionName = "원주시 동 지역"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "강원특별자치도",
                    sigungu = "원주시",
                    eupmyeondong = "중앙동"
                ),
                sigunguQuery = "원주시"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("중앙동", guide.region.eupmyeondong)
        assertEquals("원주시 동 지역", guide.targetRegionName)
    }

    @Test
    fun `읍면지역 관리구역은 읍과 면으로 끝나는 행정구역과 매칭된다`() {
        listOf("아포읍", "봉산면").forEach { eupmyeondong ->
            val result = useCase(
                candidates = listOf(
                    regionalDisposalGuide(
                        sido = "경상북도",
                        sigungu = "김천시",
                        managementZoneName = "김천시",
                        targetRegionName = "김천시 읍면 지역"
                    )
                ),
                query = regionalGuideQuery(
                    displayRegion = Region(
                        sido = "경상북도",
                        sigungu = "김천시",
                        eupmyeondong = eupmyeondong
                    ),
                    sigunguQuery = "김천시"
                )
            )

            val guide = (result as RegionalGuideLookupResult.Success).guide

            assertEquals(eupmyeondong, guide.region.eupmyeondong)
            assertEquals("김천시 읍면 지역", guide.targetRegionName)
        }
    }

    @Test
    fun `읍면지역 관리구역은 동 선택과 매칭하지 않는다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "경상북도",
                    sigungu = "김천시",
                    managementZoneName = "김천시",
                    targetRegionName = "읍면지역"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "경상북도",
                    sigungu = "김천시",
                    eupmyeondong = "율곡동"
                ),
                sigunguQuery = "김천시"
            )
        )

        assertEquals(RegionalGuideLookupResult.CandidateNotFound, result)
    }

    @Test
    fun `세종특별자치시가 붙은 동지역 관리구역도 동으로 끝나는 행정동과 매칭된다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "세종특별자치시",
                    sigungu = null,
                    managementZoneName = "세종특별자치시",
                    targetRegionName = "세종특별자치시 동지역"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "세종특별자치시",
                    sigungu = null,
                    eupmyeondong = "나성동"
                ),
                sigunguQuery = "없음"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("나성동", guide.region.eupmyeondong)
        assertEquals("세종특별자치시 동지역", guide.targetRegionName)
    }

    @Test
    fun `단어 일부가 동지역인 관리구역은 동지역으로 매칭하지 않는다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "테스트도",
                    sigungu = "테스트시",
                    managementZoneName = "테스트시",
                    targetRegionName = "중동지역"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "테스트도",
                    sigungu = "테스트시",
                    eupmyeondong = "중앙동"
                ),
                sigunguQuery = "테스트시"
            )
        )

        assertEquals(RegionalGuideLookupResult.CandidateNotFound, result)
    }

    @Test
    fun `동지역 관리구역은 읍면 선택과 매칭하지 않는다`() {
        val eupResult = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "경상북도",
                    sigungu = "김천시",
                    managementZoneName = "김천시",
                    targetRegionName = "동지역"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "경상북도",
                    sigungu = "김천시",
                    eupmyeondong = "아포읍"
                ),
                sigunguQuery = "김천시"
            )
        )
        val myeonResult = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "경상북도",
                    sigungu = "김천시",
                    managementZoneName = "김천시",
                    targetRegionName = "동지역"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "경상북도",
                    sigungu = "김천시",
                    eupmyeondong = "봉산면"
                ),
                sigunguQuery = "김천시"
            )
        )

        assertEquals(RegionalGuideLookupResult.CandidateNotFound, eupResult)
        assertEquals(RegionalGuideLookupResult.CandidateNotFound, myeonResult)
    }

    @Test
    fun `동지역 관리구역 후보가 여러 개이면 후보 목록을 반환한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "경상북도",
                    sigungu = "김천시",
                    managementZoneName = "김천시 동지역 1권역",
                    targetRegionName = "동지역"
                ),
                regionalDisposalGuide(
                    sido = "경상북도",
                    sigungu = "김천시",
                    managementZoneName = "김천시 동지역 2권역",
                    targetRegionName = "동지역"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "경상북도",
                    sigungu = "김천시",
                    eupmyeondong = "율곡동"
                ),
                sigunguQuery = "김천시"
            )
        )

        val candidates = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(2, candidates.size)
        assertEquals(
            listOf("김천시 동지역 1권역", "김천시 동지역 2권역"),
            candidates.map { guide -> guide.managementZoneName }
        )
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
    fun `법정동 매핑 후보가 있어도 안내 후보와 교집합이 없으면 기존 후보 없음 흐름을 유지한다`() {
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

    @Test
    fun `직접 매칭이 실패하고 같은 시군구의 유형 후보만 여러 개 있으면 후보 목록을 반환한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "강릉시",
                    managementZoneName = "문전수거 지역",
                    targetRegionName = "문전수거 지역",
                    disposalPlaceType = "문전수거"
                ),
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "강릉시",
                    managementZoneName = "거점수거 지역",
                    targetRegionName = "거점수거 지역",
                    disposalPlaceType = "거점수거"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "강원특별자치도",
                    sigungu = "강릉시",
                    eupmyeondong = "사천면"
                ),
                sigunguQuery = "강릉시"
            )
        )

        val candidates = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(2, candidates.size)
        assertEquals("사천면", candidates[0].region.eupmyeondong)
        assertEquals("문전수거", candidates[0].disposalPlaceType)
        assertEquals("거점수거", candidates[1].disposalPlaceType)
    }

    @Test
    fun `직접 매칭 실패 후 없음 전체 기준 수거 유형 후보만 있으면 대체 후보 목록을 반환한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "없음",
                    targetRegionName = "없음",
                    disposalPlaceType = "문전수거"
                ),
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "없음",
                    targetRegionName = "없음",
                    disposalPlaceType = "거점수거"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    eupmyeondong = "양구읍"
                ),
                sigunguQuery = "양구군"
            )
        )

        val candidatesResult = result as RegionalGuideLookupResult.Candidates

        assertEquals(
            RegionalGuideCandidateLookupReason.FALLBACK_BECAUSE_DIRECT_MATCH_NOT_FOUND,
            candidatesResult.reason
        )
        assertEquals(2, candidatesResult.guides.size)
        assertEquals(listOf("문전수거", "거점수거"), candidatesResult.guides.map { guide -> guide.disposalPlaceType })
        assertEquals(listOf("양구읍", "양구읍"), candidatesResult.guides.map { guide -> guide.region.eupmyeondong })
    }

    @Test
    fun `직접 매칭 실패 후 같은 시군구 후보와 다른 읍면동 후보가 섞이면 시군구 후보만 반환한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "강릉시",
                    managementZoneName = "문전수거 지역",
                    targetRegionName = "문전수거 지역",
                    disposalPlaceType = "문전수거"
                ),
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "강릉시",
                    managementZoneName = "거점수거 지역",
                    targetRegionName = "거점수거 지역",
                    disposalPlaceType = "거점수거"
                ),
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "강릉시",
                    managementZoneName = "교1동",
                    targetRegionName = "교1동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "강원특별자치도",
                    sigungu = "강릉시",
                    eupmyeondong = "사천면"
                ),
                sigunguQuery = "강릉시"
            )
        )

        val candidates = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(2, candidates.size)
        assertEquals(listOf("문전수거", "거점수거"), candidates.map { guide -> guide.disposalPlaceType })
        assertEquals(listOf("문전수거 지역", "거점수거 지역"), candidates.map { guide -> guide.managementZoneName })
    }

    @Test
    fun `직접 매칭 실패 후 같은 시군구 권역 후보가 하나만 남으면 해당 후보를 선택한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "강릉시",
                    managementZoneName = "문전수거 지역",
                    targetRegionName = "문전수거 지역",
                    disposalPlaceType = "문전수거"
                ),
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "강릉시",
                    managementZoneName = "교1동",
                    targetRegionName = "교1동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "강원특별자치도",
                    sigungu = "강릉시",
                    eupmyeondong = "사천면"
                ),
                sigunguQuery = "강릉시"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("사천면", guide.region.eupmyeondong)
        assertEquals("문전수거 지역", guide.managementZoneName)
        assertEquals("문전수거", guide.disposalPlaceType)
    }

    @Test
    fun `직접 매칭이 실패해도 명시적인 다른 읍면동 후보는 같은 시군구 대체 후보로 노출하지 않는다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "강릉시",
                    managementZoneName = "교1동",
                    targetRegionName = "교1동"
                ),
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "강릉시",
                    managementZoneName = "홍제동",
                    targetRegionName = "홍제동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "강원특별자치도",
                    sigungu = "강릉시",
                    eupmyeondong = "사천면"
                ),
                sigunguQuery = "강릉시"
            )
        )

        assertEquals(RegionalGuideLookupResult.CandidateNotFound, result)
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
    fun `동일 후보 행의 최종수정일이 다르면 최신 행의 일정만 사용한다`() {
        val oldSchedule = RegionalWasteSchedule(
            wasteType = RegionalWasteType.GENERAL,
            disposalDays = "월"
        )
        val latestSchedule = RegionalWasteSchedule(
            wasteType = RegionalWasteType.GENERAL,
            disposalDays = "수"
        )

        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "없음",
                    targetRegionName = "없음",
                    disposalPlaceType = "거점수거",
                    schedules = listOf(oldSchedule),
                    sourceMetadata = RegionalGuideSourceMetadata(lastModifiedPoint = "20240101000000")
                ),
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "없음",
                    targetRegionName = "없음",
                    disposalPlaceType = "거점수거",
                    schedules = listOf(latestSchedule),
                    sourceMetadata = RegionalGuideSourceMetadata(lastModifiedPoint = "20240709000000")
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    eupmyeondong = "양구읍"
                ),
                sigunguQuery = "양구군"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals(listOf(latestSchedule), guide.schedules)
    }

    @Test
    fun `최종수정일이 없으면 데이터 기준일로 최신 행을 판단한다`() {
        val oldSchedule = RegionalWasteSchedule(
            wasteType = RegionalWasteType.FOOD,
            disposalDays = "화"
        )
        val latestSchedule = RegionalWasteSchedule(
            wasteType = RegionalWasteType.FOOD,
            disposalDays = "목"
        )

        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "없음",
                    targetRegionName = "없음",
                    disposalPlaceType = "거점수거",
                    schedules = listOf(oldSchedule),
                    sourceMetadata = RegionalGuideSourceMetadata(dataCriteriaDate = "20240101")
                ),
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "없음",
                    targetRegionName = "없음",
                    disposalPlaceType = "거점수거",
                    schedules = listOf(latestSchedule),
                    sourceMetadata = RegionalGuideSourceMetadata(dataCriteriaDate = "20240709")
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    eupmyeondong = "양구읍"
                ),
                sigunguQuery = "양구군"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals(listOf(latestSchedule), guide.schedules)
    }

    @Test
    fun `동일 후보 행의 관리번호와 미수거일이 달라도 최신 행의 값만 사용한다`() {
        val oldSchedule = RegionalWasteSchedule(
            wasteType = RegionalWasteType.GENERAL,
            disposalDays = "월, 화, 수, 목, 금, 토"
        )
        val latestSchedule = RegionalWasteSchedule(
            wasteType = RegionalWasteType.GENERAL,
            disposalDays = "일, 월, 화, 수, 목, 금, 토"
        )

        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "없음",
                    targetRegionName = "없음",
                    disposalPlaceType = "거점수거",
                    schedules = listOf(oldSchedule),
                    uncollectedDays = "설날, 추석, 일요일",
                    departmentName = "환경위생과",
                    departmentPhoneNumber = "033-480-2668",
                    sourceMetadata = RegionalGuideSourceMetadata(
                        managementNumber = "202000000000000001",
                        lastModifiedPoint = "20201217143200"
                    )
                ),
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "없음",
                    targetRegionName = "없음",
                    disposalPlaceType = "거점수거",
                    schedules = listOf(latestSchedule),
                    uncollectedDays = "설날, 추석, 일요일(음식물쓰레기)",
                    departmentName = "환경과",
                    departmentPhoneNumber = "033-480-7282",
                    sourceMetadata = RegionalGuideSourceMetadata(
                        managementNumber = "202000000000000002",
                        lastModifiedPoint = "20211014151500"
                    )
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    eupmyeondong = "양구읍"
                ),
                sigunguQuery = "양구군"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals(listOf(latestSchedule), guide.schedules)
        assertEquals("설날, 추석, 일요일(음식물쓰레기)", guide.uncollectedDays)
        assertEquals("환경과", guide.departmentName)
        assertEquals("033-480-7282", guide.departmentPhoneNumber)
    }

    @Test
    fun `날짜 파싱에 실패하면 최신 행을 임의 선택하지 않고 기존 병합 정책을 유지한다`() {
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
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "없음",
                    targetRegionName = "없음",
                    disposalPlaceType = "거점수거",
                    schedules = listOf(firstSchedule),
                    sourceMetadata = RegionalGuideSourceMetadata(
                        lastModifiedPoint = "invalid",
                        dataCriteriaDate = "20240101"
                    )
                ),
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "없음",
                    targetRegionName = "없음",
                    disposalPlaceType = "거점수거",
                    schedules = listOf(secondSchedule),
                    sourceMetadata = RegionalGuideSourceMetadata(
                        lastModifiedPoint = "20240709000000",
                        dataCriteriaDate = "20240709"
                    )
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    eupmyeondong = "양구읍"
                ),
                sigunguQuery = "양구군"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals(listOf(firstSchedule, secondSchedule), guide.schedules)
    }

    @Test
    fun `최종수정일이 일부 누락되면 데이터 기준일로 넘기지 않고 기존 병합 정책을 유지한다`() {
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
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "없음",
                    targetRegionName = "없음",
                    disposalPlaceType = "거점수거",
                    schedules = listOf(firstSchedule),
                    sourceMetadata = RegionalGuideSourceMetadata(
                        lastModifiedPoint = null,
                        dataCriteriaDate = "20240101"
                    )
                ),
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "없음",
                    targetRegionName = "없음",
                    disposalPlaceType = "거점수거",
                    schedules = listOf(secondSchedule),
                    sourceMetadata = RegionalGuideSourceMetadata(
                        lastModifiedPoint = "20240709000000",
                        dataCriteriaDate = "20240709"
                    )
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    eupmyeondong = "양구읍"
                ),
                sigunguQuery = "양구군"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals(listOf(firstSchedule, secondSchedule), guide.schedules)
    }

    @Test
    fun `존재하지 않는 날짜이면 최신 행을 임의 선택하지 않고 기존 병합 정책을 유지한다`() {
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
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "없음",
                    targetRegionName = "없음",
                    disposalPlaceType = "거점수거",
                    schedules = listOf(firstSchedule),
                    sourceMetadata = RegionalGuideSourceMetadata(lastModifiedPoint = "20240231000000")
                ),
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "없음",
                    targetRegionName = "없음",
                    disposalPlaceType = "거점수거",
                    schedules = listOf(secondSchedule),
                    sourceMetadata = RegionalGuideSourceMetadata(lastModifiedPoint = "20240709000000")
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    eupmyeondong = "양구읍"
                ),
                sigunguQuery = "양구군"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals(listOf(firstSchedule, secondSchedule), guide.schedules)
    }

    @Test
    fun `데이터 기준일이 같고 일정이 다르면 별도 후보로 유지한다`() {
        val firstSchedule = RegionalWasteSchedule(
            wasteType = RegionalWasteType.GENERAL,
            disposalDays = "일, 월, 수",
            disposalStartTime = "09:00",
            disposalEndTime = "18:00",
        )
        val secondSchedule = RegionalWasteSchedule(
            wasteType = RegionalWasteType.GENERAL,
            disposalDays = "일, 월, 수",
            disposalStartTime = "20:00",
            disposalEndTime = "06:00",
        )

        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "없음",
                    targetRegionName = "없음",
                    disposalPlaceType = "거점수거",
                    schedules = listOf(firstSchedule),
                    sourceMetadata = RegionalGuideSourceMetadata(
                        dataCriteriaDate = "20240709",
                    )
                ),
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "없음",
                    targetRegionName = "없음",
                    disposalPlaceType = "거점수거",
                    schedules = listOf(secondSchedule),
                    sourceMetadata = RegionalGuideSourceMetadata(
                        dataCriteriaDate = "20240709",
                    )
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    eupmyeondong = "양구읍"
                ),
                sigunguQuery = "양구군"
            )
        )

        val guides = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(
            listOf(listOf(firstSchedule), listOf(secondSchedule)),
            guides.map { guide -> guide.schedules },
        )
    }

    @Test
    fun `최종수정일이 같고 일정이 같으면 하나의 후보로 정리한다`() {
        val schedule = RegionalWasteSchedule(
            wasteType = RegionalWasteType.GENERAL,
            disposalDays = "월, 수, 금",
            disposalStartTime = "09:00",
            disposalEndTime = "18:00",
        )

        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "경기도",
                    sigungu = "양평군",
                    managementZoneName = "양평읍",
                    targetRegionName = "양근5리",
                    disposalPlaceType = "거점수거",
                    schedules = listOf(schedule),
                    uncollectedDays = "없음",
                    sourceMetadata = RegionalGuideSourceMetadata(
                        managementNumber = "202541700000400153",
                        lastModifiedPoint = "20240709105039",
                    ),
                ),
                regionalDisposalGuide(
                    sido = "경기도",
                    sigungu = "양평군",
                    managementZoneName = "양평읍",
                    targetRegionName = "양근5리",
                    disposalPlaceType = "거점수거",
                    schedules = listOf(schedule),
                    uncollectedDays = "화, 금, 토",
                    sourceMetadata = RegionalGuideSourceMetadata(
                        managementNumber = "202541700000400121",
                        lastModifiedPoint = "20240709105039",
                    ),
                ),
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "경기도",
                    sigungu = "양평군",
                    eupmyeondong = "양평읍",
                ),
                sigunguQuery = "양평군",
            ),
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals(listOf(schedule), guide.schedules)
    }

    @Test
    fun `동률인 최신 일정 묶음에서 같은 일정만 하나로 정리한다`() {
        val daytimeSchedule = RegionalWasteSchedule(
            wasteType = RegionalWasteType.GENERAL,
            disposalDays = "일, 월, 수",
            disposalStartTime = "09:00",
            disposalEndTime = "18:00",
        )
        val nighttimeSchedule = daytimeSchedule.copy(
            disposalStartTime = "20:00",
            disposalEndTime = "06:00",
        )

        fun 후보행(
            schedule: RegionalWasteSchedule,
            managementNumber: String,
            lastModifiedPoint: String,
        ): RegionalDisposalGuide =
            regionalDisposalGuide(
                sido = "경기도",
                sigungu = "양평군",
                managementZoneName = "양평읍",
                targetRegionName = "양근5리",
                disposalPlaceType = "거점수거",
                schedules = listOf(schedule),
                sourceMetadata = RegionalGuideSourceMetadata(
                    managementNumber = managementNumber,
                    lastModifiedPoint = lastModifiedPoint,
                ),
            )

        val result = useCase(
            candidates = listOf(
                후보행(daytimeSchedule, "202541700000400153", "20240709105039"),
                후보행(daytimeSchedule, "202541700000400121", "20240709104936"),
                후보행(nighttimeSchedule, "202541700000400147", "20240709105039"),
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "경기도",
                    sigungu = "양평군",
                    eupmyeondong = "양평읍",
                ),
                sigunguQuery = "양평군",
            ),
        )

        val guides = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(
            listOf(listOf(daytimeSchedule), listOf(nighttimeSchedule)),
            guides.map { guide -> guide.schedules },
        )
        assertEquals(
            listOf("202541700000400153", "202541700000400147"),
            guides.map { guide -> guide.sourceMetadata?.managementNumber },
        )
    }

    @Test
    fun `최종수정일이 9자리에서 13자리이면 최신 행을 임의 선택하지 않고 기존 병합 정책을 유지한다`() {
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
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "없음",
                    targetRegionName = "없음",
                    disposalPlaceType = "거점수거",
                    schedules = listOf(firstSchedule),
                    sourceMetadata = RegionalGuideSourceMetadata(lastModifiedPoint = "202407091")
                ),
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "없음",
                    targetRegionName = "없음",
                    disposalPlaceType = "거점수거",
                    schedules = listOf(secondSchedule),
                    sourceMetadata = RegionalGuideSourceMetadata(lastModifiedPoint = "2024070912300")
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    eupmyeondong = "양구읍"
                ),
                sigunguQuery = "양구군"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals(listOf(firstSchedule, secondSchedule), guide.schedules)
    }

    @Test
    fun `최신성 판단 실패 후 후보 재필터링 시 최신 행을 다시 선택하지 않는다`() {
        val firstSchedule = RegionalWasteSchedule(
            wasteType = RegionalWasteType.GENERAL,
            disposalDays = "월"
        )
        val invalidSchedule = RegionalWasteSchedule(
            wasteType = RegionalWasteType.FOOD,
            disposalDays = "화"
        )
        val latestSchedule = RegionalWasteSchedule(
            wasteType = RegionalWasteType.RECYCLABLE,
            disposalDays = "수"
        )

        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "양구읍",
                    targetRegionName = "일부지역",
                    disposalPlaceType = "거점수거",
                    schedules = listOf(firstSchedule),
                    uncollectedDays = "설날",
                    sourceMetadata = RegionalGuideSourceMetadata(lastModifiedPoint = "20240101000000")
                ),
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "양구읍",
                    targetRegionName = "일부지역",
                    disposalPlaceType = "거점수거",
                    schedules = listOf(invalidSchedule),
                    uncollectedDays = "설날",
                    sourceMetadata = RegionalGuideSourceMetadata(lastModifiedPoint = "invalid")
                ),
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "양구읍",
                    targetRegionName = "일부지역",
                    disposalPlaceType = "거점수거",
                    schedules = listOf(latestSchedule),
                    uncollectedDays = "추석",
                    sourceMetadata = RegionalGuideSourceMetadata(lastModifiedPoint = "20240709000000")
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    eupmyeondong = "양구읍"
                ),
                sigunguQuery = "양구군"
            )
        )

        val candidates = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(2, candidates.size)
        assertEquals(
            listOf(
                listOf(firstSchedule, invalidSchedule),
                listOf(latestSchedule)
            ),
            candidates.map { guide -> guide.schedules }
        )
    }

    @Test
    fun `후보 식별값이 다르면 날짜가 있어도 최신성 비교 대상으로 묶지 않는다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "없음",
                    targetRegionName = "없음",
                    disposalPlaceType = "문전수거",
                    sourceMetadata = RegionalGuideSourceMetadata(lastModifiedPoint = "20240101000000")
                ),
                regionalDisposalGuide(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    managementZoneName = "없음",
                    targetRegionName = "없음",
                    disposalPlaceType = "거점수거",
                    sourceMetadata = RegionalGuideSourceMetadata(lastModifiedPoint = "20240709000000")
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "강원특별자치도",
                    sigungu = "양구군",
                    eupmyeondong = "양구읍"
                ),
                sigunguQuery = "양구군"
            )
        )

        val candidates = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(2, candidates.size)
        assertEquals(listOf("문전수거", "거점수거"), candidates.map { guide -> guide.disposalPlaceType })
    }

    @Test
    fun `후보명이 같아도 상세 필드가 다르면 별도 후보로 유지한다`() {
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

    @Test
    fun `가이드 식별 필드가 다른 적용 행은 후보 목록으로 유지한다`() {
        val recycleSchedule = RegionalWasteSchedule(
            wasteType = RegionalWasteType.RECYCLABLE,
            disposalDays = "월"
        )
        val foodSchedule = RegionalWasteSchedule(
            wasteType = RegionalWasteType.FOOD,
            disposalDays = "화"
        )
        val largeItemSchedule = RegionalWasteSchedule(
            wasteType = RegionalWasteType.LARGE_ITEM,
            disposalDays = "수"
        )

        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    managementZoneName = "재활용가능폐기물 수거(성서권)",
                    targetRegionName = "두류동+감삼동+신당동",
                    disposalPlaceType = "재활용가능폐기물 수거",
                    schedules = listOf(recycleSchedule)
                ),
                regionalDisposalGuide(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    managementZoneName = "음식물류폐기물 수거(구직영)",
                    targetRegionName = "성당동+두류동+본리동+감삼동+송현동+본동",
                    disposalPlaceType = "음식물류폐기물 수거",
                    schedules = listOf(foodSchedule)
                ),
                regionalDisposalGuide(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    managementZoneName = "대형폐기물 수거(성서권)",
                    targetRegionName = "성당동+두류동+본리동+감삼동+죽전동+장기동+용산동+이곡동+신당동",
                    disposalPlaceType = "대형폐기물 수거",
                    schedules = listOf(largeItemSchedule)
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    eupmyeondong = "두류1.2동"
                ),
                sigunguQuery = "달서구"
            )
        )

        val guides = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(3, guides.size)
        assertEquals(
            listOf(
                "재활용가능폐기물 수거(성서권)",
                "음식물류폐기물 수거(구직영)",
                "대형폐기물 수거(성서권)",
            ),
            guides.map { guide -> guide.managementZoneName }
        )
        assertEquals(
            listOf(recycleSchedule, foodSchedule, largeItemSchedule),
            guides.map { guide -> guide.schedules.single() }
        )
    }

    @Test
    fun `선택 읍면동에 적용되는 행이라도 같은 배출 유형이 중복되면 후보 목록을 유지한다`() {
        val firstRecycleSchedule = RegionalWasteSchedule(
            wasteType = RegionalWasteType.RECYCLABLE,
            disposalDays = "월"
        )
        val secondRecycleSchedule = RegionalWasteSchedule(
            wasteType = RegionalWasteType.RECYCLABLE,
            disposalDays = "목"
        )

        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    managementZoneName = "재활용가능폐기물 수거 A권역",
                    targetRegionName = "두류동+감삼동",
                    schedules = listOf(firstRecycleSchedule)
                ),
                regionalDisposalGuide(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    managementZoneName = "재활용가능폐기물 수거 B권역",
                    targetRegionName = "두류동+성당동",
                    schedules = listOf(secondRecycleSchedule)
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    eupmyeondong = "두류1.2동"
                ),
                sigunguQuery = "달서구"
            )
        )

        val candidates = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(2, candidates.size)
        assertEquals(
            listOf("재활용가능폐기물 수거 A권역", "재활용가능폐기물 수거 B권역"),
            candidates.map { guide -> guide.managementZoneName }
        )
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
    fun `조회 키와 일치하는 시군구 후보가 없으면 후보 없음으로 반환한다`() {
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
    fun `우선 대상지역이 있으면 해당 후보를 우선 선택한다`() {
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
    fun `우선 대상지역이 후보에 없으면 임의 후보를 선택하지 않는다`() {
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

class SelectRegionalGuideFavoriteCompatibilityUseCaseTest {

    private val useCase = SelectRegionalGuideCandidateUseCase()

    @Test
    fun `즐겨찾기 키의 시도 별칭이 달라도 호환 후보를 복원한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "전라남도",
                    sigungu = "나주시",
                    targetRegionName = "노안면",
                    managementZoneName = "노안면",
                    eupmyeondong = "노안면"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "전남광주통합특별시",
                    sigungu = "나주시",
                    eupmyeondong = "노안면"
                ),
                sigunguQuery = "나주시"
            ),
            favoriteKey = RegionalGuideFavoriteKey(
                sido = "전남광주통합특별시",
                sigungu = "나주시",
                eupmyeondong = "노안면",
                targetRegionName = "노안면",
                managementZoneName = "노안면",
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("전남광주통합특별시", guide.region.sido)
        assertEquals("나주시", guide.region.sigungu)
        assertEquals("노안면", guide.region.eupmyeondong)
        assertEquals("노안면", guide.targetRegionName)
    }

    @Test
    fun `즐겨찾기 키와 호환되는 후보가 여러 개면 첫 후보를 임의 선택하지 않는다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    managementZoneName = "노은2동",
                    targetRegionName = "반석동 일부지역",
                    eupmyeondong = "반석동"
                ),
                regionalDisposalGuide(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    managementZoneName = "노은3동",
                    targetRegionName = "반석동 일부지역",
                    eupmyeondong = "반석동"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "대전광역시",
                    sigungu = "유성구",
                    eupmyeondong = "반석동"
                ),
                sigunguQuery = "유성구"
            ),
            favoriteKey = RegionalGuideFavoriteKey(
                sido = "대전광역시",
                sigungu = "유성구",
                eupmyeondong = "반석동",
                targetRegionName = "반석동 일부지역",
                managementZoneName = null,
            )
        )

        val candidates = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(2, candidates.size)
        assertEquals("노은2동", candidates[0].managementZoneName)
        assertEquals("노은3동", candidates[1].managementZoneName)
    }

    @Test
    fun `가이드 식별 필드가 다른 적용 행이 있어도 즐겨찾기 원본 후보를 복원한다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    managementZoneName = "재활용가능폐기물 수거(성서권)",
                    targetRegionName = "두류동+감삼동+신당동",
                    eupmyeondong = "두류1.2동",
                ),
                regionalDisposalGuide(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    managementZoneName = "음식물류폐기물 수거(구직영)",
                    targetRegionName = "성당동+두류동+본리동+감삼동+송현동+본동",
                    eupmyeondong = "두류1.2동",
                ),
            ),
            query = regionalGuideQuery(
                displayRegion = Region(
                    sido = "대구광역시",
                    sigungu = "달서구",
                    eupmyeondong = "두류1.2동",
                ),
                sigunguQuery = "달서구",
            ),
            favoriteKey = RegionalGuideFavoriteKey(
                sido = "대구광역시",
                sigungu = "달서구",
                eupmyeondong = "두류1.2동",
                targetRegionName = "두류동+감삼동+신당동",
                managementZoneName = "재활용가능폐기물 수거(성서권)",
            ),
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("재활용가능폐기물 수거(성서권)", guide.managementZoneName)
        assertEquals("두류동+감삼동+신당동", guide.targetRegionName)
    }

    @Test
    fun `즐겨찾기 키와 호환되는 후보가 없으면 임의 후보를 선택하지 않는다`() {
        val result = useCase(
            candidates = listOf(
                regionalDisposalGuide(
                    sido = "인천광역시",
                    sigungu = "서해구",
                    targetRegionName = "서해구 전체"
                ),
                regionalDisposalGuide(
                    sido = "인천광역시",
                    sigungu = "검단구",
                    targetRegionName = "검단구 전체"
                )
            ),
            query = regionalGuideQuery(
                displayRegion = Region(sido = "인천광역시", sigungu = "서구"),
                sigunguQuery = "서구"
            ),
            favoriteKey = RegionalGuideFavoriteKey(
                sido = "인천광역시",
                sigungu = "서구",
                eupmyeondong = null,
                targetRegionName = "서구 전체",
                managementZoneName = null,
            )
        )

        assertEquals(RegionalGuideLookupResult.CandidateNotFound, result)
    }
}
