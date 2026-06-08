package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.local.RegionOptionsLocalDataSource
import com.team.yeogibeoryeo.data.region.local.RegionalGuideRegionOptionsLocalDataSource
import com.team.yeogibeoryeo.data.region.local.dto.AdministrativeRegionDto
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import javax.inject.Inject

class RegionOptionsRepositoryImpl @Inject constructor(
    private val localDataSource: RegionOptionsLocalDataSource,
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
        val targetKeyword = keyword.trim()
        if (targetKeyword.isBlank()) return emptyList()

        val regions = localDataSource.getRegions()
        val exactMatches = regions
            .filter { region -> region.eupmyeondongName == targetKeyword }

        val matchedRegions = exactMatches.ifEmpty {
            regions.filter { region ->
                region.eupmyeondongName.startsWith(targetKeyword)
            }
        }

        return matchedRegions
            .mapToRegion()
            .distinct()
            .sortedWith(REGION_NAME_COMPARATOR)
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

    private fun List<AdministrativeRegionDto>.mapToRegion(): List<Region> {
        return map { region ->
            RegionNormalizer.normalize(
                Region(
                    sido = region.sidoName,
                    sigungu = region.sigunguName.ifBlank { null },
                    eupmyeondong = region.eupmyeondongName
                )
            )
        }
    }

    private companion object {
        private val REGION_NAME_COMPARATOR = compareBy<Region>(
            { region -> region.sido.orEmpty() },
            { region -> region.sigungu.orEmpty() },
            { region -> region.eupmyeondong.orEmpty() }
        )
    }
}
