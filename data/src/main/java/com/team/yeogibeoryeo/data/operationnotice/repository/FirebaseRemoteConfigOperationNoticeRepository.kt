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
import java.util.concurrent.Executor
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
internal constructor(
    private val getRemoteConfigString: (String) -> String,
    private val setConfigSettings: suspend (Long) -> Unit,
    private val setDefaults: suspend (Map<String, String>) -> Unit,
    private val fetchAndActivate: suspend () -> Unit,
    private val isDebug: Boolean = BuildConfig.DEBUG,
) : OperationNoticeRepository {
    private val refreshSignals = MutableStateFlow(0)
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Inject
    constructor() : this(
        getRemoteConfigString = { key -> Firebase.remoteConfig.getString(key) },
        setConfigSettings = { minimumFetchIntervalInSeconds ->
            Firebase.remoteConfig.setConfigSettingsAsync(
                remoteConfigSettings {
                    this.minimumFetchIntervalInSeconds = minimumFetchIntervalInSeconds
                },
            ).awaitRemoteConfigTask()
        },
        setDefaults = { defaults -> Firebase.remoteConfig.setDefaultsAsync(defaults).awaitRemoteConfigTask() },
        fetchAndActivate = { Firebase.remoteConfig.fetchAndActivate().awaitRemoteConfigTask() },
    )

    override fun observeOperationNotices(): Flow<List<OperationNotice>> =
        refreshSignals.map {
            parseOperationNotices(getRemoteConfigString(OPERATION_NOTICES_KEY))
        }

    override suspend fun refreshOperationNotices() {
        try {
            setConfigSettings(
                if (isDebug) {
                    DEBUG_MINIMUM_FETCH_INTERVAL_SECONDS
                } else {
                    RELEASE_MINIMUM_FETCH_INTERVAL_SECONDS
                },
            )
            setDefaults(
                mapOf(OPERATION_NOTICES_KEY to DEFAULT_OPERATION_NOTICES_JSON),
            )
            fetchAndActivate()
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

    private companion object {
        const val OPERATION_NOTICES_KEY = "operation_notices"
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val DEBUG_MINIMUM_FETCH_INTERVAL_SECONDS = 0L
        const val RELEASE_MINIMUM_FETCH_INTERVAL_SECONDS = 3600L
        const val DEFAULT_OPERATION_NOTICES_JSON = """{"schemaVersion":1,"notices":[]}"""
    }
}

internal suspend fun <T> Task<T>.awaitRemoteConfigTask(): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener(DirectExecutor) { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWith(Result.failure(task.exception ?: IllegalStateException()))
                }
            }
        }

private object DirectExecutor : Executor {
    override fun execute(command: Runnable) {
        command.run()
    }
}

