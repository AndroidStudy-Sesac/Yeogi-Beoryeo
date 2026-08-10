package com.team.yeogibeoryeo.data.operationnotice.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.team.yeogibeoryeo.data.BuildConfig
import com.team.yeogibeoryeo.data.operationnotice.mapper.toDomainOrNull
import com.team.yeogibeoryeo.data.operationnotice.remote.OperationNoticesRemoteConfigDto
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNotice
import com.team.yeogibeoryeo.domain.operationnotice.repository.OperationNoticeRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume

class FirebaseRemoteConfigOperationNoticeRepository
@Inject
constructor() : OperationNoticeRepository {
    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
    private val refreshSignals = MutableStateFlow(0)
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override fun observeOperationNotices(): Flow<List<OperationNotice>> =
        refreshSignals.map {
            parseOperationNotices(remoteConfig.getString(OPERATION_NOTICES_KEY))
        }

    override suspend fun refreshOperationNotices() {
        try {
            remoteConfig.setConfigSettingsAsync(
                remoteConfigSettings {
                    minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) {
                        DEBUG_MINIMUM_FETCH_INTERVAL_SECONDS
                    } else {
                        RELEASE_MINIMUM_FETCH_INTERVAL_SECONDS
                    }
                },
            ).await()
            remoteConfig.setDefaultsAsync(
                mapOf(OPERATION_NOTICES_KEY to DEFAULT_OPERATION_NOTICES_JSON),
            ).await()
            remoteConfig.fetchAndActivate().await()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            // Remote Config 실패는 공지 미노출로만 처리하고 앱 사용을 막지 않는다.
        } finally {
            refreshSignals.value += 1
        }
    }

    private fun parseOperationNotices(rawJson: String): List<OperationNotice> {
        if (rawJson.isBlank()) return emptyList()

        return try {
            val dto = json.decodeFromString<OperationNoticesRemoteConfigDto>(rawJson)
            if (dto.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
                emptyList()
            } else {
                dto.notices.mapNotNull { notice -> notice.toDomainOrNull() }
            }
        } catch (exception: SerializationException) {
            emptyList()
        } catch (exception: IllegalArgumentException) {
            emptyList()
        }
    }

    private suspend fun <T> Task<T>.await(): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWith(Result.failure(task.exception ?: IllegalStateException()))
                }
            }
        }

    private companion object {
        const val OPERATION_NOTICES_KEY = "operation_notices"
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val DEBUG_MINIMUM_FETCH_INTERVAL_SECONDS = 0L
        const val RELEASE_MINIMUM_FETCH_INTERVAL_SECONDS = 3600L
        const val DEFAULT_OPERATION_NOTICES_JSON = """{"schemaVersion":1,"notices":[]}"""
    }
}

