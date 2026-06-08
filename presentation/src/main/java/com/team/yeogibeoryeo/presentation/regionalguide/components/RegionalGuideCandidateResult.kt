package com.team.yeogibeoryeo.presentation.regionalguide.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideCandidateUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideUiModel

@Composable
fun RegionalGuideCandidateResult(
    candidates: List<RegionalGuideCandidateUiModel>,
    onCandidateClick: (RegionalGuideCandidateUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

    RegionalGuideCandidateList(
        candidates = candidates,
        key = { candidate -> candidate.displayText },
        onCandidateClick = onCandidateClick,
        modifier = modifier
    ) { candidate ->
        RegionalGuideCandidateRowText(text = candidate.displayText)
    }
}

@Composable
private fun RegionalGuideCandidateRowText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 20.dp,
                top = 14.dp,
                bottom = 14.dp
            )
    )
}

@Preview(showBackground = true)
@Composable
private fun RegionalGuideCandidateResultPreview() {
    MaterialTheme {
        RegionalGuideCandidateResult(
            candidates = listOf(
                RegionalGuideCandidateUiModel(
                    guide = previewGuide("범서, 온양, 웅촌, 언양, 삼남, 상북, 온산, 청량, 서생"),
                    sido = "울산광역시",
                    sigungu = "울주군",
                    eupmyeondong = null
                ),
                RegionalGuideCandidateUiModel(
                    guide = previewGuide("두동, 두서, 삼동"),
                    sido = "울산광역시",
                    sigungu = "울주군",
                    eupmyeondong = null
                ),
            ),
            onCandidateClick = {},
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

private fun previewGuide(
    targetRegionName: String
): RegionalGuideUiModel =
    RegionalGuideUiModel(
        regionName = "울산광역시 울주군",
        managementZoneName = "울산광역시 울주군",
        targetRegionName = targetRegionName,
        disposalPlaceType = "문전수거",
        disposalPlaceDescription = "문전",
        schedules = emptyList(),
        uncollectedDays = "토, 일",
        departmentInfo = "환경자원과"
    )
