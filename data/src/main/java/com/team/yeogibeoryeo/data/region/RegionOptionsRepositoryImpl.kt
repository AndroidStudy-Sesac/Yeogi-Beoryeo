package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.local.RegionOptionsLocalDataSource
import com.team.yeogibeoryeo.data.region.local.LegalAdminDongMappingLocalDataSource
import com.team.yeogibeoryeo.data.region.local.RegionalGuideRegionOptionsLocalDataSource
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import javax.inject.Inject

class RegionOptionsRepositoryImpl @Inject constructor(
    private val localDataSource: RegionOptionsLocalDataSource,
    private val legalAdminDongMappingLocalDataSource: LegalAdminDongMappingLocalDataSource,
    private val regionalGuideRegionOptionsLocalDataSource: RegionalGuideRegionOptionsLocalDataSource
) : RegionOptionsRepository {

    override suspend fun getSidoOptions(): List<String> {
        return RegionOptionsMapper.getSidoOptions(
            regionalGuideRegions = regionalGuideRegionOptionsLocalDataSource.getRegions()
        )
    }

    override suspend fun getSigunguOptions(
        sido: String
    ): List<String> {
        return RegionOptionsMapper.getSigunguOptions(
            regionalGuideRegions = regionalGuideRegionOptionsLocalDataSource.getRegions(),
            sido = sido
        )
    }

    override suspend fun getEupmyeondongOptions(
        sido: String,
        sigungu: String
    ): List<String> {
        return RegionOptionsMapper.getEupmyeondongOptions(
            administrativeRegions = localDataSource.getRegions(),
            sido = sido,
            sigungu = sigungu
        )
    }

    override suspend fun findRegionsByEupmyeondongKeyword(
        keyword: String
    ): List<Region> {
        return RegionOptionsMapper.findEupmyeondongRegions(
            administrativeRegions = localDataSource.getRegions(),
            legalAdminDongMappings = legalAdminDongMappingLocalDataSource.getMappings(),
            keyword = keyword
        )
    }

    override suspend fun findLegalDongKeywordsByRegion(
        region: Region,
        keyword: String
    ): List<String> {
        return RegionOptionsMapper.findLegalDongKeywordsByRegion(
            mappings = legalAdminDongMappingLocalDataSource.getMappings(),
            region = region,
            keyword = keyword
        )
    }

    override suspend fun findRegionsBySigunguKeyword(
        keyword: String
    ): List<Region> {
        return RegionOptionsMapper.findSigunguRegions(
            administrativeRegions = localDataSource.getRegions(),
            regionalGuideRegions = regionalGuideRegionOptionsLocalDataSource.getRegions(),
            keyword = keyword
        )
    }

    override suspend fun normalizeRegionForRegionalGuide(
        region: Region
    ): Region {
        return RegionOptionsMapper.normalizeRegionForRegionalGuide(
            region = region,
            regionalGuideRegions = regionalGuideRegionOptionsLocalDataSource.getRegions()
        )
    }

    override suspend fun findAdminDongCandidatesForLegalDong(
        region: Region
    ): List<Region> {
        return RegionOptionsMapper.findAdminDongCandidatesForLegalDong(
            mappings = legalAdminDongMappingLocalDataSource.getMappings(),
            region = region
        )
    }

}
