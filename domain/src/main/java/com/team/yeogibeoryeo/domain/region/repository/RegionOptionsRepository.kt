package com.team.yeogibeoryeo.domain.region.repository

import com.team.yeogibeoryeo.domain.region.model.Region

interface RegionOptionsRepository {

    suspend fun getSidoOptions(): List<String>

    suspend fun getSigunguOptions(
        sido: String
    ): List<String>

    suspend fun getRegionalGuideSigunguOptions(
        sido: String
    ): List<String> = getSigunguOptions(sido)

    suspend fun getEupmyeondongOptions(
        sido: String,
        sigungu: String
    ): List<String>

    suspend fun getRegionalGuideEupmyeondongOptions(
        sido: String,
        sigungu: String
    ): List<String> = getEupmyeondongOptions(sido, sigungu)

    suspend fun findRegionsByEupmyeondongKeyword(
        keyword: String
    ): List<Region>

    suspend fun findRegionalGuideRegionsByEupmyeondongKeyword(
        keyword: String
    ): List<Region> = findRegionsByEupmyeondongKeyword(keyword)

    suspend fun findAvailableRegionalGuideRegionsByEupmyeondongKeyword(
        keyword: String
    ): List<Region> = findRegionalGuideRegionsByEupmyeondongKeyword(keyword)

    suspend fun filterAvailableRegionalGuideRegions(
        regions: List<Region>
    ): List<Region> = regions

    suspend fun findLegalDongKeywordsByRegion(
        region: Region,
        keyword: String
    ): List<String> = emptyList()

    suspend fun findRegionsBySigunguKeyword(
        keyword: String
    ): List<Region>

    suspend fun normalizeRegionForRegionalGuide(
        region: Region
    ): Region

    suspend fun findAdminDongCandidatesForLegalDong(
        region: Region
    ): List<Region>
}
