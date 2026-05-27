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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalWasteScheduleUiModel

@Composable
fun RegionalWasteScheduleCard(
    schedule: RegionalWasteScheduleUiModel,
    modifier: Modifier = Modifier
) {
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
