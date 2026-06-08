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
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionSearchCandidateUiModel

@Composable
fun RegionalGuideAmbiguousResult(
    candidates: List<RegionSearchCandidateUiModel>,
    onCandidateClick: (RegionSearchCandidateUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val validCandidates = candidates.filter { candidate -> candidate.isValid }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

    RegionalGuideCandidateList(
        candidates = validCandidates,
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
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
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
private fun RegionalGuideAmbiguousResultPreview() {
    MaterialTheme {
        RegionalGuideAmbiguousResult(
            candidates = listOf(
                RegionSearchCandidateUiModel(
                    sido = "서울특별시",
                    sigungu = "중구",
                    eupmyeondong = null,
                ),
                RegionSearchCandidateUiModel(
                    sido = "대구광역시",
                    sigungu = "중구",
                    eupmyeondong = null,
                ),
                RegionSearchCandidateUiModel(
                    sido = "인천광역시",
                    sigungu = "중구",
                    eupmyeondong = null,
                ),
                RegionSearchCandidateUiModel(
                    sido = "울산광역시",
                    sigungu = "중구",
                    eupmyeondong = null,
                ),
            ),
            onCandidateClick = {},
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
