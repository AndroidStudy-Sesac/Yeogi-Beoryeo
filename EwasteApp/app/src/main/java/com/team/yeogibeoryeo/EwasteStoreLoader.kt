package com.team.yeogibeoryeo

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

fun loadEwasteStores(context: Context): List<EwasteStore> {
    val jsonString = context.assets
        .open("ewaste_seoul_geocoded.json")
        .bufferedReader()
        .use { it.readText() }

    val jsonArray = JSONArray(jsonString)
    val stores = mutableListOf<EwasteStore>()

    for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)

        stores.add(
            EwasteStore(
                storeName = obj.optString("storeName"),
                region = obj.optString("region"),
                category = obj.optString("category"),
                latitude = obj.optDoubleOrNull("latitude"),
                longitude = obj.optDoubleOrNull("longitude"),
                address = obj.optString("address"),
                roadAddress = obj.optString("roadAddress"),
                matchedName = obj.optString("matchedName"),
                geocoded = obj.optBoolean("geocoded", false),
                approximate = obj.optBoolean("approximate", false),
                source = obj.optString("source")
            )
        )
    }

    return stores
}

private fun JSONObject.optDoubleOrNull(key: String): Double? {
    if (!has(key) || isNull(key)) return null

    return when (val value = opt(key)) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }
}