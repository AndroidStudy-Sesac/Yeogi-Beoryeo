package com.team.yeogibeoryeo.domain.regionalguide.repository

import kotlinx.coroutines.flow.Flow

interface HomeRegionalGuidePrimaryFavoriteRepository {
    fun observePrimaryFavoriteTargetId(): Flow<String?>

    fun observeLastSelectedFavoriteTargetId(): Flow<String?>

    suspend fun setPrimaryFavoriteTargetId(targetId: String)

    suspend fun clearPrimaryFavoriteTargetId()

    suspend fun clearPrimaryFavoriteTargetIdIfMatches(targetId: String)

    suspend fun setLastSelectedFavoriteTargetId(targetId: String)

    suspend fun clearLastSelectedFavoriteTargetId()

    suspend fun clearLastSelectedFavoriteTargetIdIfMatches(targetId: String)
}
