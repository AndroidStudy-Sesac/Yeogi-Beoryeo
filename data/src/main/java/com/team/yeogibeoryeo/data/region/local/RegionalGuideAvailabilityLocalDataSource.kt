package com.team.yeogibeoryeo.data.region.local

import android.content.Context
import com.team.yeogibeoryeo.data.region.local.dto.RegionalGuideAvailabilityDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegionalGuideAvailabilityLocalDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private var cachedRegions: List<RegionalGuideAvailabilityDto>? = null

    suspend fun getRegions(): List<RegionalGuideAvailabilityDto> {
        return cachedRegions ?: loadRegions().also { regions ->
            cachedRegions = regions
        }
    }

    private suspend fun loadRegions(): List<RegionalGuideAvailabilityDto> {
        return withContext(Dispatchers.IO) {
            val jsonText = context.assets
                .open(RegionAssetContract.REGIONAL_GUIDE_AVAILABILITY_ASSET_PATH)
                .bufferedReader()
                .use { reader -> reader.readText() }

            json.decodeFromString<List<RegionalGuideAvailabilityDto>>(jsonText)
        }
    }
}
