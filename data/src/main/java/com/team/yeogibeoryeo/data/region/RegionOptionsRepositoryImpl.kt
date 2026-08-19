package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.local.LegalAdminDongMappingLocalDataSource
import com.team.yeogibeoryeo.data.region.local.RegionOptionsLocalDataSource
import com.team.yeogibeoryeo.data.region.local.RegionalGuideAvailabilityLocalDataSource
import com.team.yeogibeoryeo.data.region.local.RegionalGuideRegionOptionsLocalDataSource
import com.team.yeogibeoryeo.data.region.local.dto.AdministrativeRegionDto
import com.team.yeogibeoryeo.data.region.local.dto.LegalAdminDongMappingDto
import com.team.yeogibeoryeo.data.region.local.dto.RegionalGuideAvailabilityDto
import com.team.yeogibeoryeo.data.region.local.dto.RegionalGuideRegionDto
import com.team.yeogibeoryeo.domain.core.di.DefaultDispatcher
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RegionOptionsRepositoryImpl internal constructor(
    private val getAdministrativeRegions: suspend () -> List<AdministrativeRegionDto>,
    private val getLegalAdminDongMappings: suspend () -> List<LegalAdminDongMappingDto>,
    private val getRegionalGuideAvailabilityRegions: suspend () -> List<RegionalGuideAvailabilityDto>,
    private val getRegionalGuideRegionOptions: suspend () -> List<RegionalGuideRegionDto>,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : RegionOptionsRepository {

    private val regionSearchIndexMutex = Mutex()
    private var regionSearchIndex: RegionSearchIndex? = null

    @Inject constructor(
        localDataSource: RegionOptionsLocalDataSource,
        legalAdminDongMappingLocalDataSource: LegalAdminDongMappingLocalDataSource,
        regionalGuideAvailabilityLocalDataSource: RegionalGuideAvailabilityLocalDataSource,
        regionalGuideRegionOptionsLocalDataSource: RegionalGuideRegionOptionsLocalDataSource,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ) : this(
        getAdministrativeRegions = { localDataSource.getRegions() },
        getLegalAdminDongMappings = { legalAdminDongMappingLocalDataSource.getMappings() },
        getRegionalGuideAvailabilityRegions = {
            regionalGuideAvailabilityLocalDataSource.getRegions()
        },
        getRegionalGuideRegionOptions = { regionalGuideRegionOptionsLocalDataSource.getRegions() },
        defaultDispatcher = defaultDispatcher,
    )

    override suspend fun getSidoOptions(): List<String> {
        val regionalGuideRegions = getAvailableRegionalGuideRegions()

        return mapRegionOptions {
            RegionOptionsMapper.getSidoOptions(
                regionalGuideRegions = regionalGuideRegions
            )
        }
    }

    override suspend fun getSigunguOptions(
        sido: String
    ): List<String> {
        val regionalGuideRegions = getAvailableRegionalGuideRegions()

        return mapRegionOptions {
            RegionOptionsMapper.getSigunguOptions(
                regionalGuideRegions = regionalGuideRegions,
                sido = sido
            )
        }
    }

    override suspend fun getEupmyeondongOptions(
        sido: String,
        sigungu: String
    ): List<String> {
        val administrativeRegions = getAdministrativeRegions()

        return mapRegionOptions {
            RegionOptionsMapper.getEupmyeondongOptions(
                administrativeRegions = administrativeRegions,
                sido = sido,
                sigungu = sigungu
            )
        }
    }

    override suspend fun findRegionsByEupmyeondongKeyword(
        keyword: String
    ): List<Region> =
        mapRegionOptions {
            getRegionSearchIndex().findEupmyeondongRegions(keyword)
        }

    override suspend fun getRegionalGuideSigunguOptions(
        sido: String
    ): List<String> {
        val regionalGuideRegions = getAvailableRegionalGuideRegions()

        return mapRegionOptions {
            RegionOptionsMapper.getRegionalGuideSigunguOptions(
                regionalGuideRegions = regionalGuideRegions,
                sido = sido
            )
        }
    }

    override suspend fun getRegionalGuideEupmyeondongOptions(
        sido: String,
        sigungu: String
    ): List<String> {
        val administrativeRegions = getAdministrativeRegions()
        val options = mapRegionOptions {
            RegionOptionsMapper.getRegionalGuideEupmyeondongOptions(
                administrativeRegions = administrativeRegions,
                sido = sido,
                sigungu = sigungu
            )
        }
        val availability = getRegionalGuideAvailability()
        if (availability.isEmpty()) return options

        return mapRegionOptions {
            RegionalGuideAvailabilityFilter.filterEupmyeondongOptions(
                options = options,
                availability = availability,
                sido = sido,
                sigungu = sigungu,
            )
        }
    }

    override suspend fun findRegionalGuideRegionsByEupmyeondongKeyword(
        keyword: String
    ): List<Region> =
        mapRegionOptions {
            getRegionSearchIndex().findRegionalGuideEupmyeondongRegions(keyword)
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

        return mapRegionOptions {
            RegionalGuideAvailabilityFilter.filterRegions(
                regions = regions,
                availability = availability,
            )
        }
    }

    override suspend fun findLegalDongKeywordsByRegion(
        region: Region,
        keyword: String
    ): List<String> =
        mapRegionOptions {
            getRegionSearchIndex().findLegalDongKeywordsByRegion(region, keyword)
        }

    override suspend fun findRegionsBySigunguKeyword(
        keyword: String
    ): List<Region> =
        mapRegionOptions {
            getRegionSearchIndex().findSigunguRegions(keyword)
        }

    override suspend fun normalizeRegionForRegionalGuide(
        region: Region
    ): Region {
        val regionalGuideRegions = getAvailableRegionalGuideRegions()

        return mapRegionOptions {
            RegionOptionsMapper.normalizeRegionForRegionalGuide(
                region = region,
                regionalGuideRegions = regionalGuideRegions
            )
        }
    }

    override suspend fun findAdminDongCandidatesForLegalDong(
        region: Region
    ): List<Region> =
        mapRegionOptions {
            getRegionSearchIndex().findAdminDongCandidatesForLegalDong(region)
        }

    private suspend fun getRegionSearchIndex(): RegionSearchIndex =
        regionSearchIndex ?: regionSearchIndexMutex.withLock {
            regionSearchIndex ?: RegionSearchIndex.create(
                administrativeRegions = getAdministrativeRegions(),
                legalAdminDongMappings = getLegalAdminDongMappings(),
                regionalGuideRegions = getAvailableRegionalGuideRegions(),
            ).also { index ->
                regionSearchIndex = index
            }
        }

    private suspend fun getAvailableRegionalGuideRegions(): List<RegionalGuideRegionDto> {
        val availability = getRegionalGuideAvailability()
        val availableRegions = mapRegionOptions {
            availability.map { region ->
                RegionalGuideRegionDto(
                    sidoName = region.sidoName,
                    sigunguName = region.sigunguName
                )
            }
                .distinct()
        }

        return availableRegions.ifEmpty {
            getRegionalGuideRegionOptions()
        }
    }

    private suspend fun getRegionalGuideAvailability(): List<RegionalGuideAvailabilityDto> =
        getRegionalGuideAvailabilityRegions()

    private suspend fun <T> mapRegionOptions(block: suspend () -> T): T =
        withContext(defaultDispatcher) {
            block()
        }

}
