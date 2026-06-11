package com.team.yeogibeoryeo.data.favorite.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collection_spot_favorite_snapshots")
data class CollectionSpotFavoriteSnapshotEntity(
    @PrimaryKey val targetId: String,
    val name: String,
    val spotType: String,
    val address: String,
    val detailLocation: String?,
    val latitude: Double?,
    val longitude: Double?,
)
