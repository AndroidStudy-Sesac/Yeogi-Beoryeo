package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.regionalguide.model.HomeRegionalGuideSummary
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteSchedule
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteType
import com.team.yeogibeoryeo.domain.regionalguide.model.TodayRegionalWasteSummaryResult
import javax.inject.Inject

class GetTodayRegionalWasteSummaryUseCase
    @Inject
    constructor() {
        operator fun invoke(
            targetId: String,
            regionName: String,
            guide: RegionalDisposalGuide,
        ): TodayRegionalWasteSummaryResult {
            val representativeSchedule =
                guide.schedules.firstOrNull { schedule ->
                    schedule.wasteType == RegionalWasteType.GENERAL
                }

            return TodayRegionalWasteSummaryResult.Summary(
                HomeRegionalGuideSummary(
                    targetId = targetId,
                    regionName = regionName,
                    wasteTypeNames =
                        guide.schedules
                            .map { schedule -> schedule.wasteType.description }
                            .distinct(),
                    disposalDays = representativeSchedule?.disposalDays.representativeDaysOrNull(),
                    disposalTime = representativeSchedule?.displayTime(),
                    hasDifferentDisposalDays =
                        guide.schedules.hasDifferentValues { schedule ->
                            schedule.disposalDays?.trimToNull()
                        },
                    hasDifferentDisposalTime =
                        guide.schedules.hasDifferentValues { schedule ->
                            schedule.displayTime()
                        },
                ),
            )
        }

        private fun List<RegionalWasteSchedule>.hasDifferentValues(
            selector: (RegionalWasteSchedule) -> String?,
        ): Boolean =
            mapNotNull(selector)
                .distinct()
                .size > 1

        private fun String?.representativeDaysOrNull(): String? =
            trimToNull()
                ?.takeUnless { days ->
                    days.split(',')
                        .mapNotNull { day -> day.trimToNull() }
                        .any { day -> day in NEEDS_CONFIRMATION_DAY_LABELS }
                }

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
            private val NEEDS_CONFIRMATION_DAY_LABELS = setOf("기타", "미지정", "해당없음")
        }
    }
