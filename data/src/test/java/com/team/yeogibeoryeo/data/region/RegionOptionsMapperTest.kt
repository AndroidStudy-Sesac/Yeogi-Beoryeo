package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.local.dto.AdministrativeRegionDto
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
}
