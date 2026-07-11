package com.team.yeogibeoryeo.data.region.local

import android.content.Context
import com.team.yeogibeoryeo.data.region.local.dto.LegalAdminDongMappingDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegalAdminDongMappingLocalDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private var cachedMappings: List<LegalAdminDongMappingDto>? = null

    suspend fun getMappings(): List<LegalAdminDongMappingDto> {
        return cachedMappings ?: loadMappings().also { mappings ->
            cachedMappings = mappings
        }
    }

    private suspend fun loadMappings(): List<LegalAdminDongMappingDto> {
        return withContext(Dispatchers.IO) {
            val jsonText = context.assets
                .open(MAPPING_ASSET_PATH)
                .bufferedReader()
                .use { reader -> reader.readText() }

            json.decodeFromString<List<LegalAdminDongMappingDto>>(jsonText)
        }
    }

    companion object {
        private const val MAPPING_ASSET_PATH =
            "region/legal_to_admin_mappings.20260701.json"
    }
}
