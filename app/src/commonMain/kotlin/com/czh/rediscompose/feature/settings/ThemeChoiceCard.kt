package com.czh.rediscompose.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppColors
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.settings.Accent
import com.czh.rediscompose.domain.settings.ThemeMode

/** Theme card with a Compose-drawn mini window (no screenshots). */
@Composable
fun ThemeChoiceCard(
    choice: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
    accent: Accent = Accent.IRIS,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(dimens.radiusMd)
    val ring = 2.dp
    Column(
        modifier =
            modifier
                .width(dimens.settings.themeCardWidth)
                .drawBehind {
                    if (selected) {
                        val px = ring.toPx()
                        drawRoundRect(
                            color = colors.accentSoft,
                            topLeft = Offset(-px, -px),
                            size = Size(size.width + px * 2, size.height + px * 2),
                            cornerRadius = CornerRadius(dimens.radiusMd.toPx() + px),
                        )
                    }
                }
                .background(if (hovered) colors.surface3 else colors.surface2, shape)
                .border(
                    1.dp,
                    when {
                        selected -> colors.accent
                        hovered -> colors.borderStrong
                        else -> colors.border
                    },
                    shape,
                )
                .hoverable(interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                )
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(dimens.settings.themeCardPadding),
    ) {
        ThemeThumbnail(choice = choice, accent = accent, modifier = Modifier.fillMaxWidth())
        Row(
            modifier = Modifier.padding(top = dimens.spaceXs + 2.dp, start = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 1.dp),
        ) {
            Icon(
                imageVector = choice.icon(),
                contentDescription = null,
                tint = if (selected) colors.text else colors.textTertiary,
                modifier = Modifier.size(dimens.iconXs),
            )
            Text(
                text = choice.label(),
                style = typography.caption,
                color = if (selected) colors.text else colors.textTertiary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ThemeThumbnail(
    choice: ThemeMode,
    accent: Accent,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalAppDimens.current
    val dark = AppColors.forTheme(dark = true, accent = accent)
    val light = AppColors.forTheme(dark = false, accent = accent)
    val shape = RoundedCornerShape(dimens.settings.themeThumbRadius)
    val background =
        when (choice) {
            ThemeMode.DARK -> Brush.linearGradient(listOf(dark.background, dark.background))
            ThemeMode.LIGHT -> Brush.linearGradient(listOf(light.background, light.background))
            ThemeMode.SYSTEM ->
                Brush.horizontalGradient(
                    0f to light.background,
                    0.5f to light.background,
                    0.5f to dark.background,
                    1f to dark.background,
                )
        }
    val railBackground =
        when (choice) {
            ThemeMode.DARK -> Brush.linearGradient(listOf(dark.surface, dark.surface))
            ThemeMode.LIGHT -> Brush.linearGradient(listOf(light.surface, light.surface))
            ThemeMode.SYSTEM ->
                Brush.verticalGradient(
                    0f to light.surface,
                    0.5f to light.surface,
                    0.5f to dark.surface,
                    1f to dark.surface,
                )
        }
    val borderColor =
        when (choice) {
            ThemeMode.DARK -> dark.border
            ThemeMode.LIGHT -> light.border
            ThemeMode.SYSTEM -> light.border
        }
    Box(
        modifier =
            modifier
                .height(dimens.settings.themeThumbHeight)
                .clip(shape)
                .background(background)
                .border(1.dp, borderColor, shape),
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .width(dimens.settings.themeThumbRailWidth)
                    .fillMaxHeight()
                    .background(railBackground),
            )
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(dimens.settings.themeThumbPadding),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceXs - 1.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.55f)
                        .height(dimens.settings.themeThumbLineHeight)
                        .background(
                            Brush.horizontalGradient(listOf(light.accent, dark.accent)),
                            RoundedCornerShape(1.dp),
                        ),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(dimens.settings.themeThumbLineHeight)
                        .background(
                            Brush.horizontalGradient(
                                if (choice == ThemeMode.LIGHT) {
                                    listOf(light.borderStrong, light.borderStrong)
                                } else {
                                    listOf(dark.borderStrong, dark.borderStrong)
                                },
                            ),
                            RoundedCornerShape(1.dp),
                        ),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(dimens.settings.themeThumbPanelHeight)
                        .background(
                            when (choice) {
                                ThemeMode.DARK -> dark.surface2
                                ThemeMode.LIGHT -> light.surface
                                ThemeMode.SYSTEM -> dark.surface2
                            },
                            RoundedCornerShape(dimens.settings.themeThumbInnerRadius),
                        )
                        .border(
                            1.dp,
                            if (choice == ThemeMode.LIGHT) light.border else dark.border,
                            RoundedCornerShape(dimens.settings.themeThumbInnerRadius),
                        ),
                )
            }
        }
    }
}

internal fun ThemeMode.label(): String =
    when (this) {
        ThemeMode.DARK -> "深色"
        ThemeMode.LIGHT -> "浅色"
        ThemeMode.SYSTEM -> "跟随系统"
    }

internal fun ThemeMode.icon(): ImageVector =
    when (this) {
        ThemeMode.DARK -> AppIcons.ThemeDark
        ThemeMode.LIGHT -> AppIcons.ThemeLight
        ThemeMode.SYSTEM -> AppIcons.ThemeSystem
    }

@Preview(widthDp = 480, heightDp = 160)
@Composable
private fun ThemeChoiceCardPreview() {
    AppTheme(dark = true) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeChoiceCard(ThemeMode.DARK, selected = true, onClick = {})
            ThemeChoiceCard(ThemeMode.LIGHT, selected = false, onClick = {})
            ThemeChoiceCard(ThemeMode.SYSTEM, selected = false, onClick = {})
        }
    }
}

@Preview(widthDp = 480, heightDp = 160)
@Composable
private fun ThemeChoiceCardLightPreview() {
    AppTheme(dark = false) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeChoiceCard(ThemeMode.DARK, selected = false, onClick = {})
            ThemeChoiceCard(ThemeMode.LIGHT, selected = true, onClick = {})
            ThemeChoiceCard(ThemeMode.SYSTEM, selected = false, onClick = {})
        }
    }
}
