package com.team.yeogibeoryeo.presentation.regionalguide.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalWasteScheduleUiModel

@Composable
fun RegionalWasteScheduleCard(
    schedule: RegionalWasteScheduleUiModel,
    disposalPlaces: List<String> = listOfNotNull(schedule.disposalPlace),
    modifier: Modifier = Modifier
) {
    val normalizedDisposalPlaces = disposalPlaces
        .map { disposalPlace -> disposalPlace.trim() }
        .filter { disposalPlace -> disposalPlace.isNotEmpty() }
        .distinct()
    val disposalPlaceExpansionKey = listOf(
        schedule.wasteTypeName,
        schedule.disposalDays.orEmpty(),
        schedule.disposalTime.orEmpty(),
        schedule.disposalMethod.orEmpty(),
        normalizedDisposalPlaces.joinToString(DISPOSAL_PLACE_KEY_DELIMITER),
    ).joinToString(DISPOSAL_PLACE_KEY_DELIMITER)
    var isDisposalPlaceExpanded by rememberSaveable(disposalPlaceExpansionKey) {
        mutableStateOf(false)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = schedule.wasteTypeName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            schedule.disposalDays?.let { disposalDays ->
                RegionalGuideInfoRow(
                    title = stringResource(id = R.string.regional_waste_schedule_days_label),
                    value = disposalDays
                )
            }

            schedule.disposalTime?.let { disposalTime ->
                RegionalGuideInfoRow(
                    title = stringResource(id = R.string.regional_waste_schedule_time_label),
                    value = disposalTime
                )
            }

            schedule.disposalMethod?.let { disposalMethod ->
                RegionalGuideInfoRow(
                    title = stringResource(id = R.string.regional_waste_schedule_method_label),
                    value = disposalMethod
                )
            }

            if (normalizedDisposalPlaces.size == 1) {
                RegionalGuideInfoRow(
                    title = stringResource(id = R.string.regional_waste_schedule_place_label),
                    value = normalizedDisposalPlaces.single()
                )
            }

            if (normalizedDisposalPlaces.size > 1) {
                RegionalGuideInfoRow(
                    title = stringResource(id = R.string.regional_waste_schedule_place_label),
                    value = stringResource(
                        id = R.string.regional_waste_schedule_place_count_format,
                        normalizedDisposalPlaces.size,
                    )
                )

                TextButton(
                    onClick = { isDisposalPlaceExpanded = !isDisposalPlaceExpanded },
                    modifier = Modifier.padding(top = 0.dp)
                ) {
                    Text(
                        text = stringResource(
                            id = if (isDisposalPlaceExpanded) {
                                R.string.regional_waste_schedule_place_collapse_action
                            } else {
                                R.string.regional_waste_schedule_place_expand_action
                            }
                        )
                    )
                }

                if (isDisposalPlaceExpanded) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        normalizedDisposalPlaces.forEach { disposalPlace ->
                            Text(
                                text = "- $disposalPlace",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val DISPOSAL_PLACE_KEY_DELIMITER = "\u001F"

@Preview(showBackground = true)
@Composable
private fun RegionalWasteScheduleCardPreview() {
    MaterialTheme {
        RegionalWasteScheduleCard(
            schedule = RegionalWasteScheduleUiModel(
                wasteTypeName = "일반쓰레기",
                disposalDays = "월, 수, 금",
                disposalTime = "18:00 ~ 24:00",
                disposalMethod = "종량제 봉투에 담아 배출",
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
