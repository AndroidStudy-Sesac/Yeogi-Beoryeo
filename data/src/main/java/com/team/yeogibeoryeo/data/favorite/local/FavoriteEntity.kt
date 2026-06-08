package com.team.yeogibeoryeo.data.favorite.local

import androidx.room.Entity

@Entity(
    tableName = "favorites",
    primaryKeys = ["type", "targetId"],
)
data class FavoriteEntity(
    val type: String,
    val targetId: String,
    val savedAtMillis: Long,
)
