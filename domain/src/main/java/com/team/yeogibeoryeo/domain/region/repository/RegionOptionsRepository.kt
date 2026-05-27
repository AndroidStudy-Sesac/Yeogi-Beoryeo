package com.team.yeogibeoryeo.domain.region.repository

interface RegionOptionsRepository {

    suspend fun getSidoOptions(): List<String>

    suspend fun getSigunguOptions(
        sido: String
    ): List<String>

    suspend fun getEupmyeondongOptions(
        sido: String,
        sigungu: String
    ): List<String>
}
