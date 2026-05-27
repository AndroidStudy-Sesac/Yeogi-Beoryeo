package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.local.RegionOptionsLocalDataSource
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
    }
}