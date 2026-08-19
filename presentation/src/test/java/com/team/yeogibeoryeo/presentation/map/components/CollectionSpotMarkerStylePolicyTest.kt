package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
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

    @Test
    fun `다크모드 cluster leaf 마커는 기본 마커 크기를 px로 변환해 사용한다`() {
        val defaultStyle = CollectionSpotMarkerStylePolicy.defaultStyle(
            isDarkTheme = true,
            lightColor = LightDefaultMarkerColor,
        )

        val markerSize = defaultStyle.toClusterLeafMarkerSize(
            density = TestDensity,
            sizeAuto = SizeAuto,
        )

        assertEquals(80, markerSize.width)
        assertEquals(106, markerSize.height)
    }

    @Test
    fun `라이트모드 cluster leaf 마커는 SDK 자동 크기를 유지한다`() {
        val defaultStyle = CollectionSpotMarkerStylePolicy.defaultStyle(
            isDarkTheme = false,
            lightColor = LightDefaultMarkerColor,
        )

        val markerSize = defaultStyle.toClusterLeafMarkerSize(
            density = TestDensity,
            sizeAuto = SizeAuto,
        )

        assertEquals(SizeAuto, markerSize.width)
        assertEquals(SizeAuto, markerSize.height)
    }

    @Test
    fun `Compose 기본 마커와 cluster leaf 마커는 같은 기본 스타일 크기 정책을 사용한다`() {
        val composeDefaultStyle = CollectionSpotMarkerStylePolicy.style(
            isSelected = false,
            isDarkTheme = true,
            lightDefaultColor = LightDefaultMarkerColor,
            lightSelectedColor = LightSelectedMarkerColor,
        )

        val clusterLeafMarkerSize =
            composeDefaultStyle.toClusterLeafMarkerSize(
                density = TestDensity,
                sizeAuto = SizeAuto,
            )

        assertEquals(40.dp, composeDefaultStyle.width)
        assertEquals(53.dp, composeDefaultStyle.height)
        assertEquals(80, clusterLeafMarkerSize.width)
        assertEquals(106, clusterLeafMarkerSize.height)
    }

    private companion object {
        val LightDefaultMarkerColor = Color(0xFF2E7D32)
        val LightSelectedMarkerColor = Color(0xFFF9A825)
        val TestDensity = Density(2f)
        const val SizeAuto = -1
    }
}
