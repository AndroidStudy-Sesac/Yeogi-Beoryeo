package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteSchedule
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetTodayRegionalWasteSummaryUseCaseTest {
    private val useCase = GetTodayRegionalWasteSummaryUseCase()

    @Test
    fun `today schedules are summarized by Asia Seoul date input`() {
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
                today = LocalDate.of(2026, 6, 15),
            )

        requireNotNull(summary)
        assertEquals("target-id", summary.targetId)
        assertEquals(listOf(RegionalWasteType.GENERAL.description), summary.wasteTypeNames)
        assertEquals("월, 수, 금", summary.disposalDays)
        assertEquals("18:00 이후", summary.disposalTime)
    }

    @Test
    fun `no matching weekday returns null`() {
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
                today = LocalDate.of(2026, 6, 15),
            )

        assertNull(summary)
    }

    @Test
    fun `blank days are not inferred as today schedules`() {
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
                today = LocalDate.of(2026, 6, 15),
            )

        assertNull(summary)
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
