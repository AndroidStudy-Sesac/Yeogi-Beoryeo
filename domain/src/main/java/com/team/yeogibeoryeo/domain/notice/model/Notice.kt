package com.team.yeogibeoryeo.domain.notice.model

data class Notice(
    val id: String,
    val title: String,
    val body: String,
    val publishedAtMillis: Long,
    val updatedAtMillis: Long?,
)
