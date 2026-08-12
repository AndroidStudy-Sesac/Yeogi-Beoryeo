package com.team.yeogibeoryeo.data.notice.remote.dto

data class NoticeDto(
    val id: String,
    val title: String?,
    val body: String?,
    val publishedAtMillis: Long?,
    val updatedAtMillis: Long?,
)
