package com.team.yeogibeoryeo.presentation.regionalguide.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.common.R as CommonR

@Composable
fun RegionalGuidePublicNoticeCta(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content: @Composable RowScope.() -> Unit = {
        Icon(
            painter = painterResource(id = CommonR.drawable.ic_action_search),
            contentDescription = null,
            modifier = Modifier.size(PublicNoticeCtaIconSize),
        )
        Spacer(modifier = Modifier.size(PublicNoticeCtaIconSpacing))
        Text(
            text = stringResource(R.string.regional_guide_public_notice_cta),
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
        )
        Icon(
            painter = painterResource(id = CommonR.drawable.ic_action_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(PublicNoticeCtaIconSize),
        )
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PublicNoticeCtaMinHeight),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        content = content,
    )
}

private val PublicNoticeCtaIconSize = 24.dp
private val PublicNoticeCtaIconSpacing = 12.dp
private val PublicNoticeCtaMinHeight = 56.dp

@Preview(showBackground = true)
@Composable
private fun RegionalGuidePublicNoticeCtaPreview() {
    MaterialTheme {
        RegionalGuidePublicNoticeCta(
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
