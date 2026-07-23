package com.team.yeogibeoryeo.appguide

import com.team.yeogibeoryeo.domain.appguide.repository.AppGuideRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class AppGuideViewModelTest {
    @get:Rule
    val mainDispatcherRule = AppGuideMainDispatcherRule()

    @Test
    fun `가이드를 완료하지 않았으면 첫 단계부터 자동으로 보여준다`() = runTest {
        val viewModel = AppGuideViewModel(FakeAppGuideRepository(completedVersion = 0))

        assertTrue(viewModel.uiState.value.isReady)
        assertTrue(viewModel.uiState.value.isVisible)
        assertEquals(AppGuideStep.ITEM_SEARCH, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `현재 버전 가이드를 완료했으면 자동으로 보여주지 않는다`() = runTest {
        val viewModel = AppGuideViewModel(
            FakeAppGuideRepository(completedVersion = CURRENT_APP_GUIDE_VERSION),
        )

        assertTrue(viewModel.uiState.value.isReady)
        assertFalse(viewModel.uiState.value.isVisible)
    }

    @Test
    fun `다음과 이전을 선택하면 정해진 순서로 단계를 이동한다`() = runTest {
        val viewModel = AppGuideViewModel(FakeAppGuideRepository(completedVersion = 0))

        viewModel.showNextStep()
        assertEquals(AppGuideStep.QUICK_CATEGORY, viewModel.uiState.value.currentStep)

        viewModel.showNextStep()
        assertEquals(AppGuideStep.USEFUL_GUIDE, viewModel.uiState.value.currentStep)

        viewModel.showNextStep()
        assertEquals(AppGuideStep.MAP, viewModel.uiState.value.currentStep)

        viewModel.showPreviousStep()
        assertEquals(AppGuideStep.USEFUL_GUIDE, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `마지막 단계에서 완료하면 현재 버전을 저장하고 가이드를 닫는다`() = runTest {
        val repository = FakeAppGuideRepository(completedVersion = 0)
        val viewModel = AppGuideViewModel(repository)
        repeat(AppGuideStep.entries.size) {
            viewModel.showNextStep()
        }

        assertFalse(viewModel.uiState.value.isVisible)
        assertEquals(CURRENT_APP_GUIDE_VERSION, repository.completedVersion.value)
    }

    @Test
    fun `설정에서 다시 시작하면 완료 여부와 관계없이 첫 단계부터 보여준다`() = runTest {
        val repository = FakeAppGuideRepository(completedVersion = CURRENT_APP_GUIDE_VERSION)
        val viewModel = AppGuideViewModel(repository)

        viewModel.startGuide()

        assertTrue(viewModel.uiState.value.isVisible)
        assertEquals(AppGuideStep.ITEM_SEARCH, viewModel.uiState.value.currentStep)
    }
}

private class FakeAppGuideRepository(
    completedVersion: Int,
) : AppGuideRepository {
    val completedVersion = MutableStateFlow(completedVersion)

    override fun observeCompletedVersion(): Flow<Int> = completedVersion

    override suspend fun markCompleted(version: Int) {
        completedVersion.value = maxOf(completedVersion.value, version)
    }

    override fun observeCompletedMapLocationGuideVersion(): Flow<Int> = MutableStateFlow(0)

    override suspend fun markMapLocationGuideCompleted(version: Int) = Unit

    override fun observeHasRequestedMapLocationPermission(): Flow<Boolean> = MutableStateFlow(false)

    override suspend fun markMapLocationPermissionRequested() = Unit

    override fun observeIsMapLocationPermissionBlocked(): Flow<Boolean> = MutableStateFlow(false)

    override suspend fun markMapLocationPermissionBlocked() = Unit

    override suspend fun clearMapLocationPermissionBlocked() = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppGuideMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
