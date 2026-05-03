package com.team.yeogibeoryeo

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

fun fetchWasteSpotsByAddress(
    addr: String,
    serviceKey: String,
    numOfRows: Int = 100
): List<WasteSpot> {
    val encodedAddr = URLEncoder.encode(addr, "UTF-8")
    val encodedServiceKey = URLEncoder.encode(serviceKey, "UTF-8")

    val urlString =
        "https://apis.data.go.kr/1482000/WasteRecyclingService/getSpot" +
                "?serviceKey=$encodedServiceKey" +
                "&pageNo=1" +
                "&numOfRows=$numOfRows" +
                "&addr=$encodedAddr" +
                "&_type=json"

    val connection = URL(urlString).openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 10_000
    connection.readTimeout = 10_000

    return try {
        val responseText = connection.inputStream
            .bufferedReader()
            .use { it.readText() }

        parseWasteSpotResponse(responseText)
            .filter { it.spotName == "중소형 수거함" }
    } finally {
        connection.disconnect()
    }
}

private fun parseWasteSpotResponse(jsonText: String): List<WasteSpot> {
    val root = JSONObject(jsonText)
    val response = root.getJSONObject("response")
    val header = response.getJSONObject("header")

    val resultCode = header.optString("resultCode")
    if (resultCode != "00") {
        val resultMsg = header.optString("resultMsg")

        if (resultCode == "03" || resultCode == "3") {
            return emptyList()
        }

        throw IllegalStateException("공공데이터 API 오류: $resultCode / $resultMsg")
    }

    val body = response.getJSONObject("body")
    val itemsObject = body.optJSONObject("items") ?: return emptyList()
    val itemValue = itemsObject.opt("item") ?: return emptyList()

    val itemArray = when (itemValue) {
        is JSONArray -> itemValue
        is JSONObject -> JSONArray().put(itemValue)
        else -> return emptyList()
    }

    val result = mutableListOf<WasteSpot>()

    for (i in 0 until itemArray.length()) {
        val obj = itemArray.getJSONObject(i)

        result.add(
            WasteSpot(
                spotName = obj.optString("spotNm"),
                address = obj.optString("addrBase"),
                detailLocation = obj.optString("addrDtl")
            )
        )
    }

    return result
}