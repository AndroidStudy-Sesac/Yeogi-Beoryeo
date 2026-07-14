package com.team.yeogibeoryeo.presentation.regionalguide.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.regionalguide.RegionalGuideCandidateListScrollPosition
import com.team.yeogibeoryeo.presentation.regionalguide.regionalGuideCandidateListScrollKey
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideCandidateCollectionTypeHint
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideCandidateDistinguishingLabel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideCandidateDistinguishingText
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideCandidateUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideUiModel

@Composable
fun RegionalGuideCandidateResult(
    candidates: List<RegionalGuideCandidateUiModel>,
    onCandidateClick: (RegionalGuideCandidateUiModel) -> Unit,
    modifier: Modifier = Modifier,
    scrollStateKey: String = candidates.regionalGuideCandidateListScrollKey(),
    initialScrollPosition: RegionalGuideCandidateListScrollPosition =
        RegionalGuideCandidateListScrollPosition.Initial,
    onScrollPositionChange: (RegionalGuideCandidateListScrollPosition) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        RegionalGuideCandidateList(
            candidates = candidates,
            key = { candidate -> candidate.stableKey },
            onCandidateClick = onCandidateClick,
            scrollStateKey = scrollStateKey,
            initialScrollPosition = initialScrollPosition,
            onScrollPositionChange = onScrollPositionChange,
        ) { candidate ->
            RegionalGuideCandidateRowText(
                text = candidate.displayText,
                supportingText = null
            )
        }
    }
}

@Composable
fun RegionalGuideCollectionTypeCandidateResult(
    message: String,
    candidates: List<RegionalGuideCandidateUiModel>,
    onCandidateClick: (RegionalGuideCandidateUiModel) -> Unit,
    modifier: Modifier = Modifier,
    messageDescription: String? = null,
    sectionTitle: String? = null,
    selectedRegionText: String? = null,
) {
    RegionalGuideFallbackCandidatePanel(
        message = message,
        description = messageDescription,
        selectedRegionText = selectedRegionText,
        sectionTitle = sectionTitle,
        candidates = candidates,
        onCandidateClick = onCandidateClick,
        modifier = modifier,
    )
}

@Composable
private fun RegionalGuideCandidateRowText(
    text: String,
    supportingText: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 20.dp,
                top = 14.dp,
                bottom = 14.dp
            )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )

        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun RegionalGuideFallbackCandidatePanel(
    message: String,
    description: String?,
    selectedRegionText: String?,
    sectionTitle: String?,
    candidates: List<RegionalGuideCandidateUiModel>,
    onCandidateClick: (RegionalGuideCandidateUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
        ),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RegionalGuideFallbackPanelIntro(
                message = message,
                description = description,
            )

            if (selectedRegionText != null) {
                RegionalGuideFallbackSelectedRegion(
                    selectedRegionText = selectedRegionText
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = sectionTitle ?: stringResource(
                        id = R.string.regional_guide_candidate_fallback_section_title
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )

                RegionalGuideFallbackCandidateCards(
                    candidates = candidates,
                    onCandidateClick = onCandidateClick,
                )
            }
        }
    }
}

