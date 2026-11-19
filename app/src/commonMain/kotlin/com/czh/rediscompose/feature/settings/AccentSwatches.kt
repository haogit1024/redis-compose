package com.czh.rediscompose.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppTooltip
import com.czh.rediscompose.design.theme.AppColors
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.domain.settings.Accent

/** Five accent dots; the selected one gets a contrast ring and an inner dot. */
@Composable
fun AccentSwatches(
    selected: Accent,
    onSelect: (Accent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dimens.settings.accentSwatchGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Accent.entries.forEach { accent ->
            AppTooltip(accent.displayName()) {
                AccentSwatch(
                    accent = accent,
                    selected = accent == selected,
                    onClick = { onSelect(accent) },
                )
            }
        }
    }
}

@Composable
private fun AccentSwatch(
    accent: Accent,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val base = remember(accent, colors.isDark) { AppColors.forTheme(colors.isDark, accent).accent }
    val ring = 2.dp
    Box(
        modifier =
            Modifier
                .size(dimens.settings.accentSwatchSize)
                .drawBehind {
                    if (selected) {
                        val px = ring.toPx()
                        drawCircle(
                            color = colors.surface,
                            radius = size.minDimension / 2f + px,
                            center = Offset(size.width / 2f, size.height / 2f),
                        )
                    }
                }
                .background(base, CircleShape)
                .border(
                    ring,
                    if (selected) colors.text else Color.Transparent,
                    CircleShape,
                )
                .clickable(onClick = onClick)
                .pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(dimens.dotSize + 2.dp)
                    .background(colors.swatchDot, CircleShape),
            )
        }
    }
}

internal fun Accent.displayName(): String =
    when (this) {
        Accent.IRIS -> "Iris"
        Accent.MINT -> "Mint"
        Accent.SKY -> "Sky"
        Accent.AMBER -> "Amber"
        Accent.ROSE -> "Rose"
    }

@Preview(widthDp = 240, heightDp = 80)
@Composable
private fun AccentSwatchesPreview() {
    AppTheme(dark = true) {
        Box(Modifier.background(LocalAppColors.current.surface).padding(16.dp)) {
            AccentSwatches(selected = Accent.IRIS, onSelect = {})
        }
    }
}

@Preview(widthDp = 240, heightDp = 80)
@Composable
private fun AccentSwatchesLightPreview() {
    AppTheme(dark = false, accent = Accent.MINT) {
        Box(Modifier.background(LocalAppColors.current.surface).padding(16.dp)) {
            AccentSwatches(selected = Accent.MINT, onSelect = {})
        }
    }
}
