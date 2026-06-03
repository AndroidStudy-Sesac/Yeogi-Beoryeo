package com.team.yeogibeoryeo.domain.region.repository

import com.team.yeogibeoryeo.domain.region.model.Region

interface RegionOptionsRepository {

    suspend fun getSidoOptions(): List<String>

    suspend fun getSigunguOptions(
        sido: String
    ): List<String>

    suspend fun getEupmyeondongOptions(
        sido: String,
        sigungu: String
    ): List<String>

    suspend fun findRegionsByEupmyeondongKeyword(
        keyword: String
    ): List<Region>
}
