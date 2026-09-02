package com.team.yeogibeoryeo.core.diagnostics

import com.google.firebase.crashlytics.CustomKeysAndValues
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.team.yeogibeoryeo.BuildConfig
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorContext
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorReporter
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class CrashlyticsNonFatalErrorReporter internal constructor(
    private val isDebug: Boolean,
    private val record: (Throwable, Map<String, String>) -> Unit,
) : NonFatalErrorReporter {

    @Inject
    constructor() : this(
        isDebug = BuildConfig.DEBUG,
        record = { error, keys ->
            val builder = CustomKeysAndValues.Builder()
            keys.forEach { (key, value) -> builder.putString(key, value) }
            FirebaseCrashlytics.getInstance().recordException(error, builder.build())
        },
    )

    override fun report(error: Throwable, context: NonFatalErrorContext) {
        if (error is CancellationException || error is Error) throw error
        if (isDebug) return

        val sanitizedError = SanitizedNonFatalException(context)
        val keys = mapOf(
            "failure_api" to context.api.name,
            "failure_stage" to context.stage.name,
            "failure_category" to context.category.name,
            "failure_http_status_class" to context.httpStatusClass.name,
            "failure_retry_count" to context.retryCount.name,
            "failure_partial_result" to context.isPartialResult.toString(),
        )

        try {
            record(sanitizedError, keys)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: RuntimeException) {
            // 진단 기록 실패가 기존 오류 처리를 바꾸거나 다시 기록을 시도하지 않게 합니다.
        }
    }
}

private class SanitizedNonFatalException(context: NonFatalErrorContext) : RuntimeException(
    "${context.api.name}/${context.stage.name}/${context.category.name}",
    null,
    false,
    false,
)
