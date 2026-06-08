package com.team.yeogibeoryeo.data.region.local

import android.content.Context
import com.team.yeogibeoryeo.data.region.local.dto.AdministrativeRegionDto
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Singleton
class RegionOptionsLocalDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private var cachedRegions: List<AdministrativeRegionDto>? = null

    suspend fun getRegions(): List<AdministrativeRegionDto> {
        return cachedRegions ?: loadRegions().also { regions ->
            cachedRegions = regions
        }
    }

    private suspend fun loadRegions(): List<AdministrativeRegionDto> {
        return withContext(Dispatchers.IO) {
            val jsonText = context.assets
                .open(REGION_ASSET_PATH)
                .bufferedReader()
                .use { reader -> reader.readText() }

            json.decodeFromString<List<AdministrativeRegionDto>>(jsonText)
        }
    }

    companion object {
        private const val REGION_ASSET_PATH =
            "region/administrative_regions.20260102.json"
    }
}
