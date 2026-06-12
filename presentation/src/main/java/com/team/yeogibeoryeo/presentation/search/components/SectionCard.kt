package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import com.team.yeogibeoryeo.domain.item.model.DisposalGuideSectionRow
import com.team.yeogibeoryeo.presentation.common.text.withKoreanLineBreakOpportunities
import com.team.yeogibeoryeo.presentation.search.ItemSearchLayoutDefaults

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
    val spacing = ItemSearchLayoutDefaults.spacing
    val size = ItemSearchLayoutDefaults.size
    val stroke = ItemSearchLayoutDefaults.stroke
    val elevation = ItemSearchLayoutDefaults.elevation

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(stroke.outline, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.none)
    ) {
        Column(
            modifier = Modifier.padding(spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
            if (lines.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    lines.forEachIndexed { index, line ->
                        Text(
                            text =
                                (if (numbered) "${index + 1}. $line" else line)
                                    .withKoreanLineBreakOpportunities(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                lineBreak = koreanBodyLineBreak,
                            ),
                        )
                    }
                }
            }

            if (rows.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        ) {
                            Text(
                                text = row.label,
                                modifier = Modifier.widthIn(
                                    min = size.infoLabelMinWidth,
                                    max = size.infoLabelMaxWidth,
                                ),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    lineBreak = koreanBodyLineBreak,
                                ),
                            )
                            Text(
                                text = row.value,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
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
