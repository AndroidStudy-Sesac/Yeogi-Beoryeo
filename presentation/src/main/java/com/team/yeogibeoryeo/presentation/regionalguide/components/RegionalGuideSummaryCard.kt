package com.team.yeogibeoryeo.presentation.regionalguide.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalWasteScheduleUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.takeIfRegionalGuideDisplayValue
import com.team.yeogibeoryeo.common.R as CommonR

@Composable
fun RegionalGuideSummaryCard(
    guide: RegionalGuideUiModel,
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = guide.regionName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        painter = painterResource(
                            id = if (isFavorite) {
                                CommonR.drawable.ic_favorite_filled
                            } else {
                                CommonR.drawable.ic_favorite
                            },
                        ),
                        contentDescription = if (isFavorite) {
                            "즐겨찾기 해제"
                        } else {
                            "즐겨찾기 추가"
                        },
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            guide.managementZoneName.takeIfRegionalGuideDisplayValue()?.let { managementZoneName ->
                RegionalGuideInfoRow(
                    title = "관리구역",
                    value = managementZoneName
                )
            }

            guide.targetRegionName.takeIfRegionalGuideDisplayValue()?.let { targetRegionName ->
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

@Preview(showBackground = true)
@Composable
private fun RegionalGuideSummaryCardPreview() {
    MaterialTheme {
        RegionalGuideSummaryCard(
            guide = RegionalGuideUiModel(
                regionName = "서울특별시 영등포구 문래동",
                managementZoneName = "영등포구",
                targetRegionName = "문래동",
                disposalPlaceType = "문전수거",
                disposalPlaceDescription = "집 앞 지정 장소에 배출",
                schedules = listOf(
                    RegionalWasteScheduleUiModel(
                        wasteTypeName = "일반쓰레기",
                        disposalDays = "월, 수, 금",
                        disposalTime = "18:00 ~ 24:00",
                        disposalMethod = "종량제 봉투에 담아 배출",
                    ),
                ),
                uncollectedDays = "토요일",
                departmentInfo = "청소행정과 02-0000-0000",
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
