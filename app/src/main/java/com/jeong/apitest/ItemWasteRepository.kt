package com.jeong.apitest

class ItemWasteRepository(
    private val wasteService: ItemWasteService,
    private val householdService: ItemHouseholdWasteService
) {
    suspend fun getItem(pageNo: Int, rows: Int, itemNm: String) =
        wasteService.getItem(BuildConfig.API_KEY, pageNo, rows, itemNm)

    suspend fun getSpot(
        pageNo: Int,
        rows: Int,
        addr: String,
        lat: Double?,
        lon: Double?,
        radius: Int
    ) =
        wasteService.getSpot(BuildConfig.API_KEY, pageNo, rows, addr, lat, lon, radius)

    suspend fun getHouseholdInfo(
        pageNo: Int,
        rows: Int,
        sggNameQuery: String? = null,
        updatedFrom: String? = null,
        updatedUntil: String? = null,
        administrativeCode: String? = null,
        baseDateFrom: String? = null,
        baseDateUntil: String? = null
    ): ItemWasteResponse<ItemHouseholdWasteInfo> {
        // Sejong special case handling
        val isSejong = sggNameQuery == "세종특별자치시" || sggNameQuery == "세종시" || sggNameQuery == "세종"
        val apiSgg = if (isSejong) "없음" else sggNameQuery

        val response = householdService.getInfo(
            BuildConfig.API_KEY, pageNo, rows,
            sggNameQuery = if (apiSgg.isNullOrBlank()) null else apiSgg,
            updatedFrom = updatedFrom,
            updatedUntil = updatedUntil,
            administrativeCode = administrativeCode,
            baseDateFrom = baseDateFrom,
            baseDateUntil = baseDateUntil
        )

        // Transform response for Sejong city
        if (isSejong) {
            val items = response.response.body.items?.item ?: emptyList()
            val transformedItems = items.map {
                if (it.SGG_NM == "없음") it.copy(SGG_NM = "세종특별자치시") else it
            }
            return response.copy(
                response = response.response.copy(
                    body = response.response.body.copy(
                        items = ItemItems(transformedItems)
                    )
                )
            )
        }

        return response
    }

    /**
     * Helper to extract Si/Gun/Gu from an address string.
     * This belongs here as it's a data-related transformation.
     */
    fun extractSggFromAddress(address: String): String? {
        val parts = address.split(" ")
        return parts.reversed().find { part ->
            part.endsWith("시") || part.endsWith("군") || part.endsWith("구")
        }
    }
}
