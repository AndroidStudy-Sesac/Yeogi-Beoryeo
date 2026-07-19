package com.team.yeogibeoryeo.presentation.regionalguide

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegionalGuideInitialRouteRequestPolicyTest {
    @get:Rule
    val mainDispatcherRule = RegionalGuideMainDispatcherRule()

    @Test
    fun `같은 뷰모델이 초기 상태로 돌아와도 최초 경로 요청은 한 번만 처리한다`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.consumeInitialRouteRequest())

        viewModel.onRegionSelectionStarted()

        assertTrue(viewModel.uiState.value is RegionalGuideUiState.Idle)
        assertFalse(viewModel.consumeInitialRouteRequest())
    }

    @Test
    fun `새 뷰모델은 최초 경로 요청을 다시 처리한다`() = runTest {
        val existingViewModel = createViewModel()
        val recreatedViewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(existingViewModel.consumeInitialRouteRequest())
        assertTrue(recreatedViewModel.consumeInitialRouteRequest())
    }
}
