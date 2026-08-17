package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.common.design.theme.MapMarkerDefaultDark
import com.team.yeogibeoryeo.common.design.theme.MapMarkerSelectedDark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionSpotMarkerStylePolicyTest {
    @Test
    fun `다크모드 기본 마커는 지도 전용 고대비 색상을 사용한다`() {
        val style = CollectionSpotMarkerStylePolicy.defaultStyle(
            isDarkTheme = true,
            lightColor = LightDefaultMarkerColor,
        )

        assertEquals(CollectionSpotMarkerIcon.Black, style.icon)
        assertEquals(MapMarkerDefaultDark, style.color)
        assertEquals(40.dp, style.width)
        assertEquals(53.dp, style.height)
        assertEquals(0, style.zIndex)
    }

    @Test
    fun `다크모드 선택 마커는 색상과 크기로 기본 마커와 구분한다`() {
        val defaultStyle = CollectionSpotMarkerStylePolicy.defaultStyle(
            isDarkTheme = true,
            lightColor = LightDefaultMarkerColor,
        )
        val selectedStyle = CollectionSpotMarkerStylePolicy.selectedStyle(
            isDarkTheme = true,
            lightColor = LightSelectedMarkerColor,
        )

        assertEquals(CollectionSpotMarkerIcon.Black, selectedStyle.icon)
        assertEquals(MapMarkerSelectedDark, selectedStyle.color)
        assertTrue(requireNotNull(selectedStyle.width) > requireNotNull(defaultStyle.width))
        assertTrue(requireNotNull(selectedStyle.height) > requireNotNull(defaultStyle.height))
        assertTrue(selectedStyle.zIndex > defaultStyle.zIndex)
        assertTrue(selectedStyle.isForceShowIcon)
    }

    @Test
    fun `라이트모드 마커는 기존 테마 색상과 자동 크기를 유지한다`() {
        val defaultStyle = CollectionSpotMarkerStylePolicy.defaultStyle(
            isDarkTheme = false,
            lightColor = LightDefaultMarkerColor,
        )
        val selectedStyle = CollectionSpotMarkerStylePolicy.selectedStyle(
            isDarkTheme = false,
            lightColor = LightSelectedMarkerColor,
        )

        assertEquals(CollectionSpotMarkerIcon.Default, defaultStyle.icon)
        assertEquals(CollectionSpotMarkerIcon.Default, selectedStyle.icon)
        assertEquals(LightDefaultMarkerColor, defaultStyle.color)
        assertEquals(LightSelectedMarkerColor, selectedStyle.color)
        assertNull(defaultStyle.width)
        assertNull(defaultStyle.height)
        assertNull(selectedStyle.width)
        assertNull(selectedStyle.height)
        assertTrue(selectedStyle.zIndex > defaultStyle.zIndex)
        assertFalse(selectedStyle.isForceShowIcon)
    }

    private companion object {
        val LightDefaultMarkerColor = Color(0xFF2E7D32)
        val LightSelectedMarkerColor = Color(0xFFF9A825)
    }
}
