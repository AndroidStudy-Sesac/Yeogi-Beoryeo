package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteSchedule
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteType
import com.team.yeogibeoryeo.domain.regionalguide.model.TodayRegionalWasteSummaryResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetTodayRegionalWasteSummaryUseCaseTest {
    private val useCase = GetTodayRegionalWasteSummaryUseCase()

    @Test
    fun `지역 가이드 일정을 일반쓰레기 대표값 기준으로 요약한다`() {
        val guide =
            sampleGuide(
                schedules =
                    listOf(
                        sampleSchedule(
                            type = RegionalWasteType.GENERAL,
                            days = "월, 수, 금",
                            start = "18:00",
                        ),
                        sampleSchedule(
                            type = RegionalWasteType.RECYCLABLE,
                            days = "화, 목",
                            start = "19:00",
                        ),
                    ),
            )

        val summary =
            useCase(
                targetId = "target-id",
                regionName = "서울특별시 > 노원구 > 하계동",
                guide = guide,
            )

        require(summary is TodayRegionalWasteSummaryResult.Summary)
        summary.summary.run {
            assertEquals("target-id", targetId)
            assertEquals(
                listOf(
                    RegionalWasteType.GENERAL.description,
                    RegionalWasteType.RECYCLABLE.description,
                ),
                wasteTypeNames,
            )
            assertEquals("월, 수, 금", disposalDays)
            assertEquals("18:00 이후", disposalTime)
            assertTrue(hasDifferentDisposalDays)
            assertTrue(hasDifferentDisposalTime)
        }
    }

    @Test
    fun `품목별 일정이 다르면 일반쓰레기 일정을 대표값으로 선택한다`() {
        val guide =
            sampleGuide(
                schedules =
                    listOf(
                        sampleSchedule(
                            type = RegionalWasteType.FOOD,
                            days = "월, 화, 수",
                            start = "20:00",
                            end = "02:00",
                        ),
                        sampleSchedule(
                            type = RegionalWasteType.GENERAL,
                            days = "월, 수, 금",
                            start = "18:00",
                            end = "23:00",
                        ),
                    ),
            )

        val summary =
            useCase(
                targetId = "target-id",
                regionName = "서울특별시 > 노원구 > 하계동",
                guide = guide,
            )

        require(summary is TodayRegionalWasteSummaryResult.Summary)
        summary.summary.run {
            assertEquals("월, 수, 금", disposalDays)
            assertEquals("18:00 ~ 23:00", disposalTime)
            assertTrue(hasDifferentDisposalDays)
            assertTrue(hasDifferentDisposalTime)
        }
    }

    @Test
    fun `품목별 요일과 시간이 같으면 품목별 다름 상태가 꺼져 있다`() {
        val guide =
            sampleGuide(
                schedules =
                    listOf(
                        sampleSchedule(
                            type = RegionalWasteType.GENERAL,
                            days = "월, 수, 금",
                            start = "18:00",
                            end = "23:00",
                        ),
                        sampleSchedule(
                            type = RegionalWasteType.FOOD,
                            days = "월, 수, 금",
                            start = "18:00",
                            end = "23:00",
                        ),
                        sampleSchedule(
                            type = RegionalWasteType.RECYCLABLE,
                            days = "월, 수, 금",
                            start = "18:00",
                            end = "23:00",
                        ),
                    ),
            )

        val summary =
            useCase(
                targetId = "target-id",
                regionName = "서울특별시 > 노원구 > 하계동",
                guide = guide,
            )

        require(summary is TodayRegionalWasteSummaryResult.Summary)
        summary.summary.run {
            assertEquals("월, 수, 금", disposalDays)
            assertEquals("18:00 ~ 23:00", disposalTime)
            assertFalse(hasDifferentDisposalDays)
            assertFalse(hasDifferentDisposalTime)
        }
    }

    @Test
    fun `다른 품목의 요일이 달라도 일반쓰레기 일정을 대표값으로 선택한다`() {
        val guide =
            sampleGuide(
                schedules =
                    listOf(
                        sampleSchedule(
                            type = RegionalWasteType.GENERAL,
                            days = "화, 목",
                            start = "18:00",
                        ),
                        sampleSchedule(
                            type = RegionalWasteType.FOOD,
                            days = "월, 수, 금",
                            start = "20:00",
                        ),
                    ),
            )

        val summary =
            useCase(
                targetId = "target-id",
                regionName = "서울특별시 > 노원구 > 하계동",
                guide = guide,
            )

        require(summary is TodayRegionalWasteSummaryResult.Summary)
        summary.summary.run {
            assertEquals(
                listOf(
                    RegionalWasteType.GENERAL.description,
                    RegionalWasteType.FOOD.description,
                ),
                wasteTypeNames,
            )
            assertEquals("화, 목", disposalDays)
            assertEquals("18:00 이후", disposalTime)
            assertTrue(hasDifferentDisposalDays)
            assertTrue(hasDifferentDisposalTime)
        }
    }

    @Test
    fun `일반쓰레기 일정이 없으면 다른 품목 값을 대표값으로 대체하지 않는다`() {
        val guide =
            sampleGuide(
                schedules =
                    listOf(
                        sampleSchedule(
                            type = RegionalWasteType.FOOD,
                            days = "월, 수, 금",
                            start = "20:00",
                        ),
                    ),
            )

        val summary =
            useCase(
                targetId = "target-id",
                regionName = "서울특별시 > 노원구 > 하계동",
                guide = guide,
            )

        require(summary is TodayRegionalWasteSummaryResult.Summary)
        summary.summary.run {
            assertEquals(listOf(RegionalWasteType.FOOD.description), wasteTypeNames)
            assertNull(disposalDays)
            assertNull(disposalTime)
        }
    }

    @Test
    fun `일반쓰레기 요일이 불명확하면 대표 요일로 사용하지 않는다`() {
        val guide =
            sampleGuide(
                schedules =
                    listOf(
                        sampleSchedule(
                            type = RegionalWasteType.GENERAL,
                            days = "기타",
                            start = "18:00",
                        ),
                        sampleSchedule(
                            type = RegionalWasteType.FOOD,
                            days = "월, 수, 금",
                            start = "20:00",
                        ),
                    ),
            )

        val summary =
            useCase(
                targetId = "target-id",
                regionName = "서울특별시 > 노원구 > 하계동",
                guide = guide,
            )

        require(summary is TodayRegionalWasteSummaryResult.Summary)
        summary.summary.run {
            assertNull(disposalDays)
            assertEquals("18:00 이후", disposalTime)
        }
    }

    @Test
    fun `일반쓰레기 요일은 오늘 요일 매칭 없이 표시한다`() {
        val guide =
            sampleGuide(
                schedules =
                    listOf(
                        sampleSchedule(
                            type = RegionalWasteType.GENERAL,
                            days = "화, 목",
                        ),
                    ),
            )

        val summary =
            useCase(
                targetId = "target-id",
                regionName = "서울특별시 > 노원구 > 하계동",
                guide = guide,
            )

        require(summary is TodayRegionalWasteSummaryResult.Summary)
        summary.summary.run {
            assertEquals("화, 목", disposalDays)
            assertNull(disposalTime)
        }
    }

    @Test
    fun `일반쓰레기 요일이 공백이면 대표 요일 대체 문구를 사용한다`() {
        val guide =
            sampleGuide(
                schedules =
                    listOf(
                        sampleSchedule(
                            type = RegionalWasteType.GENERAL,
                            days = null,
                        ),
                    ),
            )

        val summary =
            useCase(
                targetId = "target-id",
                regionName = "서울특별시 > 노원구 > 하계동",
                guide = guide,
            )

        require(summary is TodayRegionalWasteSummaryResult.Summary)
        summary.summary.run {
            assertNull(disposalDays)
            assertNull(disposalTime)
        }
    }

    @Test
    fun `일반쓰레기 요일이 해당없음이면 대표 요일 대체 문구를 사용한다`() {
        val guide =
            sampleGuide(
                schedules =
                    listOf(
                        sampleSchedule(
                            type = RegionalWasteType.GENERAL,
                            days = "해당없음",
                            start = "18:00",
                        ),
                    ),
            )

        val summary =
            useCase(
                targetId = "target-id",
                regionName = "서울특별시 > 노원구 > 하계동",
                guide = guide,
            )

        require(summary is TodayRegionalWasteSummaryResult.Summary)
        summary.summary.run {
            assertNull(disposalDays)
            assertEquals("18:00 이후", disposalTime)
        }
    }

    @Test
    fun `일반쓰레기 요일이 미지정이면 대표 요일 대체 문구를 사용한다`() {
        val guide =
            sampleGuide(
                schedules =
                    listOf(
                        sampleSchedule(
                            type = RegionalWasteType.GENERAL,
                            days = "기타",
                            start = "18:00",
                        ),
                    ),
            )

        val summary =
            useCase(
                targetId = "target-id",
                regionName = "서울특별시 > 노원구 > 하계동",
                guide = guide,
            )

        require(summary is TodayRegionalWasteSummaryResult.Summary)
        summary.summary.run {
            assertNull(disposalDays)
            assertEquals("18:00 이후", disposalTime)
        }
    }

    private fun sampleGuide(
        schedules: List<RegionalWasteSchedule>,
    ): RegionalDisposalGuide =
        RegionalDisposalGuide(
            region = Region(sido = "서울특별시", sigungu = "노원구", eupmyeondong = "하계동"),
            schedules = schedules,
        )

    private fun sampleSchedule(
        type: RegionalWasteType,
        days: String?,
        start: String? = null,
        end: String? = null,
    ): RegionalWasteSchedule =
        RegionalWasteSchedule(
            wasteType = type,
            disposalDays = days,
            disposalStartTime = start,
            disposalEndTime = end,
        )
}
