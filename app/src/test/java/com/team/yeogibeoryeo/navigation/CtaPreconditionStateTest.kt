package com.team.yeogibeoryeo.navigation

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CtaPreconditionStateTest {
    private var isInternetAvailable = true
    private var hasFineLocationPermission = false
    private var hasCoarseLocationPermission = false
    private var isLocationServiceEnabled = true
    private var canOpenExternalUrl = true
    private val openedMapTypes = mutableListOf<CollectionSpotType>()
    private val openedUrls = mutableListOf<String>()
    private lateinit var state: CtaPreconditionState

    @Before
    fun setUp() {
        state = CtaPreconditionState(
            isInternetAvailable = { isInternetAvailable },
            hasFineLocationPermission = { hasFineLocationPermission },
            hasCoarseLocationPermission = { hasCoarseLocationPermission },
            isLocationServiceEnabled = { isLocationServiceEnabled },
            onOpenMap = openedMapTypes::add,
            onOpenExternalUrl = { url ->
                openedUrls += url
                canOpenExternalUrl
            },
        )
    }

    @Test
    fun `지도 CTA는 인터넷 연결 뒤 위치 권한 사전 안내를 표시한다`() {
        isInternetAvailable = false

        state.requestMap(CollectionSpotType.BATTERY_BIN)

        assertEquals(CtaPreconditionDialog.MapInternetRequired, state.dialog)

        isInternetAvailable = true
        state.confirmDialog()

        assertEquals(CtaPreconditionDialog.LocationPermissionRationale, state.dialog)
        assertTrue(openedMapTypes.isEmpty())
    }

    @Test
    fun `위치 권한 사전 안내의 계속하기는 시스템 권한 요청을 반환한다`() {
        state.requestMap(CollectionSpotType.BATTERY_BIN)

        val effect = state.confirmDialog()

        assertEquals(CtaPreconditionEffect.RequestLocationPermission, effect)
        assertNull(state.dialog)
    }

    @Test
    fun `정확한 위치 권한과 기기 위치가 준비되면 지도를 한 번 연다`() {
        hasFineLocationPermission = true

        state.requestMap(CollectionSpotType.FLUORESCENT_LAMP_BIN)

        assertEquals(
            listOf(CollectionSpotType.FLUORESCENT_LAMP_BIN),
            openedMapTypes,
        )
        assertNull(state.dialog)
    }

    @Test
    fun `대략적 위치만 허용하면 정확한 위치 업그레이드 안내 뒤 시스템 권한을 요청한다`() {
        state.requestMap(CollectionSpotType.SMALL_E_WASTE_BIN)
        state.confirmDialog()

        state.onLocationPermissionResult(
            isFineLocationGranted = false,
            isCoarseLocationGranted = true,
            canRequestAgain = true,
        )

        assertEquals(CtaPreconditionDialog.PreciseLocationRationale, state.dialog)
        assertEquals(CtaPreconditionEffect.RequestLocationPermission, state.confirmDialog())
    }

    @Test
    fun `이미 대략적 위치만 허용된 상태도 업그레이드 안내 뒤 시스템 권한을 요청한다`() {
        hasCoarseLocationPermission = true

        state.requestMap(CollectionSpotType.SMALL_E_WASTE_BIN)

        assertEquals(CtaPreconditionDialog.PreciseLocationRationale, state.dialog)
        assertEquals(CtaPreconditionEffect.RequestLocationPermission, state.confirmDialog())
    }

    @Test
    fun `정확한 위치 업그레이드 거절 뒤 다시 요청할 수 있다`() {
        hasCoarseLocationPermission = true
        state.requestMap(CollectionSpotType.SMALL_E_WASTE_BIN)
        state.confirmDialog()

        state.onLocationPermissionResult(
            isFineLocationGranted = false,
            isCoarseLocationGranted = true,
            canRequestAgain = true,
        )

        assertEquals(CtaPreconditionDialog.PreciseLocationDenied, state.dialog)
        assertEquals(CtaPreconditionEffect.RequestLocationPermission, state.confirmDialog())
    }

    @Test
    fun `정확한 위치 업그레이드를 다시 요청할 수 없으면 앱 설정을 안내한다`() {
        hasCoarseLocationPermission = true
        state.requestMap(CollectionSpotType.SMALL_E_WASTE_BIN)
        state.confirmDialog()

        state.onLocationPermissionResult(
            isFineLocationGranted = false,
            isCoarseLocationGranted = true,
            canRequestAgain = false,
        )

        assertEquals(CtaPreconditionDialog.PreciseLocationSettings, state.dialog)
        assertEquals(CtaPreconditionEffect.OpenAppSettings, state.confirmDialog())
    }

    @Test
    fun `일반 권한 거절은 이유 안내 뒤 다시 요청할 수 있다`() {
        state.requestMap(CollectionSpotType.CLOTHING_BIN)
        state.confirmDialog()

        state.onLocationPermissionResult(
            isFineLocationGranted = false,
            isCoarseLocationGranted = false,
            canRequestAgain = true,
        )

        assertEquals(CtaPreconditionDialog.LocationPermissionDenied, state.dialog)
        assertEquals(CtaPreconditionEffect.RequestLocationPermission, state.confirmDialog())
    }

    @Test
    fun `영구 권한 거절은 앱 설정을 사용자가 선택하게 한다`() {
        state.requestMap(CollectionSpotType.MEDICINE_DROP_BOX)
        state.confirmDialog()

        state.onLocationPermissionResult(
            isFineLocationGranted = false,
            isCoarseLocationGranted = false,
            canRequestAgain = false,
        )

        assertEquals(CtaPreconditionDialog.LocationPermissionSettings, state.dialog)
        assertEquals(CtaPreconditionEffect.OpenAppSettings, state.confirmDialog())
        assertTrue(openedMapTypes.isEmpty())
    }

    @Test
    fun `앱 설정에서 정확한 위치를 허용하면 원래 지도 요청을 재개한다`() {
        state.requestMap(CollectionSpotType.MEDICINE_DROP_BOX)
        state.confirmDialog()
        state.onLocationPermissionResult(
            isFineLocationGranted = false,
            isCoarseLocationGranted = false,
            canRequestAgain = false,
        )
        state.confirmDialog()

        hasFineLocationPermission = true
        state.onResumeFromSettings()

        assertEquals(listOf(CollectionSpotType.MEDICINE_DROP_BOX), openedMapTypes)
        assertNull(state.dialog)
    }

    @Test
    fun `앱 설정에서 권한을 허용하지 않으면 팝업을 반복하지 않는다`() {
        state.requestMap(CollectionSpotType.MEDICINE_DROP_BOX)
        state.confirmDialog()
        state.onLocationPermissionResult(
            isFineLocationGranted = false,
            isCoarseLocationGranted = false,
            canRequestAgain = false,
        )
        state.confirmDialog()

        state.onResumeFromSettings()

        assertNull(state.dialog)
        assertTrue(openedMapTypes.isEmpty())
    }

    @Test
    fun `기기 위치 설정을 켜고 돌아오면 원래 지도 요청을 재개한다`() {
        hasFineLocationPermission = true
        isLocationServiceEnabled = false
        state.requestMap(CollectionSpotType.ICE_PACK_BIN)

        assertEquals(CtaPreconditionDialog.LocationServiceDisabled, state.dialog)
        assertEquals(CtaPreconditionEffect.OpenLocationSettings, state.confirmDialog())

        isLocationServiceEnabled = true
        state.onResumeFromSettings()

        assertEquals(listOf(CollectionSpotType.ICE_PACK_BIN), openedMapTypes)
    }

    @Test
    fun `외부 URL은 인터넷 연결 뒤 처음 선택한 주소를 연다`() {
        isInternetAvailable = false
        val url = "https://example.com/guide"

        state.requestExternalUrl(url)
        assertEquals(CtaPreconditionDialog.ExternalUrlInternetRequired, state.dialog)

        isInternetAvailable = true
        state.confirmDialog()

        assertEquals(listOf(url), openedUrls)
        assertNull(state.dialog)
    }

    @Test
    fun `외부 URL 실행 실패는 공통 안내를 표시한다`() {
        canOpenExternalUrl = false
        val url = "https://example.com/guide"

        state.requestExternalUrl(url)

        assertEquals(listOf(url), openedUrls)
        assertEquals(CtaPreconditionDialog.ExternalUrlOpenFailed, state.dialog)
    }

    @Test
    fun `오프라인 안내를 취소하면 보류한 외부 URL을 열지 않는다`() {
        isInternetAvailable = false
        state.requestExternalUrl("https://example.com/guide")

        state.cancelPendingRequest()
        isInternetAvailable = true
        state.confirmDialog()

        assertTrue(openedUrls.isEmpty())
        assertNull(state.dialog)
    }
}
