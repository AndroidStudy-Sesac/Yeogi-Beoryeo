package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.team.yeogibeoryeo.domain.item.model.DisposalGuideSectionRow
import com.team.yeogibeoryeo.presentation.common.text.withKoreanLineBreakOpportunities

/**
 * 부가적인 정보를 담는 카드 컴포넌트입니다.
 * 테마의 SurfaceVariant 컬러를 사용하여 배경과 구분합니다.
 */
@Composable
fun SectionCard(
    title: String,
    lines: List<String>,
    modifier: Modifier = Modifier,
    numbered: Boolean = false,
    rows: List<DisposalGuideSectionRow> = emptyList(),
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
            if (lines.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    lines.forEachIndexed { index, line ->
                        Text(
                            text =
                                (if (numbered) "${index + 1}. $line" else line)
                                    .withKoreanLineBreakOpportunities(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                lineHeight = 22.sp,
                                fontSize = 15.sp,
                                lineBreak = koreanBodyLineBreak,
                            ),
                        )
                    }
                }
            }

            if (rows.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = row.label,
                                modifier = Modifier.widthIn(min = 88.dp, max = 112.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 21.sp,
                                    fontSize = 14.sp,
                                    lineBreak = koreanBodyLineBreak,
                                ),
                            )
                            Text(
                                text = row.value,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                    lineHeight = 22.sp,
                                    fontSize = 15.sp,
                                    lineBreak = koreanBodyLineBreak,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

private val koreanBodyLineBreak =
    LineBreak(
        strategy = LineBreak.Strategy.Simple,
        strictness = LineBreak.Strictness.Loose,
        wordBreak = LineBreak.WordBreak.Unspecified,
    )
