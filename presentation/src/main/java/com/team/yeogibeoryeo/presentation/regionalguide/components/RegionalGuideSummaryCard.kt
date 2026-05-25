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
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideUiModel

@Composable
fun RegionalGuideSummaryCard(
    guide: RegionalGuideUiModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = guide.regionName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            guide.managementZoneName?.let { managementZoneName ->
                RegionalGuideInfoRow(
                    title = "관리구역",
                    value = managementZoneName
                )
            }

            guide.targetRegionName?.let { targetRegionName ->
                RegionalGuideInfoRow(
                    title = "대상지역",
                    value = targetRegionName
                )
            }

            guide.disposalPlaceType?.let { placeType ->
                RegionalGuideInfoRow(
                    title = "배출장소",
                    value = placeType
                )
            }

            guide.disposalPlaceDescription?.let { placeDescription ->
                RegionalGuideInfoRow(
                    title = "장소설명",
                    value = placeDescription
                )
            }

            guide.uncollectedDays?.let { uncollectedDays ->
                RegionalGuideInfoRow(
                    title = "미수거일",
                    value = uncollectedDays
                )
            }

            guide.departmentInfo?.let { departmentInfo ->
                RegionalGuideInfoRow(
                    title = "문의",
                    value = departmentInfo
                )
            }
        }
    }
}