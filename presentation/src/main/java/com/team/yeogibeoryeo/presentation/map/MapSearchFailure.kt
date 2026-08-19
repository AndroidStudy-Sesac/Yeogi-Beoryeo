package com.team.yeogibeoryeo.presentation.map

import androidx.annotation.StringRes
import com.team.yeogibeoryeo.presentation.R
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class MapSearchFailure(
    val reason: MapSearchFailureReason,
    @param:StringRes val titleResId: Int,
    @param:StringRes val messageResId: Int,
    val canRetry: Boolean = true,
)

enum class MapSearchFailureReason {
    Network,
    ExternalService,
    Unknown,
}

object MapSearchFailures {
    fun fromThrowable(throwable: Throwable): MapSearchFailure {
        return when (throwable.toSearchFailureReason()) {
            MapSearchFailureReason.Network -> Network
            MapSearchFailureReason.ExternalService -> ExternalService
            MapSearchFailureReason.Unknown -> Unknown
        }
    }

    val Network = MapSearchFailure(
        reason = MapSearchFailureReason.Network,
        titleResId = R.string.map_search_network_failure_title,
        messageResId = R.string.map_search_network_failure_message,
    )

    val ExternalService = MapSearchFailure(
        reason = MapSearchFailureReason.ExternalService,
        titleResId = R.string.map_search_external_service_failure_title,
        messageResId = R.string.map_search_external_service_failure_message,
    )

    val Unknown = MapSearchFailure(
        reason = MapSearchFailureReason.Unknown,
        titleResId = R.string.map_search_unknown_failure_title,
        messageResId = R.string.map_search_unknown_failure_message,
    )
}

private fun Throwable.toSearchFailureReason(): MapSearchFailureReason {
    if (hasCause<UnknownHostException>() ||
        hasCause<ConnectException>() ||
        hasCause<NoRouteToHostException>()
    ) {
        return MapSearchFailureReason.Network
    }

    if (hasCause<SocketTimeoutException>() ||
        hasCauseBySimpleName("HttpException") ||
        hasMessageContaining("timeout") ||
        hasMessageContaining("timed out") ||
        hasPublicDataTemporaryServiceError()
    ) {
        return MapSearchFailureReason.ExternalService
    }

    return MapSearchFailureReason.Unknown
}

private inline fun <reified T : Throwable> Throwable.hasCause(): Boolean =
    causeChain().any { throwable -> throwable is T }

private fun Throwable.hasCauseBySimpleName(simpleName: String): Boolean =
    causeChain().any { throwable -> throwable::class.simpleName == simpleName }

private fun Throwable.hasMessageContaining(text: String): Boolean =
    causeChain().any { throwable ->
        throwable.message?.contains(text, ignoreCase = true) == true
    }

private fun Throwable.hasPublicDataTemporaryServiceError(): Boolean =
    causeChain().any { throwable ->
        val message = throwable.message.orEmpty()
        message.contains("수거 장소 API 오류", ignoreCase = true) &&
            message.contains("SERVICE ERROR", ignoreCase = true)
    }

private fun Throwable.causeChain(): Sequence<Throwable> =
    generateSequence(this) { throwable -> throwable.cause }