@Composable
private fun RegionalGuideFallbackPanelIntro(
    message: String,
    description: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )

        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RegionalGuideFallbackSelectedRegion(
    selectedRegionText: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(id = R.string.regional_guide_candidate_selected_region_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )

        Text(
            text = selectedRegionText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun RegionalGuideFallbackCandidateCards(
    candidates: List<RegionalGuideCandidateUiModel>,
    onCandidateClick: (RegionalGuideCandidateUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val duplicatedOptionTexts = candidates
        .groupingBy { candidate -> candidate.collectionTypeOptionText }
        .eachCount()
        .filterValues { count -> count > 1 }
        .keys

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        candidates.forEach { candidate ->
            key(candidate.stableKey) {
                RegionalGuideFallbackCandidateCard(
                    candidate = candidate,
                    showDistinguishingText = candidate.collectionTypeOptionText in duplicatedOptionTexts,
                    onClick = { onCandidateClick(candidate) },
                )
            }
        }
    }
}

@Composable
private fun RegionalGuideFallbackCandidateCard(
    candidate: RegionalGuideCandidateUiModel,
    showDistinguishingText: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
        ),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = candidate.collectionTypeOptionText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )

                candidate.collectionTypeSupportingText(enabled = true)?.let { supportingText ->
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (showDistinguishingText) {
                    candidate.collectionTypeDistinguishingText?.let { distinguishingText ->
                        Text(
                            text = distinguishingText.labelText(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = stringResource(id = R.string.regional_guide_candidate_select_action),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun RegionalGuideCandidateUiModel.collectionTypeSupportingText(
    enabled: Boolean
): String? {
    if (!enabled) return null

    return when (collectionTypeHint) {
        RegionalGuideCandidateCollectionTypeHint.DOOR_TO_DOOR ->
            stringResource(id = R.string.regional_guide_candidate_door_to_door_supporting_text)

        RegionalGuideCandidateCollectionTypeHint.BASE_POINT ->
            stringResource(id = R.string.regional_guide_candidate_base_point_supporting_text)

        null -> null
    }
}

@Composable
private fun RegionalGuideCandidateDistinguishingText.labelText(): String {
    val labelText = when (label) {
        RegionalGuideCandidateDistinguishingLabel.DISPOSAL_PLACE ->
            stringResource(id = R.string.regional_guide_candidate_distinguishing_disposal_place)

        RegionalGuideCandidateDistinguishingLabel.UNCOLLECTED_DAYS ->
            stringResource(id = R.string.regional_guide_candidate_distinguishing_uncollected_days)

        RegionalGuideCandidateDistinguishingLabel.SCHEDULE ->
            stringResource(id = R.string.regional_guide_candidate_distinguishing_schedule)

        RegionalGuideCandidateDistinguishingLabel.DEPARTMENT ->
            stringResource(id = R.string.regional_guide_candidate_distinguishing_department)
    }

    return stringResource(
        id = R.string.regional_guide_candidate_distinguishing_format,
        labelText,
        value,
    )
}

private fun previewFallbackCandidates(): List<RegionalGuideCandidateUiModel> =
    listOf(
        RegionalGuideCandidateUiModel(
            guide = RegionalGuideUiModel(
                regionName = "강원특별자치도 강릉시",
                managementZoneName = "거점수거 지역",
                targetRegionName = "거점수거 지역",
                disposalPlaceType = "거점수거",
                disposalPlaceDescription = "거점",
                schedules = emptyList(),
                uncollectedDays = null,
                departmentInfo = null
            ),
            sido = "강원특별자치도",
            sigungu = "강릉시",
            eupmyeondong = "사천면"
        ),
        RegionalGuideCandidateUiModel(
            guide = RegionalGuideUiModel(
                regionName = "강원특별자치도 강릉시",
                managementZoneName = "문전수거 지역",
                targetRegionName = "문전수거 지역",
                disposalPlaceType = "문전수거",
                disposalPlaceDescription = "문전",
                schedules = emptyList(),
                uncollectedDays = null,
                departmentInfo = null
            ),
            sido = "강원특별자치도",
            sigungu = "강릉시",
            eupmyeondong = "사천면"
        ),
    )

@Preview(
    name = "fallback 후보 안내",
    showBackground = true,
    widthDp = 360
)
@Composable
private fun RegionalGuideCandidateResultFallbackPreview() {
    MaterialTheme {
        RegionalGuideCollectionTypeCandidateResult(
            message = "직접 안내를 찾지 못했어요",
            messageDescription = "사천면의 직접 배출 안내가 없어 강릉시 기준 수거 유형을 선택해 주세요.",
            selectedRegionText = "강원특별자치도 > 강릉시 > 사천면",
            sectionTitle = "수거 유형 선택",
            candidates = previewFallbackCandidates(),
            onCandidateClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
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
