package com.team.yeogibeoryeo.presentation.search.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.design.AppAccentColors
import com.team.yeogibeoryeo.presentation.common.text.withKoreanLineBreakOpportunities
import com.team.yeogibeoryeo.domain.item.model.DisposalInstruction

/**
 * '버리는 방법'을 강조하여 보여주는 카드 컴포넌트입니다.
 * 테마의 PrimaryContainer 컬러를 사용하여 강조 효과를 줍니다.
 */
@Composable
fun DisposalInstructionCard(
    instructions: List<DisposalInstruction>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppAccentColors.MainCyan,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.disposal_method_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
            )
            instructions.forEach { instruction ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = instruction.method.withKoreanLineBreakOpportunities(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 24.sp,
                            lineBreak = koreanBodyLineBreak,
                        ),
                    )
                    instruction.tip?.let {
                        Text(
                            text = it.withKoreanLineBreakOpportunities(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.84f),
                                lineHeight = 20.sp,
                                lineBreak = koreanBodyLineBreak,
                            ),
                        )
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

