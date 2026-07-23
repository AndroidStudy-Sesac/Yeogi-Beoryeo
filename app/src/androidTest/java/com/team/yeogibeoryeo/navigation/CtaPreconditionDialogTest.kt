package com.team.yeogibeoryeo.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import org.junit.Rule
import org.junit.Test

class CtaPreconditionDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 위치_권한_사전_안내는_시스템_권한과_구분되는_계속하기를_표시한다() {
        val state = CtaPreconditionState(
            isInternetAvailable = { true },
            hasFineLocationPermission = { false },
            hasCoarseLocationPermission = { false },
            isLocationServiceEnabled = { true },
            onOpenMap = {},
            onOpenExternalUrl = { true },
        ).apply {
            requestMap(CollectionSpotType.BATTERY_BIN)
        }

        composeTestRule.setContent {
            MaterialTheme {
                CtaPreconditionDialogHost(state = state)
            }
        }

        composeTestRule.onNodeWithText("정확한 위치가 필요해요").assertIsDisplayed()
        composeTestRule.onNodeWithText("계속하기").assertIsDisplayed()
        composeTestRule.onNodeWithText("위치 권한 허용").assertDoesNotExist()
    }
}
