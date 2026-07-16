package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.local.LegalAdminDongMappingLocalDataSource
import com.team.yeogibeoryeo.data.region.local.RegionOptionsLocalDataSource
import com.team.yeogibeoryeo.data.region.local.RegionalGuideAvailabilityLocalDataSource
import com.team.yeogibeoryeo.data.region.local.RegionalGuideRegionOptionsLocalDataSource
import com.team.yeogibeoryeo.data.region.local.dto.RegionalGuideAvailabilityDto
import com.team.yeogibeoryeo.data.region.local.dto.RegionalGuideRegionDto
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import javax.inject.Inject

class RegionOptionsRepositoryImpl @Inject constructor(
    private val localDataSource: RegionOptionsLocalDataSource,
    private val legalAdminDongMappingLocalDataSource: LegalAdminDongMappingLocalDataSource,
    private val regionalGuideAvailabilityLocalDataSource: RegionalGuideAvailabilityLocalDataSource,
    private val regionalGuideRegionOptionsLocalDataSource: RegionalGuideRegionOptionsLocalDataSource
) : RegionOptionsRepository {

    override suspend fun getSidoOptions(): List<String> {
        return RegionOptionsMapper.getSidoOptions(
            regionalGuideRegions = getAvailableRegionalGuideRegions()
        )
    }

    override suspend fun getSigunguOptions(
        sido: String
    ): List<String> {
        return RegionOptionsMapper.getSigunguOptions(
            regionalGuideRegions = getAvailableRegionalGuideRegions(),
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

    override suspend fun getRegionalGuideSigunguOptions(
        sido: String
    ): List<String> {
        return RegionOptionsMapper.getRegionalGuideSigunguOptions(
            regionalGuideRegions = getAvailableRegionalGuideRegions(),
            sido = sido
        )
    }

    override suspend fun getRegionalGuideEupmyeondongOptions(
        sido: String,
        sigungu: String
    ): List<String> {
        val options = RegionOptionsMapper.getRegionalGuideEupmyeondongOptions(
            administrativeRegions = localDataSource.getRegions(),
            sido = sido,
            sigungu = sigungu
        )
        val availability = getRegionalGuideAvailability()
        if (availability.isEmpty()) return options

        return RegionOptionsMapper.filterRegionalGuideEupmyeondongOptions(
            options = options,
            availability = availability,
            sido = sido,
            sigungu = sigungu,
        )
    }

    override suspend fun findRegionalGuideRegionsByEupmyeondongKeyword(
        keyword: String
    ): List<Region> {
        return RegionOptionsMapper.findRegionalGuideEupmyeondongRegions(
            administrativeRegions = localDataSource.getRegions(),
            legalAdminDongMappings = legalAdminDongMappingLocalDataSource.getMappings(),
            keyword = keyword
        )
    }

    override suspend fun findAvailableRegionalGuideRegionsByEupmyeondongKeyword(
        keyword: String
    ): List<Region> {
        val regions = findRegionalGuideRegionsByEupmyeondongKeyword(keyword)

        return filterAvailableRegionalGuideRegions(regions)
    }

    override suspend fun filterAvailableRegionalGuideRegions(
        regions: List<Region>
    ): List<Region> {
        val availability = getRegionalGuideAvailability()
        if (availability.isEmpty()) return regions

        return RegionOptionsMapper.filterAvailableRegionalGuideRegions(
            regions = regions,
            availability = availability,
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
            regionalGuideRegions = getAvailableRegionalGuideRegions(),
            keyword = keyword
        )
    }

    override suspend fun normalizeRegionForRegionalGuide(
        region: Region
    ): Region {
        return RegionOptionsMapper.normalizeRegionForRegionalGuide(
            region = region,
            regionalGuideRegions = getAvailableRegionalGuideRegions()
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

    private suspend fun getAvailableRegionalGuideRegions(): List<RegionalGuideRegionDto> {
        val availableRegions = getRegionalGuideAvailability()
            .map { region ->
                RegionalGuideRegionDto(
                    sidoName = region.sidoName,
                    sigunguName = region.sigunguName
                )
            }
            .distinct()

        return availableRegions.ifEmpty {
            regionalGuideRegionOptionsLocalDataSource.getRegions()
        }
    }

    private suspend fun getRegionalGuideAvailability(): List<RegionalGuideAvailabilityDto> =
        regionalGuideAvailabilityLocalDataSource.getRegions()

}
