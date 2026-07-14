package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.local.dto.AdministrativeRegionDto
import com.team.yeogibeoryeo.data.region.local.dto.LegalAdminDongMappingDto
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
    fun `접미사 없는 법정동 검색어는 실제 법정동 이름을 후보에 유지한다`() {
        val regions = RegionOptionsMapper.findEupmyeondongRegions(
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
}
