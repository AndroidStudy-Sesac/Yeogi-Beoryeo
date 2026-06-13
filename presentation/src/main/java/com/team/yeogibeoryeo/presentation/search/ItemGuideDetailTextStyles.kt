package com.team.yeogibeoryeo.presentation.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle

@Composable
internal fun itemGuideDetailTextStyles(): ItemGuideDetailTextStyles {
    val useLargeFontLayout = LocalDensity.current.fontScale >= ItemGuideDetailTypographyBreakpoints.LargeFontScale

    return if (useLargeFontLayout) {
        ItemGuideDetailTextStyles(
            title = MaterialTheme.typography.headlineLarge,
            sectionTitle = MaterialTheme.typography.titleLarge,
            body = MaterialTheme.typography.titleMedium,
            emphasizedBody = MaterialTheme.typography.titleLarge,
            supportingBody = MaterialTheme.typography.titleMedium,
            subGuideTitle = MaterialTheme.typography.titleMedium,
        )
    } else {
        ItemGuideDetailTextStyles(
            title = MaterialTheme.typography.headlineMedium,
            sectionTitle = MaterialTheme.typography.titleMedium,
            body = MaterialTheme.typography.bodyLarge,
            emphasizedBody = MaterialTheme.typography.titleMedium,
            supportingBody = MaterialTheme.typography.bodyLarge,
            subGuideTitle = MaterialTheme.typography.titleSmall,
        )
    }
}

internal data class ItemGuideDetailTextStyles(
    val title: TextStyle,
    val sectionTitle: TextStyle,
    val body: TextStyle,
    val emphasizedBody: TextStyle,
    val supportingBody: TextStyle,
    val subGuideTitle: TextStyle,
)

private object ItemGuideDetailTypographyBreakpoints {
    const val LargeFontScale = 1.3f
}
