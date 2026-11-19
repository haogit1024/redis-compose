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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

@Composable
fun SettingsNav(
    active: SettingsSection,
    onSelect: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Column(
        modifier =
            modifier
                .width(dimens.settingsNavWidth)
                .fillMaxHeight()
                .background(colors.surface)
                .drawBehind {
                    drawRect(
                        color = colors.border,
                        topLeft = Offset(size.width - 1.dp.toPx(), 0f),
                        size = Size(1.dp.toPx(), size.height),
                    )
                }
                .padding(horizontal = dimens.settings.navPaddingH, vertical = dimens.settings.navPaddingV),
    ) {
        SettingsGroup.entries.forEachIndexed { index, group ->
            Text(
                text = group.label,
                style = typography.microLabel,
                color = colors.textQuaternary,
                modifier =
                    Modifier.padding(
                        start = dimens.settings.navItemPaddingH,
                        top = if (index == 0) dimens.spaceSm else dimens.settings.navGroupTop,
                        bottom = dimens.settings.navGroupBottom,
                    ),
            )
            SettingsSection.entries.filter { it.group == group }.forEach { section ->
                SettingsNavItem(
                    section = section,
                    active = section == active,
                    onClick = { onSelect(section) },
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier =
                Modifier
                    .padding(top = dimens.spaceLg)
                    .padding(horizontal = dimens.spaceSm)
                    .fillMaxWidth()
                    .background(colors.surface2, RoundedCornerShape(dimens.radiusMd))
                    .border(1.dp, colors.border, RoundedCornerShape(dimens.radiusMd))
                    .padding(dimens.spaceMd),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceXs)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.iconTextGap),
                ) {
                    Icon(
                        imageVector = AppIcons.Sparkles,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(dimens.iconXs),
                    )
                    Text(
                        text = "同步状态",
                        style = typography.bodyStrong,
                        color = colors.text,
                    )
                }
                Text(
                    text = "设置实时写入本地，不会上传到云端。",
                    style = typography.caption,
                    color = colors.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun SettingsNavItem(
    section: SettingsSection,
    active: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(dimens.settings.navItemRadius)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 1.dp)
                .height(dimens.settings.navItemHeight)
                .background(
                    when {
                        active -> colors.accentSoft
                        hovered -> colors.surface2
                        else -> Color.Transparent
                    },
                    shape,
                )
                .border(
                    1.dp,
                    if (active) colors.accent.copy(alpha = 0.20f) else Color.Transparent,
                    shape,
                )
                .hoverable(interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                )
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = dimens.settings.navItemPaddingH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.settings.navItemGap),
    ) {
        Icon(
            imageVector = section.icon(),
            contentDescription = null,
            tint =
                when {
                    active -> colors.accent
                    hovered -> colors.text
                    else -> colors.textTertiary
                },
            modifier = Modifier.size(dimens.iconSm),
        )
        Text(
            text = section.title,
            style = typography.body,
            color =
                when {
                    active -> colors.text
                    hovered -> colors.text
                    else -> colors.textTertiary
                },
            maxLines = 1,
        )
    }
}

internal fun SettingsSection.icon(): ImageVector =
    when (this) {
        SettingsSection.APPEARANCE -> AppIcons.ThemeLight
        SettingsSection.EDITOR -> AppIcons.Code
        SettingsSection.BROWSER -> AppIcons.Key
        SettingsSection.SECURITY -> AppIcons.Shield
        SettingsSection.SHORTCUTS -> AppIcons.Keyboard
        SettingsSection.ABOUT -> AppIcons.Info
    }

@Preview(widthDp = 208, heightDp = 640)
@Composable
private fun SettingsNavPreview() {
    AppTheme(dark = true) {
        SettingsNav(active = SettingsSection.APPEARANCE, onSelect = {})
    }
}

@Preview(widthDp = 208, heightDp = 640)
@Composable
private fun SettingsNavLightPreview() {
    AppTheme(dark = false) {
        SettingsNav(active = SettingsSection.SECURITY, onSelect = {})
    }
}
