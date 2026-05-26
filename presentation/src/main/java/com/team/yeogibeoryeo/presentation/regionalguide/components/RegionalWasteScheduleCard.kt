package com.team.yeogibeoryeo.presentation.regionalguide.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalWasteScheduleUiModel

@Composable
fun RegionalWasteScheduleCard(
    schedule: RegionalWasteScheduleUiModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = schedule.wasteTypeName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            RegionalGuideInfoRow(
                title = "요일",
                value = schedule.disposalDays
            )

            RegionalGuideInfoRow(
                title = "시간",
                value = schedule.disposalTime
            )

            RegionalGuideInfoRow(
                title = "방법",
                value = schedule.disposalMethod
            )
        }
    }
}