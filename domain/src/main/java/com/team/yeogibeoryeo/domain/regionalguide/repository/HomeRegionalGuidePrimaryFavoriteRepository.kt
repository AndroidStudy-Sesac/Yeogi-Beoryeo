package com.team.yeogibeoryeo.domain.regionalguide.repository

import kotlinx.coroutines.flow.Flow

interface HomeRegionalGuidePrimaryFavoriteRepository {
    fun observePrimaryFavoriteTargetId(): Flow<String?>

    suspend fun setPrimaryFavoriteTargetId(targetId: String)

    suspend fun clearPrimaryFavoriteTargetId()

    suspend fun clearPrimaryFavoriteTargetIdIfMatches(targetId: String)
}
