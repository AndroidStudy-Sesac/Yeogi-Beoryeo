package com.team.yeogibeoryeo.data.favorite.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "regional_guide_favorite_snapshots")
data class RegionalGuideFavoriteSnapshotEntity(
    @PrimaryKey val targetId: String,
    val sido: String?,
    val sigungu: String?,
    val eupmyeondong: String?,
    val targetRegionName: String?,
    val managementZoneName: String?,
)
