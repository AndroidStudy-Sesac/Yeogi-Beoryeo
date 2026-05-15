package com.team.yeogibeoryeo.domain.item.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisposalRecyclabilityTest {
    @Test
    fun `재활용폐기물 배출방법은 재활용 가능이다`() {
        assertTrue(DisposalRecyclability.fromMethods(listOf("재활용폐기물")))
    }

    @Test
    fun `보증금 환급 배출방법은 재활용 가능이다`() {
        assertTrue(DisposalRecyclability.fromMethods(listOf("보증금 환급")))
    }

    @Test
    fun `역회수 배출방법은 재활용 가능이다`() {
        assertTrue(DisposalRecyclability.fromMethods(listOf("역회수")))
    }

    @Test
    fun `종량제봉투 단독 배출방법은 재활용 불가이다`() {
        assertFalse(DisposalRecyclability.fromMethods(listOf("종량제봉투")))
    }

    @Test
    fun `재활용 가능과 불가 배출방법이 섞이면 재활용 가능이다`() {
        assertTrue(DisposalRecyclability.fromMethods(listOf("종량제봉투", "재활용폐기물")))
    }

    @Test
    fun `빈 배출방법 목록은 재활용 불가이다`() {
        assertFalse(DisposalRecyclability.fromMethods(emptyList()))
    }

    @Test
    fun `알 수 없는 배출방법은 재활용 불가이다`() {
        assertFalse(DisposalRecyclability.fromMethods(listOf("대형폐기물")))
    }

    @Test
    fun `재활용 카테고리는 재활용 가능이다`() {
        listOf(
            DisposalCategory.PAPER,
            DisposalCategory.GLASS,
            DisposalCategory.METAL,
            DisposalCategory.PLASTIC,
            DisposalCategory.STYROFOAM,
            DisposalCategory.VINYL,
            DisposalCategory.ELECTRONICS,
            DisposalCategory.CLOTHING,
        ).forEach { category ->
            assertTrue("$category 카테고리는 재활용 가능이어야 합니다", DisposalRecyclability.fromCategory(category))
        }
    }

    @Test
    fun `비재활용 카테고리는 재활용 불가이다`() {
        listOf(
            DisposalCategory.FOOD_WASTE,
            DisposalCategory.LARGE_WASTE,
            DisposalCategory.HAZARDOUS,
            DisposalCategory.GENERAL,
            DisposalCategory.OTHER,
        ).forEach { category ->
            assertFalse("$category 카테고리는 재활용 불가여야 합니다", DisposalRecyclability.fromCategory(category))
        }
    }
}
