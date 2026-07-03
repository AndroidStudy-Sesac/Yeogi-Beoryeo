package com.team.yeogibeoryeo.presentation.map

object MapLocationNotices {
    const val SpotSearchFailureMessage = "잠시 후 다시 시도하거나 네트워크 연결을 확인해 주세요."
    const val CurrentLocationSpotSearchFailureMessage =
        "네트워크 연결을 확인한 뒤 다시 시도하거나 직접 동/읍/면을 검색해 주세요."

    val PermissionDenied = MapLocationNotice(
        title = "위치 권한이 필요합니다.",
        message = "현재 위치 검색은 정확한 위치 권한을 허용하면 사용할 수 있어요. 직접 동/읍/면을 검색할 수도 있습니다.",
        action = MapLocationNoticeAction.OpenAppSettings,
    )

    val LocationServiceDisabled = MapLocationNotice(
        title = "위치 서비스가 꺼져 있습니다.",
        message = "기기의 위치 서비스가 꺼져 있어 현재 위치를 확인할 수 없어요. 위치 서비스를 켠 뒤 다시 시도하거나 직접 동/읍/면을 검색해 주세요.",
        action = MapLocationNoticeAction.OpenLocationSettings,
    )

    val CurrentLocationUnavailable = MapLocationNotice(
        title = "현재 위치를 확인하지 못했습니다.",
        message = "현재 위치를 확인하지 못했습니다. 잠시 후 다시 시도하거나 직접 동/읍/면을 검색해 주세요.",
    )
}
