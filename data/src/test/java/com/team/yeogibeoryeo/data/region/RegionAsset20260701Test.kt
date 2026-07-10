package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.local.dto.AdministrativeRegionDto
import com.team.yeogibeoryeo.data.region.local.dto.LegalAdminDongMappingDto
import com.team.yeogibeoryeo.data.region.local.dto.RegionalGuideRegionDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RegionAsset20260701Test {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `행정구역 자산에 20260701 기준 지역 변경이 반영된다`() {
        val regions = decodeAsset<List<AdministrativeRegionDto>>(ADMINISTRATIVE_REGION_ASSET_PATH)

        val incheonSigunguNames = regions
            .filter { region -> region.sidoName == "인천광역시" }
            .map { region -> region.sigunguName }
            .toSet()

        assertTrue("영종구" in incheonSigunguNames)
        assertTrue("제물포구" in incheonSigunguNames)
        assertTrue("서해구" in incheonSigunguNames)
        assertTrue("검단구" in incheonSigunguNames)
        assertFalse("중구" in incheonSigunguNames)
        assertFalse("동구" in incheonSigunguNames)
        assertFalse("서구" in incheonSigunguNames)

        assertTrue(regions.any { region -> region.sidoName == "전남광주통합특별시" })
        assertFalse(regions.any { region -> region.sidoName == "광주광역시" })
        assertFalse(regions.any { region -> region.sidoName == "전라남도" })
    }

    @Test
    fun `행정구역과 법정동 매핑 자산에 안양 행정동 변경이 반영된다`() {
        val regions = decodeAsset<List<AdministrativeRegionDto>>(ADMINISTRATIVE_REGION_ASSET_PATH)
        val mappings = decodeAsset<List<LegalAdminDongMappingDto>>(LEGAL_ADMIN_MAPPING_ASSET_PATH)

        val anyangAdminDongNames = regions
            .filter { region ->
                region.sidoName == "경기도" &&
                    region.sigunguName == "안양시 만안구"
            }
            .map { region -> region.eupmyeondongName }
            .toSet()

        assertTrue("명학동" in anyangAdminDongNames)
        assertTrue("병목안동" in anyangAdminDongNames)
        assertFalse("안양8동" in anyangAdminDongNames)
        assertFalse("안양9동" in anyangAdminDongNames)

        val anyangMappingAdminDongNames = mappings
            .filter { mapping ->
                mapping.sidoName == "경기도" &&
                    mapping.sigunguName == "안양시 만안구" &&
                    mapping.legalDongName == "안양동"
            }
            .map { mapping -> mapping.adminDongName }
            .toSet()

        assertTrue("명학동" in anyangMappingAdminDongNames)
        assertTrue("병목안동" in anyangMappingAdminDongNames)
        assertFalse("안양8동" in anyangMappingAdminDongNames)
        assertFalse("안양9동" in anyangMappingAdminDongNames)
    }

    @Test
    fun `법정동 행정동 매핑 자산은 복수 후보 매핑을 유지한다`() {
        val mappings = decodeAsset<List<LegalAdminDongMappingDto>>(LEGAL_ADMIN_MAPPING_ASSET_PATH)

        val geumhoAdminDongNames = mappings
            .filter { mapping ->
                mapping.sidoName == "전남광주통합특별시" &&
                    mapping.sigunguName == "서구" &&
                    mapping.legalDongName == "금호동"
            }
            .map { mapping -> mapping.adminDongName }
            .toSet()

        assertTrue("금호1동" in geumhoAdminDongNames)
        assertTrue("금호2동" in geumhoAdminDongNames)
    }

    @Test
    fun `지역 가이드 지역 자산은 20260701 기준 지역명을 따른다`() {
        val regions = decodeAsset<List<RegionalGuideRegionDto>>(REGIONAL_GUIDE_REGION_ASSET_PATH)

        val incheonSigunguNames = regions
            .filter { region -> region.sidoName == "인천광역시" }
            .map { region -> region.sigunguName }
            .toSet()

        assertTrue("영종구" in incheonSigunguNames)
        assertTrue("제물포구" in incheonSigunguNames)
        assertTrue("서해구" in incheonSigunguNames)
        assertTrue("검단구" in incheonSigunguNames)
        assertFalse("중구" in incheonSigunguNames)
        assertFalse("동구" in incheonSigunguNames)
        assertFalse("서구" in incheonSigunguNames)

        assertTrue(
            regions.any { region ->
                region.sidoName == "전남광주통합특별시" &&
                    region.sigunguName == "광산구"
            }
        )
        assertFalse(regions.any { region -> region.sidoName == "광주광역시" })
        assertFalse(regions.any { region -> region.sidoName == "전라남도" })
    }

    private inline fun <reified T> decodeAsset(path: String): T {
        return json.decodeFromString(File(path).readText())
    }

    private companion object {
        const val ADMINISTRATIVE_REGION_ASSET_PATH =
            "src/main/assets/region/administrative_regions.20260701.json"
        const val LEGAL_ADMIN_MAPPING_ASSET_PATH =
            "src/main/assets/region/legal_to_admin_mappings.20260701.json"
        const val REGIONAL_GUIDE_REGION_ASSET_PATH =
            "src/main/assets/region/regional_guide_regions.20260701.json"
    }
}
