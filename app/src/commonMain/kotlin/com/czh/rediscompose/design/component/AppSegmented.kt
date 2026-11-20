package com.czh.rediscompose.design.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

enum class AppSegmentedSize { Medium, Small }

@Composable
fun <T> AppSegmented(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    size: AppSegmentedSize = AppSegmentedSize.Medium,
    enabled: Boolean = true,
    label: (T) -> String = { it.toString() },
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val outerShape = RoundedCornerShape(dimens.radiusControl)
    Row(
        modifier =
            modifier
                .background(colors.surface2, outerShape)
                .border(1.dp, colors.border, outerShape)
                .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            AppSegmentedItem(
                text = label(option),
                selected = option == selected,
                onClick = { onSelect(option) },
                size = size,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun AppSegmentedItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    size: AppSegmentedSize,
    enabled: Boolean,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val isSmall = size == AppSegmentedSize.Small
    val shape = RoundedCornerShape(dimens.radiusXs)
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "appSegmentedSelection",
    )
    val activeBackground = if (colors.isDark) colors.surface4 else colors.surface
    val background = lerp(activeBackground.copy(alpha = 0f), activeBackground, progress)
    val contentColor = lerp(colors.textTertiary, colors.text, progress)
    val height = if (isSmall) dimens.segmentedItemHeightSmall else dimens.segmentedItemHeight

    Box(
        modifier =
            Modifier
                .height(height)
                .clip(shape)
                .shadow(elevation = if (selected && !colors.isDark) 1.dp else 0.dp, shape = shape)
                .background(background)
                .clickable(
                    enabled = enabled,
                    role = Role.Tab,
                    onClick = onClick,
                )
                .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = typography.segment,
            color = if (enabled) contentColor else contentColor.copy(alpha = 0.45f),
            maxLines = 1,
        )
    }
}

@Preview
@Composable
private fun AppSegmentedPreview() {
    AppTheme(dark = true) {
        var selected by remember { mutableStateOf("Auto") }
        Row(
            modifier =
                Modifier
                    .background(LocalAppColors.current.background)
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppSegmented(
                options = listOf("Auto", "JSON", "Text", "HEX"),
                selected = selected,
                onSelect = { selected = it },
            )
            AppSegmented(
                options = listOf("1h", "6h", "24h"),
                selected = "1h",
                onSelect = {},
                size = AppSegmentedSize.Small,
            )
        }
    }
}
