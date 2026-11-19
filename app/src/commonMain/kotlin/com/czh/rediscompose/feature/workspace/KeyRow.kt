package com.czh.rediscompose.feature.workspace

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppMenu
import com.czh.rediscompose.design.component.AppMenuItem
import com.czh.rediscompose.design.component.AppTooltip
import com.czh.rediscompose.design.component.AppTypeDot
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppColors
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.key.KeyNode
import com.czh.rediscompose.domain.key.KeyType
import com.czh.rediscompose.util.ShortcutLabels
import com.czh.rediscompose.util.formatCount
import com.czh.rediscompose.util.formatTtlCompact

internal fun KeyType.displayLabel(): String =
    when (this) {
        KeyType.STRING -> "String"
        KeyType.HASH -> "Hash"
        KeyType.LIST -> "List"
        KeyType.SET -> "Set"
        KeyType.ZSET -> "ZSet"
        KeyType.STREAM -> "Stream"
        KeyType.NONE -> "None"
    }

internal fun KeyType.displayColor(colors: AppColors): Color =
    when (this) {
        KeyType.STRING -> colors.typeString
        KeyType.HASH -> colors.typeHash
        KeyType.LIST -> colors.typeList
        KeyType.SET -> colors.typeSet
        KeyType.ZSET -> colors.typeZset
        KeyType.STREAM -> colors.typeStream
        KeyType.NONE -> colors.typeNone
    }

/** One selectable key row with type dot, TTL badge, pin star and copy menu. */
@Composable
internal fun KeyRow(
    key: String,
    displayName: String,
    depth: Int,
    type: KeyType?,
    ttlSeconds: Long?,
    showTtl: Boolean,
    pinned: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
    onTogglePinned: () -> Unit,
    onCopy: () -> Unit,
    onEnsurePreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var menuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(key) { onEnsurePreview() }

    val indent = dimens.treeBaseIndent + dimens.treeIndentStep * depth
    val shape = RoundedCornerShape(dimens.radiusXs)
    val pinVisible = pinned || hovered || selected
    val showTtlBadge = showTtl && ttlSeconds != null && ttlSeconds > 0
    val ttlColor = if (ttlSeconds != null && ttlSeconds <= TTL_DANGER_SECONDS) colors.danger else colors.textQuaternary
    val nameColor = if (selected || hovered) colors.text else colors.textSecondary
    val typeColor = type?.displayColor(colors) ?: colors.typeNone

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(dimens.keyRowHeight)
                .clip(shape)
                .background(
                    when {
                        selected -> colors.accentSoft
                        hovered -> colors.surface2
                        else -> Color.Transparent
                    },
                )
                .drawBehind {
                    if (depth > 0) {
                        drawRect(
                            color = colors.border,
                            topLeft = Offset((indent - dimens.treeGuideInset).toPx(), 0f),
                            size = Size(1.dp.toPx(), size.height),
                        )
                    }
                    if (selected) {
                        val inset = dimens.keyRowSelectionBarInset.toPx()
                        val barWidth = dimens.keyRowSelectionBarWidth.toPx()
                        drawRoundRect(
                            color = colors.accent,
                            topLeft = Offset(0f, inset),
                            size = Size(barWidth, size.height - inset * 2),
                            cornerRadius = CornerRadius(barWidth / 2f),
                        )
                    }
                }
                .hoverable(interaction)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onSelect,
                )
                .pointerInput(key) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                menuOpen = true
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(start = indent, end = dimens.keyRowPaddingEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.iconTextGap + 1.dp),
        ) {
            AppTypeDot(
                color = typeColor,
                size = dimens.dotSize,
            )
            Text(
                text = displayName,
                style = typography.monoKeyRow,
                color = nameColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showTtlBadge) {
                Text(
                    text = formatTtlCompact(ttlSeconds),
                    style = typography.monoMicro,
                    color = ttlColor,
                    maxLines = 1,
                )
            }
            Box(
                modifier =
                    Modifier
                        .size(dimens.keyPinButtonSize)
                        .clip(RoundedCornerShape(dimens.radiusChip))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = pinVisible,
                            onClick = onTogglePinned,
                        )
                        .pointerHoverIcon(if (pinVisible) PointerIcon.Hand else PointerIcon.Default),
                contentAlignment = Alignment.Center,
            ) {
                if (pinVisible) {
                    AppTooltip(if (pinned) "取消置顶" else "置顶") {
                        Icon(
                            imageVector = if (pinned) AppIcons.StarFilled else AppIcons.Star,
                            contentDescription = if (pinned) "取消置顶" else "置顶",
                            tint = if (pinned) colors.warning else colors.textTertiary,
                            modifier = Modifier.size(dimens.iconXs),
                        )
                    }
                }
            }
        }
        AppMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            items =
                listOf(
                    AppMenuItem(
                        label = "复制 Key",
                        icon = AppIcons.Copy,
                        shortcut = ShortcutLabels.commandKey("C"),
                        onClick = onCopy,
                    ),
                ),
            alignment = Alignment.TopStart,
        )
    }
}

