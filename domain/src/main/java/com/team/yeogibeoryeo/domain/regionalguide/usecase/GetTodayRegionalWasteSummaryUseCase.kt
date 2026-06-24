package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.regionalguide.model.HomeRegionalGuideSummary
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteSchedule
import com.team.yeogibeoryeo.domain.regionalguide.model.TodayRegionalWasteSummaryResult
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GetTodayRegionalWasteSummaryUseCase
    @Inject
    constructor() {
        operator fun invoke(
            targetId: String,
            regionName: String,
            guide: RegionalDisposalGuide,
            today: LocalDate = LocalDate.now(SEOUL_ZONE_ID),
        ): TodayRegionalWasteSummaryResult {
            val todaySchedules =
                guide.schedules.filter { schedule ->
                    schedule.disposalDays.hasDay(today.dayOfWeek)
                }

            if (todaySchedules.isEmpty()) {
                return if (guide.schedules.any { schedule -> schedule.disposalDays.needsConfirmation() }) {
                    TodayRegionalWasteSummaryResult.NeedsScheduleConfirmation
                } else {
                    TodayRegionalWasteSummaryResult.NoTodaySchedule
                }
            }

            return TodayRegionalWasteSummaryResult.Summary(
                HomeRegionalGuideSummary(
                    targetId = targetId,
                    regionName = regionName,
                    wasteTypeNames =
                        todaySchedules
                            .map { schedule -> schedule.wasteType.description }
                            .distinct(),
                    disposalDays =
                        todaySchedules
                            .mapNotNull { schedule -> schedule.disposalDays?.trimToNull() }
                            .distinct()
                            .joinToString(DISPLAY_SEPARATOR),
                    disposalTime =
                        todaySchedules
                            .mapNotNull { schedule -> schedule.displayTime() }
                            .distinct()
                            .joinToString(DISPLAY_SEPARATOR)
                            .takeIf { it.isNotBlank() },
                ),
            )
        }

        private fun String?.hasDay(dayOfWeek: DayOfWeek): Boolean {
            val dayLabel = DAY_LABELS[dayOfWeek] ?: return false
            return this
                ?.split(',')
                ?.mapNotNull { day -> day.trimToNull() }
                ?.any { day -> day == dayLabel }
                ?: false
        }

        private fun String?.needsConfirmation(): Boolean =
            this
                ?.split(',')
                ?.mapNotNull { day -> day.trimToNull() }
                ?.any { day -> day in NEEDS_CONFIRMATION_DAY_LABELS }
                ?: false

        private fun RegionalWasteSchedule.displayTime(): String? {
            val start = disposalStartTime.trimToNull()
            val end = disposalEndTime.trimToNull()
            return when {
                start != null && end != null -> "$start ~ $end"
                start != null -> "$start 이후"
                end != null -> "$end 이전"
                else -> null
            }
        }

        private fun String?.trimToNull(): String? = this?.trim()?.takeIf { it.isNotBlank() }

        companion object {
            val SEOUL_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")

            private const val DISPLAY_SEPARATOR = " / "

            private val NEEDS_CONFIRMATION_DAY_LABELS = setOf("기타", "미지정")

            private val DAY_LABELS =
                mapOf(
                    DayOfWeek.MONDAY to "월",
                    DayOfWeek.TUESDAY to "화",
                    DayOfWeek.WEDNESDAY to "수",
                    DayOfWeek.THURSDAY to "목",
                    DayOfWeek.FRIDAY to "금",
                    DayOfWeek.SATURDAY to "토",
                    DayOfWeek.SUNDAY to "일",
                )
        }
    }
