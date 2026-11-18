package com.czh.rediscompose.design.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

data class AppMenuItem(
    val label: String,
    val icon: ImageVector? = null,
    val shortcut: String? = null,
    val danger: Boolean = false,
    val enabled: Boolean = true,
    val separatorBefore: Boolean = false,
    val onClick: () -> Unit = {},
)

@Composable
fun AppMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<AppMenuItem>,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopEnd,
    offset: DpOffset = DpOffset(0.dp, 4.dp),
) {
    if (!expanded) return
    val density = LocalDensity.current
    Popup(
        alignment = alignment,
        offset = with(density) { IntOffset(offset.x.roundToPx(), offset.y.roundToPx()) },
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        AppMenuSurface(items = items, modifier = modifier, onDismissRequest = onDismissRequest)
    }
}

@Composable
private fun AppMenuSurface(
    items: List<AppMenuItem>,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val shape = RoundedCornerShape(dimens.radiusMd)
    Column(
        modifier =
            modifier
                .defaultMinSize(minWidth = dimens.menuMinWidth)
                .shadow(elevation = 12.dp, shape = shape)
                .background(colors.surface3, shape)
                .border(1.dp, colors.borderStrong, shape)
                .padding(dimens.menuPadding),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        items.forEach { item ->
            if (item.separatorBefore) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimens.spaceXs + 1.dp, vertical = dimens.spaceXs - 1.dp)
                        .height(1.dp)
                        .background(colors.border),
                )
            }
            AppMenuItemRow(item = item, onDismissRequest = onDismissRequest)
        }
    }
}

@Composable
private fun AppMenuItemRow(
    item: AppMenuItem,
    onDismissRequest: () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val hoverProgress by animateFloatAsState(
        targetValue = if (hovered && item.enabled) 1f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "appMenuItemHover",
    )
    val shape = RoundedCornerShape(dimens.radiusXs)
    val hoverColor = if (item.danger) colors.danger.copy(alpha = 0.12f) else colors.accentSoft
    val background = lerp(Color.Transparent, hoverColor, hoverProgress)
    val baseText = if (item.danger) colors.danger else colors.textSecondary
    val contentColor = lerp(baseText, if (item.danger) colors.danger else colors.text, hoverProgress)
    val iconColor =
        lerp(
            if (item.danger) colors.danger else colors.textTertiary,
            if (item.danger) colors.danger else colors.accent,
            hoverProgress,
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(dimens.menuItemHeight)
                .clip(shape)
                .background(background)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = item.enabled,
                    role = Role.Button,
                    onClick = {
                        onDismissRequest()
                        item.onClick()
                    },
                )
                .then(if (item.enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                .padding(horizontal = dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm + 1.dp),
    ) {
        if (item.icon != null) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (item.enabled) iconColor else iconColor.copy(alpha = 0.45f),
                modifier = Modifier.size(dimens.iconSm),
            )
        }
        Text(
            text = item.label,
            style = typography.bodySmall,
            color = if (item.enabled) contentColor else contentColor.copy(alpha = 0.45f),
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        if (item.shortcut != null) {
            Text(
                text = item.shortcut,
                style = typography.monoMeta,
                color = colors.textQuaternary,
                maxLines = 1,
            )
        }
    }
}

@Preview
@Composable
private fun AppMenuPreview() {
    AppTheme(dark = true) {
        Box(
            Modifier
                .background(LocalAppColors.current.background)
                .padding(24.dp),
        ) {
            AppMenuSurface(
                items =
                    listOf(
                        AppMenuItem("复制 Key", AppIcons.Copy, shortcut = "⌘C"),
                        AppMenuItem("重命名", AppIcons.Edit),
                        AppMenuItem("设置 TTL", AppIcons.Clock),
                        AppMenuItem("删除 Key", AppIcons.Delete, danger = true, separatorBefore = true),
                    ),
            )
        }
    }
}
