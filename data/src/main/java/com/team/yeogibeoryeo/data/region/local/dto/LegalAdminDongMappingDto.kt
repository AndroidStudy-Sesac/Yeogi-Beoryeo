package com.team.yeogibeoryeo.data.region.local.dto

import kotlinx.serialization.Serializable

@Serializable
data class LegalAdminDongMappingDto(
    val legalCode: String,
    val legalDongName: String,
    val adminCode: String,
    val sidoName: String,
    val sigunguName: String,
    val adminDongName: String,
    val adminFullName: String,
    val legalFullName: String,
    val createdDate: String? = null
)
