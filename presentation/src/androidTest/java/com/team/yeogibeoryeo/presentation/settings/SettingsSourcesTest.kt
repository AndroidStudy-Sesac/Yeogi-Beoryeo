package com.team.yeogibeoryeo.presentation.settings

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.ADMINISTRATIVE_CODE_URL
import com.team.yeogibeoryeo.presentation.common.DISPOSAL_API_URL
import com.team.yeogibeoryeo.presentation.common.DISPOSAL_PORTAL_URL
import com.team.yeogibeoryeo.presentation.common.PAPER_CUP_NOODLE_CONTAINER_URL
import com.team.yeogibeoryeo.presentation.common.PAPER_RECYCLING_GUIDE_URL
import com.team.yeogibeoryeo.presentation.common.RECYCLABLES_DISPOSAL_GUIDE_URL
import com.team.yeogibeoryeo.presentation.common.REGIONAL_WASTE_API_URL
import com.team.yeogibeoryeo.presentation.common.REGIONAL_WASTE_FILE_DATA_URL
import com.team.yeogibeoryeo.presentation.settings.detail.SourcesDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsSourcesTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 출처_화면에_정부기관_비제휴_안내가_표시된다() {
        val description = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.settings_sources_non_affiliation_description)

        setSourcesContent()

        composeTestRule
            .onNodeWithText(description)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun 출처_화면에_정부_공공데이터_출처와_이용_안내가_표시된다() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dataTitle = context.getString(R.string.settings_sources_data_title)
        val usageDescription = context.getString(R.string.settings_sources_usage_description)

        setSourcesContent()

        composeTestRule
            .onNodeWithText(dataTitle)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(usageDescription)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun 출처_화면의_정책_고지와_데이터_기준일이_정해진_내용을_포함한다() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dataTitle = context.getString(R.string.settings_sources_data_title)
        val usageDescription = context.getString(R.string.settings_sources_usage_description)
        val climateMinistryDescription =
            context.getString(R.string.settings_source_climate_ministry_description)
        val interiorMinistryDescription =
            context.getString(R.string.settings_source_interior_ministry_description)

        assertEquals("정부·공공데이터 정보 출처", dataTitle)
        assertTrue(
            usageDescription.contains(
                "정부 기관의 공식 판단이나 개별 지방자치단체의 최종 배출 지침을 대신하지 않습니다.",
            ),
        )
        assertTrue(
            usageDescription.contains(
                "실제 배출 전 해당 정부 기관 또는 지방자치단체의 최신 안내를 확인해 주세요.",
            ),
        )
        assertTrue(climateMinistryDescription.contains("전체 확인: 2026-05-31"))
        assertTrue(climateMinistryDescription.contains("최근 부분 확인: 2026-08-13"))
        assertTrue(climateMinistryDescription.contains("이용조건 확인일\n2026-07-13"))
        assertTrue(
            interiorMinistryDescription.contains(
                "파일데이터 기반 제공 가능 지역 메타데이터: 2026-07-16 기준",
            ),
        )
        assertTrue(interiorMinistryDescription.contains("지역별 배출 정보: API 조회 시점, 일간 업데이트"))
    }

    @Test
    fun 공식_출처_버튼은_해당_URL을_전달한다() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var openedUrl: String? = null
        val sources = listOf(
            R.string.settings_source_disposal_portal_link to DISPOSAL_PORTAL_URL,
            R.string.settings_source_paper_cup_noodle_container_link to PAPER_CUP_NOODLE_CONTAINER_URL,
            R.string.settings_source_disposal_api_link to DISPOSAL_API_URL,
            R.string.settings_source_paper_recycling_guide_link to PAPER_RECYCLING_GUIDE_URL,
            R.string.settings_source_recyclables_disposal_guide_link to RECYCLABLES_DISPOSAL_GUIDE_URL,
            R.string.settings_source_regional_waste_api_link to REGIONAL_WASTE_API_URL,
            R.string.settings_source_regional_waste_file_data_link to REGIONAL_WASTE_FILE_DATA_URL,
            R.string.settings_source_administrative_code_link to ADMINISTRATIVE_CODE_URL,
        )

        setSourcesContent(onOpenSourceClick = { openedUrl = it })

        sources.forEach { (labelResId, expectedUrl) ->
            composeTestRule
                .onNodeWithText(context.getString(labelResId))
                .performScrollTo()
                .performClick()

            assertEquals(expectedUrl, openedUrl)
        }
    }

    @Test
    fun 네이버_지도_고지_버튼은_전용_콜백을_호출한다() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var legalNoticeClicked = false
        var openSourceLicenseClicked = false

        setSourcesContent(
            onOpenNaverMapLegalNoticeClick = { legalNoticeClicked = true },
            onOpenNaverMapOpenSourceLicenseClick = { openSourceLicenseClicked = true },
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.settings_naver_map_legal_notice_action))
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithText(context.getString(R.string.settings_naver_map_open_source_license_action))
            .performScrollTo()
            .performClick()

        assertTrue(legalNoticeClicked)
        assertTrue(openSourceLicenseClicked)
    }

    private fun setSourcesContent(
        onOpenNaverMapLegalNoticeClick: () -> Unit = {},
        onOpenNaverMapOpenSourceLicenseClick: () -> Unit = {},
        onOpenSourceClick: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            MaterialTheme {
                LazyColumn {
                    item {
                        SourcesDetail(
                            onOpenNaverMapLegalNoticeClick = onOpenNaverMapLegalNoticeClick,
                            onOpenNaverMapOpenSourceLicenseClick =
                                onOpenNaverMapOpenSourceLicenseClick,
                            onOpenSourceClick = onOpenSourceClick,
                        )
                    }
                }
            }
        }
    }
}
