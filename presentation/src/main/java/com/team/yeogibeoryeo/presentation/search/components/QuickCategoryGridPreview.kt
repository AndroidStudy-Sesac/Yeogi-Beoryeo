package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.team.yeogibeoryeo.common.design.theme.YeogiBeoryeoTheme
import com.team.yeogibeoryeo.presentation.search.ItemSearchLayoutDefaults

@Preview(
    name = "Quick categories 320dp font 1.0",
    showBackground = true,
    widthDp = 320,
    fontScale = 1.0f,
)
@Preview(
    name = "Quick categories 320dp font 1.3",
    showBackground = true,
    widthDp = 320,
    fontScale = 1.3f,
)
@Preview(
    name = "Quick categories 320dp font 1.5",
    showBackground = true,
    widthDp = 320,
    fontScale = 1.5f,
)
@Composable
private fun QuickCategoryGridCompactPreview() {
    QuickCategoryGridPreviewContent()
}

@Preview(
    name = "Quick categories 360dp font 1.0",
    showBackground = true,
    widthDp = 360,
    fontScale = 1.0f,
)
@Preview(
    name = "Quick categories 360dp font 1.3",
    showBackground = true,
    widthDp = 360,
    fontScale = 1.3f,
)
@Preview(
    name = "Quick categories 360dp font 1.5",
    showBackground = true,
    widthDp = 360,
    fontScale = 1.5f,
)
@Composable
private fun QuickCategoryGridPhonePreview() {
    QuickCategoryGridPreviewContent()
}

@Preview(
    name = "Quick categories 384dp font 1.0",
    showBackground = true,
    widthDp = 384,
    fontScale = 1.0f,
)
@Preview(
    name = "Quick categories 384dp font 1.3",
    showBackground = true,
    widthDp = 384,
    fontScale = 1.3f,
)
@Preview(
    name = "Quick categories 384dp font 1.5",
    showBackground = true,
    widthDp = 384,
    fontScale = 1.5f,
)
@Composable
private fun QuickCategoryGridFourColumnBoundaryPreview() {
    QuickCategoryGridPreviewContent()
}

@Preview(
    name = "Quick categories 411dp font 1.0",
    showBackground = true,
    widthDp = 411,
    fontScale = 1.0f,
)
@Preview(
    name = "Quick categories 411dp font 1.3",
    showBackground = true,
    widthDp = 411,
    fontScale = 1.3f,
)
@Preview(
    name = "Quick categories 411dp font 1.5",
    showBackground = true,
    widthDp = 411,
    fontScale = 1.5f,
)
@Composable
private fun QuickCategoryGridLargePhonePreview() {
    QuickCategoryGridPreviewContent()
}

@Composable
private fun QuickCategoryGridPreviewContent() {
    YeogiBeoryeoTheme {
        QuickCategoryGrid(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ItemSearchLayoutDefaults.spacing.xl),
            onCategoryClick = {},
        )
    }
}
