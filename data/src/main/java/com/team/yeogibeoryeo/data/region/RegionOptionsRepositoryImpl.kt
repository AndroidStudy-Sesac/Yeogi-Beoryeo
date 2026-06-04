package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.local.RegionOptionsLocalDataSource
import com.team.yeogibeoryeo.data.region.local.dto.AdministrativeRegionDto
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import javax.inject.Inject

class RegionOptionsRepositoryImpl @Inject constructor(
    private val localDataSource: RegionOptionsLocalDataSource
) : RegionOptionsRepository {

    override suspend fun getSidoOptions(): List<String> {
        return localDataSource.getRegions()
            .map { region -> region.sidoName }
            .filter { sido -> sido.isNotBlank() }
            .distinct()
            .sorted()
    }

    override suspend fun getSigunguOptions(
        sido: String
    ): List<String> {
        return localDataSource.getRegions()
            .filter { region -> region.sidoName == sido }
            .map { region ->
                region.sigunguName.ifBlank {
                    region.sidoName
                }
            }
            .filter { sigungu -> sigungu.isNotBlank() }
            .distinct()
            .sorted()
    }

    override suspend fun getEupmyeondongOptions(
        sido: String,
        sigungu: String
    ): List<String> {
        return localDataSource.getRegions()
            .filter { region ->
                region.sidoName == sido &&
                    region.sigunguName.ifBlank { region.sidoName } == sigungu
            }
            .map { region -> region.eupmyeondongName }
            .filter { eupmyeondong -> eupmyeondong.isNotBlank() }
            .distinct()
            .sorted()
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
        val targetKeyword = keyword.trim()
        if (targetKeyword.isBlank()) return emptyList()

        val regions = localDataSource.getRegions()
        val exactMatches = regions.filter { region ->
            region.sigunguName == targetKeyword
        }

        val prefixMatches = exactMatches.ifEmpty {
            regions.filter { region ->
                region.sigunguName.startsWith(targetKeyword)
            }
        }

        val matchedRegions = prefixMatches.ifEmpty {
            regions.filter { region ->
                region.sigunguName.contains(targetKeyword)
            }
        }

        return matchedRegions
            .mapToRegion()
            .distinctBy { region ->
                listOf(
                    region.sido.orEmpty(),
                    region.sigungu.orEmpty()
                )
            }
            .sortedWith(REGION_NAME_COMPARATOR)
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

    companion object {
        private val REGION_NAME_COMPARATOR = compareBy<Region>(
            { region -> region.sido.orEmpty() },
            { region -> region.sigungu.orEmpty() },
            { region -> region.eupmyeondong.orEmpty() }
        )
    }
}
