package com.team.yeogibeoryeo

data class WasteSpot(
    val spotName: String,
    val address: String,
    val detailLocation: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)