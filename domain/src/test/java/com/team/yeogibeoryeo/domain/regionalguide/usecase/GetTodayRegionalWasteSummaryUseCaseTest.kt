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
    fun `regional guide schedules are summarized with general waste representative`() {
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
    fun `general waste schedule is selected as representative when waste schedules differ`() {
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
    fun `different flags are false when waste schedules have same days and time`() {
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
    fun `general waste schedule is selected as representative even when other waste has different days`() {
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
    fun `missing general waste schedule is not replaced with other waste representative`() {
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
    fun `unclear general waste days are not used as representative days`() {
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
    fun `general waste days are shown without today weekday matching`() {
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
    fun `blank general waste days use fallback representative days`() {
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
    fun `not applicable general waste days use fallback representative days`() {
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
    fun `unknown general waste days use fallback representative days`() {
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