/** Expandable directory row in tree view. */
@Composable
internal fun KeyDirectoryRow(
    node: KeyNode,
    depth: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "treeChevron",
    )
    val indent = dimens.treeBaseIndent + dimens.treeIndentStep * depth

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(dimens.treeFolderHeight)
                .clip(RoundedCornerShape(dimens.radiusXs))
                .background(if (hovered) colors.surface2 else Color.Transparent)
                .drawBehind {
                    if (depth > 0) {
                        drawRect(
                            color = colors.border,
                            topLeft = Offset((indent - dimens.treeGuideInset).toPx(), 0f),
                            size = Size(1.dp.toPx(), size.height),
                        )
                    }
                }
                .hoverable(interaction)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onToggle,
                )
                .padding(start = indent, end = dimens.keyRowPaddingEnd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.iconTextGap),
    ) {
        Icon(
            imageVector = AppIcons.ChevronRight,
            contentDescription = if (expanded) "收起目录" else "展开目录",
            tint = colors.textQuaternary,
            modifier =
                Modifier
                    .size(dimens.treeChevronSize)
                    .rotate(chevronRotation),
        )
        Icon(
            imageVector = if (expanded) AppIcons.FolderOpen else AppIcons.Folder,
            contentDescription = null,
            tint = if (expanded) colors.warning else colors.textTertiary,
            modifier = Modifier.size(dimens.treeFolderIconSize),
        )
        Text(
            text = node.name,
            style = typography.bodySmall,
            color = if (hovered) colors.text else colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatCount(node.count.toLong()),
            style = typography.monoMicro,
            color = colors.textQuaternary,
            maxLines = 1,
        )
    }
}

private const val TTL_DANGER_SECONDS = 60L

@Preview(widthDp = 320)
@Composable
private fun KeyRowPreview() {
    AppTheme(dark = true) {
        KeyRowPreviewContent()
    }
}

@Preview(widthDp = 320)
@Composable
private fun KeyRowLightPreview() {
    AppTheme(dark = false) {
        KeyRowPreviewContent()
    }
}

@Composable
private fun KeyRowPreviewContent() {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    Column(
        modifier =
            Modifier
                .background(colors.background)
                .padding(dimens.spaceSm),
    ) {
        KeyRow(
            key = "cache:user:1001:profile",
            displayName = "1001:profile",
            depth = 1,
            type = KeyType.HASH,
            ttlSeconds = 1_800,
            showTtl = true,
            pinned = true,
            selected = false,
            onSelect = {},
            onTogglePinned = {},
            onCopy = {},
            onEnsurePreview = {},
        )
        KeyRow(
            key = "temp:otp:13800001111",
            displayName = "otp:13800001111",
            depth = 1,
            type = KeyType.STRING,
            ttlSeconds = 42,
            showTtl = true,
            pinned = false,
            selected = true,
            onSelect = {},
            onTogglePinned = {},
            onCopy = {},
            onEnsurePreview = {},
        )
        KeyRow(
            key = "queue:emails",
            displayName = "emails",
            depth = 1,
            type = null,
            ttlSeconds = null,
            showTtl = true,
            pinned = false,
            selected = false,
            onSelect = {},
            onTogglePinned = {},
            onCopy = {},
            onEnsurePreview = {},
        )
        KeyDirectoryRow(
            node = KeyNode("cache", "cache", true, 5, null, emptyList()),
            depth = 0,
            expanded = true,
            onToggle = {},
        )
        KeyDirectoryRow(
            node = KeyNode("product", "cache:product", true, 2, null, emptyList()),
            depth = 1,
            expanded = false,
            onToggle = {},
        )
    }
}
