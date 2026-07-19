package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.local.dto.AdministrativeRegionDto
import com.team.yeogibeoryeo.data.region.local.dto.LegalAdminDongMappingDto
import com.team.yeogibeoryeo.data.region.local.dto.RegionalGuideAvailabilityDto
import com.team.yeogibeoryeo.data.region.local.dto.RegionalGuideRegionDto
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideRegionKeyNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class RegionOptionsMapperTest {

    @Test
    fun `시군구 옵션은 지역 가이드 정보 지역을 사용한다`() {
        val options = RegionOptionsMapper.getSigunguOptions(
            regionalGuideRegions = listOf(
                RegionalGuideRegionDto(
                    sidoName = "경기도",
                    sigunguName = "성남시"
                ),
                RegionalGuideRegionDto(
                    sidoName = "경기도",
                    sigunguName = "수원시"
                )
            ),
            sido = "경기도"
        )

        assertEquals(listOf("성남시", "수원시"), options)
    }

    @Test
    fun `시군구 옵션은 정보 지역과 중복된 행정구역을 노출하지 않는다`() {
        val options = RegionOptionsMapper.getSigunguOptions(
            regionalGuideRegions = listOf(
                RegionalGuideRegionDto(
                    sidoName = "경기도",
                    sigunguName = "성남시"
                ),
                RegionalGuideRegionDto(
                    sidoName = "경기도",
                    sigunguName = "수원시"
                )
            ),
            sido = "경기도"
        )

        assertEquals(false, "성남시 중원구" in options)
        assertEquals(false, "수원시 장안구" in options)
    }

    @Test
    fun `세종 시군구 옵션은 없음 대신 세종 이름으로 표시한다`() {
        val options = RegionOptionsMapper.getSigunguOptions(
            regionalGuideRegions = listOf(
                RegionalGuideRegionDto(
                    sidoName = "세종특별자치시",
                    sigunguName = "없음"
                )
            ),
            sido = "세종특별자치시"
        )

        assertEquals(listOf("세종특별자치시"), options)
    }

    @Test
    fun `통합 시도 행정 자료는 광주와 전남 지역 가이드 선택지로 변환한다`() {
        val regionalGuideRegions = listOf(
            RegionalGuideRegionDto(
                sidoName = "전남광주통합특별시",
                sigunguName = "동구"
            ),
            RegionalGuideRegionDto(
                sidoName = "전남광주통합특별시",
                sigunguName = "나주시"
            )
        )
        val administrativeRegions = listOf(
            administrativeRegion(
                sidoName = "전남광주통합특별시",
                sigunguName = "동구",
                eupmyeondongName = "충장동"
            ),
            administrativeRegion(
                sidoName = "전남광주통합특별시",
                sigunguName = "나주시",
                eupmyeondongName = "빛가람동"
            )
        )

        assertEquals(
            listOf("동구"),
            RegionOptionsMapper.getRegionalGuideSigunguOptions(
                regionalGuideRegions = regionalGuideRegions,
                sido = "광주광역시"
            )
        )
        assertEquals(
            listOf("나주시"),
            RegionOptionsMapper.getRegionalGuideSigunguOptions(
                regionalGuideRegions = regionalGuideRegions,
                sido = "전라남도"
            )
        )
        assertEquals(
            listOf("충장동"),
            RegionOptionsMapper.getRegionalGuideEupmyeondongOptions(
                administrativeRegions = administrativeRegions,
                sido = "광주광역시",
                sigungu = "동구"
            )
        )
        assertEquals(
            listOf("빛가람동"),
            RegionOptionsMapper.getRegionalGuideEupmyeondongOptions(
                administrativeRegions = administrativeRegions,
                sido = "전라남도",
                sigungu = "나주시"
            )
        )
    }

    @Test
    fun `읍면동 옵션은 행정구역 시군구를 정보 시군구로 매핑한다`() {
        val options = RegionOptionsMapper.getEupmyeondongOptions(
            administrativeRegions = listOf(
                administrativeRegion(
                    sidoName = "경기도",
                    sigunguName = "수원시 장안구",
                    eupmyeondongName = "파장동"
                ),
                administrativeRegion(
                    sidoName = "경기도",
                    sigunguName = "수원시 영통구",
                    eupmyeondongName = "망포동"
                ),
                administrativeRegion(
                    sidoName = "경기도",
                    sigunguName = "성남시 중원구",
                    eupmyeondongName = "중앙동"
                )
            ),
            sido = "경기도",
            sigungu = "수원시"
        )

        assertEquals(listOf("망포동", "파장동"), options)
    }

    @Test
    fun `읍면동 옵션은 세종 행정구역을 세종 표시 시군구로 매핑한다`() {
        val options = RegionOptionsMapper.getEupmyeondongOptions(
            administrativeRegions = listOf(
                administrativeRegion(
                    sidoName = "세종특별자치시",
                    sigunguName = "",
                    eupmyeondongName = "한솔동"
                )
            ),
            sido = "세종특별자치시",
            sigungu = "세종특별자치시"
        )

        assertEquals(listOf("한솔동"), options)
    }

    @Test
    fun `시군구 키워드 검색은 행정구역보다 정보 제공 도시 지역을 우선한다`() {
        val regions = RegionOptionsMapper.findSigunguRegions(
            administrativeRegions = listOf(
                administrativeRegion(
                    sidoName = "경기도",
                    sigunguName = "성남시 중원구",
                    eupmyeondongName = "중앙동"
                ),
                administrativeRegion(
                    sidoName = "경기도",
                    sigunguName = "성남시 분당구",
                    eupmyeondongName = "분당동"
                )
            ),
            regionalGuideRegions = listOf(
                RegionalGuideRegionDto(
                    sidoName = "경기도",
                    sigunguName = "성남시"
                )
            ),
            keyword = "성남시"
        )

        assertEquals(
            listOf(
                Region(
                    sido = "경기도",
                    sigungu = "성남시"
                )
            ),
            regions
        )
    }

    @Test
    fun `시군구 키워드 검색은 정보 지역이 없으면 행정구역으로 대체한다`() {
        val regions = RegionOptionsMapper.findSigunguRegions(
            administrativeRegions = listOf(
                administrativeRegion(
                    sidoName = "경기도",
                    sigunguName = "성남시 중원구",
                    eupmyeondongName = "중앙동"
                ),
                administrativeRegion(
                    sidoName = "경기도",
                    sigunguName = "성남시 중원구",
                    eupmyeondongName = "성남동"
                )
            ),
            regionalGuideRegions = listOf(
                RegionalGuideRegionDto(
                    sidoName = "경기도",
                    sigunguName = "성남시"
                )
            ),
            keyword = "중원구"
        )

        assertEquals(
            listOf(
                Region(
                    sido = "경기도",
                    sigungu = "성남시 중원구",
                    eupmyeondong = null
                )
            ),
            regions
        )
    }

    @Test
    fun `시군구 키워드 검색은 행정구역을 상위 정보 조회 키로 매핑한다`() {
        val regions = RegionOptionsMapper.findSigunguRegions(
            administrativeRegions = listOf(
                administrativeRegion(
                    sidoName = "경기도",
                    sigunguName = "성남시 분당구",
                    eupmyeondongName = "분당동"
                )
            ),
            regionalGuideRegions = listOf(
                RegionalGuideRegionDto(
                    sidoName = "경기도",
                    sigunguName = "성남시"
                )
            ),
            keyword = "분당구"
        )

        assertEquals(
            "성남시",
            RegionalGuideRegionKeyNormalizer.normalizeSigungu(regions.first().sigungu.orEmpty())
        )
    }

    @Test
    fun `지역 정규화는 행정구역을 정보 제공 시군구로 매핑한다`() {
        val region = RegionOptionsMapper.normalizeRegionForRegionalGuide(
            region = Region(
                sido = "경기도",
                sigungu = "성남시 중원구",
                eupmyeondong = "중앙동"
            ),
            regionalGuideRegions = listOf(
                RegionalGuideRegionDto(
                    sidoName = "경기도",
                    sigunguName = "성남시"
                )
            )
        )

        assertEquals(
            Region(
                sido = "경기도",
                sigungu = "성남시",
                eupmyeondong = "중앙동"
            ),
            region
        )
    }

    @Test
    fun `지역 정규화는 시 접미사 없이 공식 도시명을 정보 키로 매핑한다`() {
        val region = RegionOptionsMapper.normalizeRegionForRegionalGuide(
            region = Region(
                sido = "경기도",
                sigungu = "남양주시",
                eupmyeondong = "다산동"
            ),
            regionalGuideRegions = listOf(
                RegionalGuideRegionDto(
                    sidoName = "경기도",
                    sigunguName = "남양주"
                )
            )
        )

        assertEquals(
            Region(
                sido = "경기도",
                sigungu = "남양주",
                eupmyeondong = "다산동"
            ),
            region
        )
    }

    @Test
    fun `읍면동 옵션은 시 접미사 없이 공식 도시명을 정보 키로 매핑한다`() {
        val options = RegionOptionsMapper.getEupmyeondongOptions(
            administrativeRegions = listOf(
                administrativeRegion(
                    sidoName = "경기도",
                    sigunguName = "남양주시",
                    eupmyeondongName = "다산동"
                ),
                administrativeRegion(
                    sidoName = "경기도",
                    sigunguName = "남양주시",
                    eupmyeondongName = "별내동"
                )
            ),
            sido = "경기도",
            sigungu = "남양주"
        )

        assertEquals(listOf("다산동", "별내동"), options)
    }

    @Test
    fun `읍면동 옵션은 가나다순과 숫자 자연 순서로 정렬한다`() {
        val options = RegionOptionsMapper.getEupmyeondongOptions(
            administrativeRegions = listOf(
                administrativeRegion(
                    sidoName = "경상북도",
                    sigunguName = "성주군",
                    eupmyeondongName = "월곡10리"
                ),
                administrativeRegion(
                    sidoName = "경상북도",
                    sigunguName = "성주군",
                    eupmyeondongName = "고산리"
                ),
                administrativeRegion(
                    sidoName = "경상북도",
                    sigunguName = "성주군",
                    eupmyeondongName = "월곡2리"
                ),
                administrativeRegion(
                    sidoName = "경상북도",
                    sigunguName = "성주군",
                    eupmyeondongName = "월곡1리"
                ),
                administrativeRegion(
                    sidoName = "경상북도",
                    sigunguName = "성주군",
                    eupmyeondongName = "문덕2리"
                )
            ),
            sido = "경상북도",
            sigungu = "성주군"
        )

        assertEquals(
            listOf("고산리", "문덕2리", "월곡1리", "월곡2리", "월곡10리"),
            options
        )
    }

    @Test
    fun `숫자로 시작하는 동 이름도 자연 순서로 정렬한다`() {
        val options = RegionOptionsMapper.getEupmyeondongOptions(
            administrativeRegions = listOf(
                administrativeRegion(
                    sidoName = "서울특별시",
                    sigunguName = "중구",
                    eupmyeondongName = "10동"
                ),
                administrativeRegion(
                    sidoName = "서울특별시",
                    sigunguName = "중구",
                    eupmyeondongName = "2동"
                ),
                administrativeRegion(
                    sidoName = "서울특별시",
                    sigunguName = "중구",
                    eupmyeondongName = "1동"
                )
            ),
            sido = "서울특별시",
            sigungu = "중구"
        )

        assertEquals(listOf("1동", "2동", "10동"), options)
    }

    @Test
    fun `시군구 키워드 검색은 시 접미사 없이 공식 도시 키워드를 정보 키로 매핑한다`() {
        val regions = RegionOptionsMapper.findSigunguRegions(
            administrativeRegions = listOf(
                administrativeRegion(
                    sidoName = "경기도",
                    sigunguName = "동두천시",
                    eupmyeondongName = "생연동"
                )
            ),
            regionalGuideRegions = listOf(
                RegionalGuideRegionDto(
                    sidoName = "경기도",
                    sigunguName = "동두천"
                )
            ),
            keyword = "동두천시"
        )

        assertEquals(
            listOf(
                Region(
                    sido = "경기도",
                    sigungu = "동두천"
                )
            ),
            regions
        )
    }

    @Test
    fun `정보 제공 지역이 없으면 지역 정규화는 원본 시군구를 유지한다`() {
        val region = RegionOptionsMapper.normalizeRegionForRegionalGuide(
            region = Region(
                sido = "경기도",
                sigungu = "중원구",
                eupmyeondong = null
            ),
            regionalGuideRegions = listOf(
                RegionalGuideRegionDto(
                    sidoName = "경기도",
                    sigunguName = "성남시"
                )
            )
        )

        assertEquals(
            Region(
                sido = "경기도",
                sigungu = "중원구",
                eupmyeondong = null
            ),
            region
        )
    }

    @Test
    fun `세종 지역 정규화는 세종 표시 시군구를 사용한다`() {
        val region = RegionOptionsMapper.normalizeRegionForRegionalGuide(
            region = Region(
                sido = "세종특별자치시",
                sigungu = null,
                eupmyeondong = "한솔동"
            ),
            regionalGuideRegions = listOf(
                RegionalGuideRegionDto(
                    sidoName = "세종특별자치시",
                    sigunguName = "없음"
                )
            )
        )

        assertEquals(
            Region(
                sido = "세종특별자치시",
                sigungu = "세종특별자치시",
                eupmyeondong = "한솔동"
            ),
            region
        )
    }

    @Test
    fun `법정동 조회는 시도 시군구 법정동 정확 일치 기준으로 매핑된 행정동을 반환한다`() {
        val regions = RegionOptionsMapper.findAdminDongCandidatesForLegalDong(
            mappings = listOf(
                legalAdminMapping(
                    sidoName = "서울특별시",
                    sigunguName = "노원구",
                    legalDongName = "하계동",
                    adminDongName = "하계1동"
                ),
                legalAdminMapping(
                    sidoName = "서울특별시",
                    sigunguName = "노원구",
                    legalDongName = "하계동",
                    adminDongName = "하계2동"
                ),
                legalAdminMapping(
                    sidoName = "서울특별시",
                    sigunguName = "중구",
                    legalDongName = "하계동",
                    adminDongName = "다른동"
                ),
                legalAdminMapping(
                    sidoName = "부산광역시",
                    sigunguName = "사하구",
                    legalDongName = "하계동",
                    adminDongName = "부산하계동"
                )
            ),
            region = Region(
                sido = "서울특별시",
                sigungu = "노원구",
                eupmyeondong = "하계동"
            )
        )

        assertEquals(
            listOf(
                Region(sido = "서울특별시", sigungu = "노원구", eupmyeondong = "하계1동"),
                Region(sido = "서울특별시", sigungu = "노원구", eupmyeondong = "하계2동")
            ),
            regions
        )
    }

    @Test
    fun `법정동 조회는 접미사 추론 없이 일대다와 다대일 관계를 유지한다`() {
        val mappings = listOf(
            legalAdminMapping(
                sidoName = "서울특별시",
                sigunguName = "노원구",
                legalDongName = "하계동",
                adminDongName = "하계1동"
            ),
            legalAdminMapping(
                sidoName = "서울특별시",
                sigunguName = "노원구",
                legalDongName = "하계동",
                adminDongName = "하계2동"
            ),
            legalAdminMapping(
                sidoName = "서울특별시",
                sigunguName = "노원구",
                legalDongName = "공릉동",
                adminDongName = "하계1동"
            )
        )

        val haggyeRegions = RegionOptionsMapper.findAdminDongCandidatesForLegalDong(
            mappings = mappings,
            region = Region(sido = "서울특별시", sigungu = "노원구", eupmyeondong = "하계동")
        )
        val gongneungRegions = RegionOptionsMapper.findAdminDongCandidatesForLegalDong(
            mappings = mappings,
            region = Region(sido = "서울특별시", sigungu = "노원구", eupmyeondong = "공릉동")
        )

        assertEquals(2, haggyeRegions.size)
        assertEquals(
            listOf(Region(sido = "서울특별시", sigungu = "노원구", eupmyeondong = "하계1동")),
            gongneungRegions
        )
    }

    @Test
    fun `법정동 조회는 정확 일치가 없으면 빈 목록을 반환한다`() {
        val regions = RegionOptionsMapper.findAdminDongCandidatesForLegalDong(
            mappings = listOf(
                legalAdminMapping(
                    sidoName = "서울특별시",
                    sigunguName = "노원구",
                    legalDongName = "하계동",
                    adminDongName = "하계1동"
                )
            ),
            region = Region(
                sido = "서울특별시",
                sigungu = "노원구",
                eupmyeondong = "하계"
            )
        )

        assertEquals(emptyList<Region>(), regions)
    }

    @Test
    fun `읍면동 조회는 행정동 접미사가 있는 법정동 후보를 포함한다`() {
        val regions = RegionOptionsMapper.findEupmyeondongRegions(
            administrativeRegions = listOf(
                administrativeRegion(
                    sidoName = "전라남도",
                    sigunguName = "광양시",
                    eupmyeondongName = "금호동"
                )
            ),
            legalAdminDongMappings = listOf(
                legalAdminMapping(
                    sidoName = "광주광역시",
                    sigunguName = "서구",
                    legalDongName = "금호동",
                    adminDongName = "금호1동"
                ),
                legalAdminMapping(
                    sidoName = "광주광역시",
                    sigunguName = "서구",
                    legalDongName = "금호동",
                    adminDongName = "금호2동"
                ),
                legalAdminMapping(
                    sidoName = "서울특별시",
                    sigunguName = "성동구",
                    legalDongName = "금호동1가",
                    adminDongName = "금호1가동"
                ),
                legalAdminMapping(
                    sidoName = "전라남도",
                    sigunguName = "보성군",
                    legalDongName = "금호리",
                    adminDongName = "노동면"
                )
            ),
            keyword = "금호동"
        )

        assertEquals(
            listOf(
                Region(sido = "광주광역시", sigungu = "서구", eupmyeondong = "금호동"),
                Region(sido = "서울특별시", sigungu = "성동구", eupmyeondong = "금호동"),
                Region(sido = "전라남도", sigungu = "광양시", eupmyeondong = "금호동")
            ),
            regions
        )
    }

    @Test
    fun `법정동 후보 생성 시 법정 행정 시군구 코드가 다른 후보는 제외한다`() {
        val regions = RegionOptionsMapper.findEupmyeondongRegions(
            administrativeRegions = emptyList(),
            legalAdminDongMappings = listOf(
                legalAdminMapping(
                    legalCode = "4117110100",
                    sidoName = "경기도",
                    sigunguName = "안양시 만안구",
                    legalDongName = "안양동",
                    adminCode = "4117151000",
                    adminDongName = "안양1동"
                ),
                legalAdminMapping(
                    legalCode = "4117110100",
                    sidoName = "경기도",
                    sigunguName = "안양시 동안구",
                    legalDongName = "안양동",
                    adminCode = "4117351000",
                    adminDongName = "비산1동"
                )
            ),
            keyword = "안양동"
        )

        assertEquals(
            listOf(
                Region(sido = "경기도", sigungu = "안양시 만안구", eupmyeondong = "안양동")
            ),
            regions
        )
    }

    @Test
    fun `접미사 없는 동 검색어도 행정동 후보로 반환한다`() {
        val regions = RegionOptionsMapper.findEupmyeondongRegions(
            administrativeRegions = listOf(
                administrativeRegion(
                    sidoName = "부산광역시",
                    sigunguName = "사하구",
                    eupmyeondongName = "괴정제1동"
                ),
                administrativeRegion(
                    sidoName = "부산광역시",
                    sigunguName = "사하구",
                    eupmyeondongName = "괴정제2동"
                )
            ),
            legalAdminDongMappings = listOf(
                legalAdminMapping(
                    sidoName = "부산광역시",
                    sigunguName = "사하구",
                    legalDongName = "괴정동",
                    adminDongName = "괴정제1동"
                )
            ),
            keyword = "괴정"
        )

        assertEquals(
            listOf(
                Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정제1동"),
                Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정제2동")
            ),
            regions
        )
    }

    @Test
    fun `번호가 생략된 동 검색어는 번호 행정동 후보를 반환한다`() {
        val regions = RegionOptionsMapper.findRegionalGuideEupmyeondongRegions(
            administrativeRegions = listOf(
                administrativeRegion(
                    sidoName = "대전광역시",
                    sigunguName = "유성구",
                    eupmyeondongName = "노은1동"
                ),
                administrativeRegion(
                    sidoName = "대전광역시",
                    sigunguName = "유성구",
                    eupmyeondongName = "노은2동"
                ),
                administrativeRegion(
                    sidoName = "대전광역시",
                    sigunguName = "유성구",
                    eupmyeondongName = "노은3동"
                ),
                administrativeRegion(
                    sidoName = "대전광역시",
                    sigunguName = "유성구",
                    eupmyeondongName = "온천1동"
                )
            ),
            legalAdminDongMappings = listOf(
                legalAdminMapping(
                    sidoName = "대전광역시",
                    sigunguName = "유성구",
                    legalDongName = "노은동",
                    adminDongName = "노은1동"
                ),
                legalAdminMapping(
                    sidoName = "대전광역시",
                    sigunguName = "유성구",
                    legalDongName = "노은동",
                    adminDongName = "온천2동"
                )
            ),
            keyword = "노은동"
        )

        assertEquals(
            listOf(
                Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "노은1동"),
                Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "노은2동"),
                Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "노은3동")
            ),
            regions
        )
    }

    @Test
    fun `제 번호가 붙은 행정동도 번호가 생략된 동 검색어로 찾을 수 있다`() {
        val regions = RegionOptionsMapper.findRegionalGuideEupmyeondongRegions(
            administrativeRegions = listOf(
                administrativeRegion(
                    sidoName = "부산광역시",
                    sigunguName = "사하구",
                    eupmyeondongName = "괴정제1동"
                ),
                administrativeRegion(
                    sidoName = "부산광역시",
                    sigunguName = "사하구",
                    eupmyeondongName = "괴정제2동"
                )
            ),
            legalAdminDongMappings = emptyList(),
            keyword = "괴정동"
        )

        assertEquals(
            listOf(
                Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정제1동"),
                Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정제2동")
            ),
            regions
        )
    }

    @Test
    fun `사천 검색은 제공 가능 시군구인 경상남도 사천시 후보를 반환한다`() {
        val regions = RegionOptionsMapper.findSigunguRegions(
            administrativeRegions = emptyList(),
            regionalGuideRegions = listOf(
                RegionalGuideRegionDto(
                    sidoName = "경상남도",
                    sigunguName = "사천시"
                )
            ),
            keyword = "사천"
        )

        assertEquals(
            listOf(Region(sido = "경상남도", sigungu = "사천시")),
            regions
        )
    }

    @Test
    fun `다른 지역에 정확한 행정동이 있어도 같은 지역의 번호 행정동 별칭은 후보로 반환한다`() {
        val regions = RegionOptionsMapper.findRegionalGuideEupmyeondongRegions(
            administrativeRegions = listOf(
                administrativeRegion(
                    sidoName = "대전광역시",
                    sigunguName = "서구",
                    eupmyeondongName = "괴정동"
                ),
                administrativeRegion(
                    sidoName = "부산광역시",
                    sigunguName = "사하구",
                    eupmyeondongName = "괴정제1동"
                ),
                administrativeRegion(
                    sidoName = "부산광역시",
                    sigunguName = "사하구",
                    eupmyeondongName = "괴정제2동"
                ),
                administrativeRegion(
                    sidoName = "부산광역시",
                    sigunguName = "사하구",
                    eupmyeondongName = "괴정제4동"
                )
            ),
            legalAdminDongMappings = listOf(
                legalAdminMapping(
                    sidoName = "부산광역시",
                    sigunguName = "사하구",
                    legalDongName = "괴정동",
                    adminDongName = "괴정제1동"
                ),
                legalAdminMapping(
                    sidoName = "부산광역시",
                    sigunguName = "사하구",
                    legalDongName = "괴정동",
                    adminDongName = "괴정제2동"
                ),
                legalAdminMapping(
                    sidoName = "부산광역시",
                    sigunguName = "사하구",
                    legalDongName = "괴정동",
                    adminDongName = "괴정제4동"
                )
            ),
            keyword = "괴정동"
        )

        assertEquals(
            listOf(
                Region(sido = "대전광역시", sigungu = "서구", eupmyeondong = "괴정동"),
                Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정제1동"),
                Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정제2동"),
                Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정제4동")
            ),
            regions
        )
    }

    @Test
    fun `법정동 매핑 행정동이 번호형이 아니면 번호 행정동 별칭으로 확장하지 않는다`() {
        val regions = RegionOptionsMapper.findRegionalGuideEupmyeondongRegions(
            administrativeRegions = listOf(
                administrativeRegion(
                    sidoName = "경기도",
                    sigunguName = "안양시 만안구",
                    eupmyeondongName = "안양1동"
                )
            ),
            legalAdminDongMappings = listOf(
                legalAdminMapping(
                    sidoName = "경기도",
                    sigunguName = "안양시 만안구",
                    legalDongName = "안양동",
                    adminDongName = "명학동"
                )
            ),
            keyword = "안양동"
        )

        assertEquals(
            listOf(
                Region(sido = "경기도", sigungu = "안양시 만안구", eupmyeondong = "안양동")
            ),
            regions
        )
    }

    @Test
    fun `접미사 없는 법정동 검색어는 실제 법정동 이름을 후보에 유지한다`() {
        val regions = RegionOptionsMapper.findRegionalGuideEupmyeondongRegions(
            administrativeRegions = emptyList(),
            legalAdminDongMappings = listOf(
                legalAdminMapping(
                    sidoName = "부산광역시",
                    sigunguName = "사하구",
                    legalDongName = "괴정동",
                    adminDongName = "괴정제1동"
                ),
                legalAdminMapping(
                    sidoName = "부산광역시",
                    sigunguName = "사하구",
                    legalDongName = "괴정동",
                    adminDongName = "괴정제2동"
                )
            ),
            keyword = "괴정"
        )

        assertEquals(
            listOf(
                Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정동")
            ),
            regions
        )
    }

    @Test
    fun `법정동 키워드 조회는 가 별칭을 반환하고 리 별칭은 제외한다`() {
        val keywords = RegionOptionsMapper.findLegalDongKeywordsByRegion(
            mappings = listOf(
                legalAdminMapping(
                    sidoName = "서울특별시",
                    sigunguName = "중구",
                    legalDongName = "명동1가",
                    adminDongName = "명동"
                ),
                legalAdminMapping(
                    sidoName = "서울특별시",
                    sigunguName = "중구",
                    legalDongName = "명동2가",
                    adminDongName = "명동"
                ),
                legalAdminMapping(
                    sidoName = "경상남도",
                    sigunguName = "김해시",
                    legalDongName = "명동리",
                    adminDongName = "한림면"
                )
            ),
            region = Region(sido = "서울특별시", sigungu = "중구", eupmyeondong = "명동"),
            keyword = "명동"
        )

        assertEquals(listOf("명동1가", "명동2가"), keywords)
    }

    @Test
    fun `문자 점 묶음 행정동 부분 검색은 에셋 원본 지역명 후보로 반환한다`() {
        val regions = RegionOptionsMapper.findEupmyeondongRegions(
            administrativeRegions = listOf(
                administrativeRegion(
                    sidoName = "대구광역시",
                    sigunguName = "동구",
                    eupmyeondongName = "불로.봉무동"
                )
            ),
            legalAdminDongMappings = emptyList(),
            keyword = "불로"
        )

        assertEquals(
            listOf(
                Region(sido = "대구광역시", sigungu = "동구", eupmyeondong = "불로.봉무동")
            ),
            regions
        )
    }

    @Test
    fun `문자 점 묶음 행정동 붙여쓴 검색어는 에셋 원본 지역명 후보로 반환한다`() {
        val regions = RegionOptionsMapper.findEupmyeondongRegions(
            administrativeRegions = listOf(
                administrativeRegion(
                    sidoName = "충청북도",
                    sigunguName = "충주시",
                    eupmyeondongName = "성내.충인동"
                )
            ),
            legalAdminDongMappings = emptyList(),
            keyword = "성내충인동"
        )

        assertEquals(
            listOf(
                Region(sido = "충청북도", sigungu = "충주시", eupmyeondong = "성내.충인동")
            ),
            regions
        )
    }

    @Test
    fun `문자 점 묶음 행정동 원본 검색어도 에셋 원본 지역명 후보로 반환한다`() {
        val regions = RegionOptionsMapper.findEupmyeondongRegions(
            administrativeRegions = listOf(
                administrativeRegion(
                    sidoName = "대구광역시",
                    sigunguName = "동구",
                    eupmyeondongName = "불로.봉무동"
                )
            ),
            legalAdminDongMappings = emptyList(),
            keyword = "불로.봉무동"
        )

        assertEquals(
            listOf(
                Region(sido = "대구광역시", sigungu = "동구", eupmyeondong = "불로.봉무동")
            ),
            regions
        )
    }

    @Test
    fun `숫자 점 묶음 행정동 검색 후보는 기존 행정동 표기를 유지한다`() {
        val regions = RegionOptionsMapper.findEupmyeondongRegions(
            administrativeRegions = listOf(
                administrativeRegion(
                    sidoName = "서울특별시",
                    sigunguName = "성동구",
                    eupmyeondongName = "금호2.3가동"
                )
            ),
            legalAdminDongMappings = emptyList(),
            keyword = "금호"
        )

        assertEquals(
            listOf(
                Region(sido = "서울특별시", sigungu = "성동구", eupmyeondong = "금호2.3가동")
            ),
            regions
        )
    }

    @Test
    fun `공용 읍면동 검색은 번호 행정동 별칭 대신 법정동 후보를 유지한다`() {
        val regions = RegionOptionsMapper.findEupmyeondongRegions(
            administrativeRegions = listOf(
                administrativeRegion(
                    sidoName = "대전광역시",
                    sigunguName = "유성구",
                    eupmyeondongName = "노은1동"
                )
            ),
            legalAdminDongMappings = listOf(
                legalAdminMapping(
                    sidoName = "대전광역시",
                    sigunguName = "유성구",
                    legalDongName = "노은동",
                    adminDongName = "노은1동"
                )
            ),
            keyword = "노은동"
        )

        assertEquals(
            listOf(
                Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "노은동")
            ),
            regions
        )
    }

    @Test
    fun `지역 가이드 읍면동 검색 후보는 숫자를 자연 순서로 정렬한다`() {
        val regions = RegionOptionsMapper.findRegionalGuideEupmyeondongRegions(
            administrativeRegions = listOf(
                administrativeRegion(
                    sidoName = "대전광역시",
                    sigunguName = "유성구",
                    eupmyeondongName = "노은10동"
                ),
                administrativeRegion(
                    sidoName = "대전광역시",
                    sigunguName = "유성구",
                    eupmyeondongName = "노은2동"
                )
            ),
            legalAdminDongMappings = emptyList(),
            keyword = "노은"
        )

        assertEquals(
            listOf(
                Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "노은2동"),
                Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "노은10동")
            ),
            regions
        )
    }

    @Test
    fun `공용 읍면동 검색 후보는 기존 문자 순서를 유지한다`() {
        val regions = RegionOptionsMapper.findEupmyeondongRegions(
            administrativeRegions = listOf(
                administrativeRegion(
                    sidoName = "대전광역시",
                    sigunguName = "유성구",
                    eupmyeondongName = "노은10동"
                ),
                administrativeRegion(
                    sidoName = "대전광역시",
                    sigunguName = "유성구",
                    eupmyeondongName = "노은2동"
                )
            ),
            legalAdminDongMappings = emptyList(),
            keyword = "노은"
        )

        assertEquals(
            listOf(
                Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "노은10동"),
                Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "노은2동")
            ),
            regions
        )
    }

    @Test
    fun `지역 가이드 제공 가능 대상지역명에 맞는 검색 후보만 유지한다`() {
        val regions = RegionOptionsMapper.filterAvailableRegionalGuideRegions(
            regions = listOf(
                Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "반석동"),
                Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "대동"),
            ),
            availability = listOf(
                regionalGuideAvailability(
                    sidoName = "대전광역시",
                    sigunguName = "유성구",
                    managementZoneName = "노은2동",
                    targetRegionName = "반석동 일부지역",
                )
            ),
        )

        assertEquals(
            listOf(Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "반석동")),
            regions,
        )
    }

    @Test
    fun `지역 가이드 제공 가능 정보가 없으면 읍면동 선택지에서 제외한다`() {
        val options = RegionOptionsMapper.filterRegionalGuideEupmyeondongOptions(
            options = listOf("노은1동", "노은2동", "대동"),
            availability = listOf(
                regionalGuideAvailability(
                    sidoName = "대전광역시",
                    sigunguName = "유성구",
                    managementZoneName = "노은1동",
                    targetRegionName = "노은1동",
                ),
                regionalGuideAvailability(
                    sidoName = "대전광역시",
                    sigunguName = "유성구",
                    managementZoneName = "노은2동",
                    targetRegionName = "반석동 일부지역",
                ),
                regionalGuideAvailability(
                    sidoName = "부산광역시",
                    sigunguName = "금정구",
                    managementZoneName = "대동",
                    targetRegionName = "대동",
                ),
            ),
            sido = "대전광역시",
            sigungu = "유성구",
        )

        assertEquals(listOf("노은1동", "노은2동"), options)
    }

    @Test
    fun `번호 범위 대상지역명에 포함된 괴정 행정동 검색 후보를 유지한다`() {
        val regions = RegionOptionsMapper.filterAvailableRegionalGuideRegions(
            regions = listOf(
                Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정제1동"),
                Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정제4동"),
                Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "당리동"),
            ),
            availability = listOf(
                regionalGuideAvailability(
                    sidoName = "부산광역시",
                    sigunguName = "사하구",
                    managementZoneName = "1구역",
                    targetRegionName = "괴정 1~3동, 하단 1~2동",
                ),
                regionalGuideAvailability(
                    sidoName = "부산광역시",
                    sigunguName = "사하구",
                    managementZoneName = "2구역",
                    targetRegionName = "괴정4동, 당리동",
                ),
            ),
        )

        assertEquals(
            listOf(
                Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정제1동"),
                Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정제4동"),
                Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "당리동"),
            ),
            regions,
        )
    }

    @Test
    fun `읍면동을 식별할 수 없는 제공 정보만 있으면 사천면 후보와 선택지를 유지한다`() {
        val availability = listOf(
            regionalGuideAvailability(
                sidoName = "강원특별자치도",
                sigunguName = "강릉시",
                managementZoneName = "문전수거 지역",
                targetRegionName = "문전수거 지역",
            ),
            regionalGuideAvailability(
                sidoName = "강원특별자치도",
                sigunguName = "강릉시",
                managementZoneName = "거점수거 지역",
                targetRegionName = "거점수거 지역",
            ),
        )

        val candidates = RegionOptionsMapper.filterAvailableRegionalGuideRegions(
            regions = listOf(
                Region(
                    sido = "강원특별자치도",
                    sigungu = "강릉시",
                    eupmyeondong = "사천면",
                ),
            ),
            availability = availability,
        )
        val options = RegionOptionsMapper.filterRegionalGuideEupmyeondongOptions(
            options = listOf("사천면", "주문진읍"),
            availability = availability,
            sido = "강원특별자치도",
            sigungu = "강릉시",
        )

        assertEquals(
            listOf(
                Region(
                    sido = "강원특별자치도",
                    sigungu = "강릉시",
                    eupmyeondong = "사천면",
                ),
            ),
            candidates,
        )
        assertEquals(listOf("사천면", "주문진읍"), options)
    }

    @Test
    fun `접미사가 생략된 제공 지역명에 맞는 온양읍 후보와 선택지만 반환한다`() {
        val availability = listOf(
            regionalGuideAvailability(
                sidoName = "울산광역시",
                sigunguName = "울주군",
                managementZoneName = "울산광역시 울주군",
                targetRegionName = "범서, 온양, 웅촌",
            ),
        )

        val candidates = RegionOptionsMapper.filterAvailableRegionalGuideRegions(
            regions = listOf(
                Region(sido = "울산광역시", sigungu = "울주군", eupmyeondong = "온양읍"),
                Region(sido = "울산광역시", sigungu = "울주군", eupmyeondong = "두동면"),
            ),
            availability = availability,
        )
        val options = RegionOptionsMapper.filterRegionalGuideEupmyeondongOptions(
            options = listOf("두동면", "온양읍"),
            availability = availability,
            sido = "울산광역시",
            sigungu = "울주군",
        )

        assertEquals(
            listOf(Region(sido = "울산광역시", sigungu = "울주군", eupmyeondong = "온양읍")),
            candidates,
        )
        assertEquals(listOf("온양읍"), options)
    }

    @Test
    fun `법정동 이름으로 시작하는 상세 대상지역에 맞는 부곡동 후보와 선택지만 반환한다`() {
        val availability = listOf(
            regionalGuideAvailability(
                sidoName = "경기도",
                sigunguName = "의왕시",
                managementZoneName = "의왕시",
                targetRegionName = "부곡중앙북6길",
            ),
            regionalGuideAvailability(
                sidoName = "경기도",
                sigunguName = "의왕시",
                managementZoneName = "의왕시",
                targetRegionName = "가구단지길",
            ),
        )

        val candidates = RegionOptionsMapper.filterAvailableRegionalGuideRegions(
            regions = listOf(
                Region(sido = "경기도", sigungu = "의왕시", eupmyeondong = "부곡동"),
                Region(sido = "경기도", sigungu = "의왕시", eupmyeondong = "오전동"),
            ),
            availability = availability,
        )
        val options = RegionOptionsMapper.filterRegionalGuideEupmyeondongOptions(
            options = listOf("부곡동", "오전동"),
            availability = availability,
            sido = "경기도",
            sigungu = "의왕시",
        )

        assertEquals(
            listOf(Region(sido = "경기도", sigungu = "의왕시", eupmyeondong = "부곡동")),
            candidates,
        )
        assertEquals(listOf("부곡동"), options)
    }

    @Test
    fun `전남광주통합특별시 제공 가능 정보는 광주와 전남 행정동에 적용한다`() {
        val regions = RegionOptionsMapper.filterAvailableRegionalGuideRegions(
            regions = listOf(
                Region(sido = "광주광역시", sigungu = "동구", eupmyeondong = "충장동"),
                Region(sido = "전라남도", sigungu = "나주시", eupmyeondong = "빛가람동"),
                Region(sido = "전라남도", sigungu = "나주시", eupmyeondong = "남평읍"),
            ),
            availability = listOf(
                regionalGuideAvailability(
                    sidoName = "전남광주통합특별시",
                    sigunguName = "동구",
                    managementZoneName = "충장동",
                    targetRegionName = "충장동",
                ),
                regionalGuideAvailability(
                    sidoName = "전남광주통합특별시",
                    sigunguName = "나주시",
                    managementZoneName = "빛가람동",
                    targetRegionName = "빛가람동",
                ),
            ),
        )

        assertEquals(
            listOf(
                Region(sido = "광주광역시", sigungu = "동구", eupmyeondong = "충장동"),
                Region(sido = "전라남도", sigungu = "나주시", eupmyeondong = "빛가람동"),
            ),
            regions,
        )
    }

    private fun administrativeRegion(
        sidoName: String,
        sigunguName: String,
        eupmyeondongName: String
    ): AdministrativeRegionDto {
        val fullName = listOf(
            sidoName,
            sigunguName,
            eupmyeondongName
        )
            .filter { regionName -> regionName.isNotBlank() }
            .joinToString(" ")

        return AdministrativeRegionDto(
            adminCode = "",
            sidoName = sidoName,
            sigunguName = sigunguName,
            eupmyeondongName = eupmyeondongName,
            fullName = fullName
        )
    }

    private fun legalAdminMapping(
        legalCode: String = "",
        sidoName: String,
        sigunguName: String,
        legalDongName: String,
        adminCode: String = "",
        adminDongName: String
    ): LegalAdminDongMappingDto {
        return LegalAdminDongMappingDto(
            legalCode = legalCode,
            legalDongName = legalDongName,
            adminCode = adminCode,
            sidoName = sidoName,
            sigunguName = sigunguName,
            adminDongName = adminDongName,
            adminFullName = listOf(sidoName, sigunguName, adminDongName).joinToString(" "),
            legalFullName = listOf(sidoName, sigunguName, legalDongName).joinToString(" ")
        )
    }

    private fun regionalGuideAvailability(
        sidoName: String,
        sigunguName: String,
        managementZoneName: String,
        targetRegionName: String,
    ): RegionalGuideAvailabilityDto =
        RegionalGuideAvailabilityDto(
            sidoName = sidoName,
            sigunguName = sigunguName,
            managementZoneName = managementZoneName,
            targetRegionName = targetRegionName,
        )
}
