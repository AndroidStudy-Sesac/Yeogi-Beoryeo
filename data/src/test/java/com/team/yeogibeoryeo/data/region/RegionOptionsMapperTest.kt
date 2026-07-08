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
    fun `sigungu options use regional guide info regions`() {
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
    fun `sigungu options do not expose administrative districts duplicated by info region`() {
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
    fun `sejong sigungu option is displayed as sejong name instead of none`() {
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
    fun `eupmyeondong options map administrative district sigungu to info sigungu`() {
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
    fun `eupmyeondong options map sejong administrative regions to sejong display sigungu`() {
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
    fun `sigungu keyword search prefers info provided city region over administrative districts`() {
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
    fun `sigungu keyword search falls back to administrative district when info region does not exist`() {
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
    fun `sigungu keyword search maps administrative district to upper info query key`() {
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
    fun `normalize region maps administrative district to info provided sigungu`() {
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
    fun `normalize region maps official city name to info key without city suffix`() {
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
    fun `eupmyeondong options map official city name to info key without city suffix`() {
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
    fun `sigungu keyword search maps official city keyword to info key without city suffix`() {
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
    fun `normalize region keeps original sigungu when info provided region does not exist`() {
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
    fun `normalize sejong region uses sejong display sigungu`() {
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
    fun `legal dong lookup returns mapped admin dongs by exact sido sigungu and legal dong`() {
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
    fun `legal dong lookup keeps one to many and many to one relationships without suffix inference`() {
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
    fun `legal dong lookup returns empty list when exact mapping is absent`() {
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
    fun `eupmyeondong lookup includes legal dong candidates with admin dong suffixes`() {
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
    fun `legal dong keyword lookup returns ga aliases but excludes ri aliases`() {
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
        sidoName: String,
        sigunguName: String,
        legalDongName: String,
        adminDongName: String
    ): LegalAdminDongMappingDto {
        return LegalAdminDongMappingDto(
            legalCode = "",
            legalDongName = legalDongName,
            adminCode = "",
            sidoName = sidoName,
            sigunguName = sigunguName,
            adminDongName = adminDongName,
            adminFullName = listOf(sidoName, sigunguName, adminDongName).joinToString(" "),
            legalFullName = listOf(sidoName, sigunguName, legalDongName).joinToString(" ")
        )
    }
}
