package com.team.yeogibeoryeo.domain.operationnotice.model

enum class OperationNoticeFeature(val remoteValue: String) {
    HOME("home"),
    COLLECTION_SPOT_MAP("collection_spot_map"),
    ;

    companion object {
        fun fromRemoteValue(value: String): OperationNoticeFeature? =
            entries.firstOrNull { it.remoteValue == value }
    }
}

