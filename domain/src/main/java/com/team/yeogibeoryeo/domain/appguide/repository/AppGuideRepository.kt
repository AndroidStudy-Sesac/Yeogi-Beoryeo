package com.team.yeogibeoryeo.domain.appguide.repository

import kotlinx.coroutines.flow.Flow

interface AppGuideRepository {
    fun observeCompletedVersion(): Flow<Int>

    suspend fun markCompleted(version: Int)

    fun observeCompletedMapLocationGuideVersion(): Flow<Int>

    suspend fun markMapLocationGuideCompleted(version: Int)

    fun observeHasRequestedMapLocationPermission(): Flow<Boolean>

    suspend fun markMapLocationPermissionRequested()

    fun observeIsMapLocationPermissionBlocked(): Flow<Boolean>

    suspend fun markMapLocationPermissionBlocked()

    suspend fun clearMapLocationPermissionBlocked()
}
