package com.team.yeogibeoryeo.presentation.map

import com.team.yeogibeoryeo.domain.appguide.repository.AppGuideRepository
import com.team.yeogibeoryeo.presentation.search.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapLocationGuideViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `지도 현재위치 가이드를 완료하지 않았으면 자동으로 보여준다`() = runTest {
        val viewModel = MapLocationGuideViewModel(FakeAppGuideRepository(completedVersion = 0))

        assertTrue(viewModel.uiState.value.isReady)
        assertTrue(viewModel.uiState.value.isVisible)
    }

    @Test
    fun `지도 현재위치 가이드 완료 버전을 읽기 전에는 준비되지 않은 상태를 유지한다`() = runTest {
        val completedVersionFlow = MutableSharedFlow<Int>()
        val viewModel = MapLocationGuideViewModel(
            FakeAppGuideRepository(completedVersionFlow = completedVersionFlow),
        )

        assertFalse(viewModel.uiState.value.isReady)

        completedVersionFlow.emit(0)

        assertTrue(viewModel.uiState.value.isReady)
        assertTrue(viewModel.uiState.value.isVisible)
    }

    @Test
    fun `지도 현재위치 가이드를 완료했으면 자동으로 보여주지 않는다`() = runTest {
        val viewModel = MapLocationGuideViewModel(
            FakeAppGuideRepository(completedVersion = CURRENT_MAP_LOCATION_GUIDE_VERSION),
        )

        assertTrue(viewModel.uiState.value.isReady)
        assertFalse(viewModel.uiState.value.isVisible)
    }

    @Test
    fun `지도 현재위치 가이드를 닫으면 현재 버전을 저장한다`() = runTest {
        val repository = FakeAppGuideRepository(completedVersion = 0)
        val viewModel = MapLocationGuideViewModel(repository)

        viewModel.dismissGuide()

        assertFalse(viewModel.uiState.value.isVisible)
        assertEquals(CURRENT_MAP_LOCATION_GUIDE_VERSION, repository.completedMapLocationGuideVersion.value)
    }

    @Test
    fun `지도 위치 권한 요청 이력을 저장한다`() = runTest {
        val repository = FakeAppGuideRepository(completedVersion = CURRENT_MAP_LOCATION_GUIDE_VERSION)
        val viewModel = MapLocationGuideViewModel(repository)

        viewModel.markLocationPermissionRequested()

        assertTrue(viewModel.uiState.value.hasRequestedLocationPermission)
        assertTrue(repository.hasRequestedLocationPermission.value)
    }

    @Test
    fun `지도 위치 권한 blocked 상태를 저장하고 해제한다`() = runTest {
        val repository = FakeAppGuideRepository(completedVersion = CURRENT_MAP_LOCATION_GUIDE_VERSION)
        val viewModel = MapLocationGuideViewModel(repository)

        viewModel.markLocationPermissionBlocked()
        assertTrue(viewModel.uiState.value.isLocationPermissionRequestBlocked)
        assertTrue(repository.isLocationPermissionRequestBlocked.value)

        viewModel.clearLocationPermissionBlocked()

        assertFalse(viewModel.uiState.value.isLocationPermissionRequestBlocked)
        assertFalse(repository.isLocationPermissionRequestBlocked.value)
    }
}

private class FakeAppGuideRepository(
    completedVersion: Int = 0,
    private val completedVersionFlow: Flow<Int>? = null,
) : AppGuideRepository {
    val completedMapLocationGuideVersion = MutableStateFlow(completedVersion)
    val hasRequestedLocationPermission = MutableStateFlow(false)
    val isLocationPermissionRequestBlocked = MutableStateFlow(false)

    override fun observeCompletedVersion(): Flow<Int> = MutableStateFlow(0)

    override suspend fun markCompleted(version: Int) = Unit

    override fun observeCompletedMapLocationGuideVersion(): Flow<Int> =
        completedVersionFlow ?: completedMapLocationGuideVersion

    override suspend fun markMapLocationGuideCompleted(version: Int) {
        completedMapLocationGuideVersion.value = maxOf(
            completedMapLocationGuideVersion.value,
            version,
        )
    }

    override fun observeHasRequestedMapLocationPermission(): Flow<Boolean> =
        hasRequestedLocationPermission

    override suspend fun markMapLocationPermissionRequested() {
        hasRequestedLocationPermission.value = true
    }

    override fun observeIsMapLocationPermissionBlocked(): Flow<Boolean> =
        isLocationPermissionRequestBlocked

    override suspend fun markMapLocationPermissionBlocked() {
        isLocationPermissionRequestBlocked.value = true
    }

    override suspend fun clearMapLocationPermissionBlocked() {
        isLocationPermissionRequestBlocked.value = false
    }
}
