package com.team.yeogibeoryeo

data class EwasteStore(
    val storeName: String,
    val region: String,
    val category: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String = "",
    val roadAddress: String = "",
    val matchedName: String = "",
    val geocoded: Boolean = false,
    val approximate: Boolean = false,
    val source: String = ""
)