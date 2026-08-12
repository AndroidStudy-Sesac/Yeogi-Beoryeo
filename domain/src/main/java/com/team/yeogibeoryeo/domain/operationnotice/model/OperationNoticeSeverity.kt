package com.team.yeogibeoryeo.domain.operationnotice.model

enum class OperationNoticeSeverity(val remoteValue: String, val sortRank: Int) {
    INFO("info", 0),
    WARNING("warning", 1),
    CRITICAL("critical", 2),
    ;

    companion object {
        fun fromRemoteValue(value: String): OperationNoticeSeverity? =
            entries.firstOrNull { it.remoteValue == value }
    }
}

