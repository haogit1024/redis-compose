package com.czh.rediscompose.feature.workspace.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppBadge
import com.czh.rediscompose.design.component.AppButton
import com.czh.rediscompose.design.component.AppButtonSize
import com.czh.rediscompose.design.component.AppButtonVariant
import com.czh.rediscompose.design.component.AppTextField
import com.czh.rediscompose.design.component.AppTooltip
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

/** Shared rounded card that hosts an editor toolbar, content and footer. */
@Composable
internal fun EditorCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(dimens.editorCardRadius))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(dimens.editorCardRadius)),
        content = content,
    )
}

internal data class ToolbarAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val enabled: Boolean,
    val primary: Boolean,
    val loading: Boolean,
)

/** Wrapping toolbar: title, count badge, filter input and right-aligned actions. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CollectionToolbar(
    title: String,
    icon: ImageVector,
    badge: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    searchPlaceholder: String,
    modifier: Modifier = Modifier,
    actions: List<ToolbarAction> = emptyList(),
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    FlowRow(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.surface2.copy(alpha = 0.70f))
                .padding(
                    horizontal = dimens.editorToolbarPaddingH,
                    vertical = dimens.editorToolbarPaddingV,
                ),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm - 1.dp),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs + 1.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.iconTextGap),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(dimens.treeFolderIconSize),
            )
            Text(
                text = title,
                style = typography.bodyStrong,
                color = colors.text,
                maxLines = 1,
            )
            AppBadge(text = badge)
        }
        AppTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.widthIn(max = dimens.collectionSearchWidth),
            placeholder = searchPlaceholder,
            leadingIcon = AppIcons.Search,
            mono = true,
            height = dimens.collectionSearchHeight,
        )
        actions.forEach { action ->
            AppButton(
                text = action.label,
                onClick = action.onClick,
                variant = if (action.primary) AppButtonVariant.Primary else AppButtonVariant.Secondary,
                size = AppButtonSize.Small,
                icon = action.icon,
                enabled = action.enabled,
                loading = action.loading,
            )
        }
    }
}

internal data class CollectionHeaderCell(
    val label: String,
    val weight: Float = 0f,
    val width: Dp? = null,
    val alignEnd: Boolean = false,
)

/** Fixed table header with token-based row height. */
@Composable
internal fun CollectionHeader(
    cells: List<CollectionHeaderCell>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(dimens.collectionHeaderHeight)
                .background(colors.surface2)
                .drawBehind {
                    drawRect(
                        color = colors.border,
                        topLeft = Offset(0f, size.height - 1.dp.toPx()),
                        size = Size(size.width, 1.dp.toPx()),
                    )
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cells.forEach { cell ->
            Box(
                modifier = cellModifier(cell).padding(horizontal = dimens.collectionCellPadding),
                contentAlignment = if (cell.alignEnd) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Text(
                    text = cell.label.uppercase(),
                    style = typography.metaLabel,
                    color = colors.textQuaternary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun RowScope.CollectionCell(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .padding(horizontal = LocalAppDimens.current.collectionCellPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        content()
    }
}

@Composable
internal fun CellTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String? = null,
    alignEnd: Boolean = false,
    isError: Boolean = false,
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val valueColor =
        if (isError) {
            colors.danger
        } else if (enabled) {
            colors.text
        } else {
            colors.textSecondary
        }
    val style = typography.monoKeyRow.copy(color = valueColor)
    if (!enabled) {
        Text(
            text = value.ifEmpty { "—" },
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            modifier = modifier.fillMaxWidth(),
        )
        return
    }
    val fieldContent: @Composable () -> Unit = {
        Box(modifier.fillMaxWidth(), contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = style,
                cursorBrush = SolidColor(colors.accent),
                decorationBox = { inner ->
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = style.copy(color = colors.textQuaternary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                },
            )
        }
    }
    if (value.length > LONG_VALUE_TOOLTIP_CHARS) {
        AppTooltip(text = value, modifier = Modifier.fillMaxWidth()) {
            fieldContent()
        }
    } else {
        fieldContent()
    }
}

private const val LONG_VALUE_TOOLTIP_CHARS = 40

/** 22dp row action that only appears when the row is hovered, dirty or selected. */
@Composable
internal fun RowActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    if (!visible) {
        Spacer(modifier.size(dimens.collectionActionSize))
        return
    }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val tint =
        when {
            !enabled -> colors.textQuaternary
            danger && hovered -> colors.danger
            hovered -> colors.text
            else -> colors.textTertiary
        }
    Box(
        modifier =
            modifier
                .size(dimens.collectionActionSize)
                .clip(RoundedCornerShape(dimens.radiusChip))
                .background(if (hovered && enabled) colors.surface3 else Color.Transparent)
                .hoverable(interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                )
                .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(dimens.iconXs),
        )
    }
}

/** Bottom status strip shared by all editors. */
@Composable
internal fun CollectionFooter(
    left: String,
    middle: String,
    dirty: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.surface)
                .drawBehind {
                    drawRect(
                        color = colors.border,
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, 1.dp.toPx()),
                    )
                }
                .padding(horizontal = dimens.editorToolbarPaddingH, vertical = dimens.spaceXs + 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd),
    ) {
        Text(text = left, style = typography.monoMicro, color = colors.textQuaternary, maxLines = 1)
        Text(text = middle, style = typography.monoMicro, color = colors.textQuaternary, maxLines = 1)
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = if (dirty) AppIcons.Edit else AppIcons.Check,
            contentDescription = null,
            tint = if (dirty) colors.warning else colors.success,
            modifier = Modifier.size(dimens.treeSectionIconSize),
        )
        Text(
            text = if (dirty) "未保存" else "已同步",
            style = typography.monoMicro,
            color = if (dirty) colors.warning else colors.success,
            maxLines = 1,
        )
    }
}

@Composable
internal fun CollectionEmpty(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val dimens = LocalAppDimens.current
    Box(
        modifier = modifier.fillMaxWidth().padding(dimens.space3Xl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = typography.bodySmall,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

private fun RowScope.cellModifier(cell: CollectionHeaderCell): Modifier =
    if (cell.width != null) Modifier.width(cell.width) else Modifier.weight(cell.weight)

@Preview(widthDp = 760, heightDp = 220)
@Composable
private fun CollectionTablePreview() {
    AppTheme(dark = true) {
        val dimens = LocalAppDimens.current
        EditorCard(Modifier.padding(dimens.spaceLg)) {
            CollectionToolbar(
                title = "Hash fields",
                icon = AppIcons.Hash,
                badge = "8 fields",
                searchQuery = "",
                onSearchChange = {},
                searchPlaceholder = "筛选字段…",
                actions = emptyList(),
            )
            CollectionHeader(
                cells =
                    listOf(
                        CollectionHeaderCell("Field", weight = 0.32f),
                        CollectionHeaderCell("Value", weight = 1f),
                        CollectionHeaderCell("", width = dimens.collectionActionsWidth),
                    ),
            )
            Row(Modifier.fillMaxWidth().height(dimens.collectionRowHeight), verticalAlignment = Alignment.CenterVertically) {
                CollectionCell(Modifier.weight(0.32f)) {
                    CellTextField("email", {}, enabled = true)
                }
                CollectionCell(Modifier.weight(1f)) {
                    CellTextField("ada@acme.io", {}, enabled = true)
                }
                CollectionCell(Modifier.width(dimens.collectionActionsWidth)) {
                    RowActionButton(AppIcons.Check, "保存", {})
                }
            }
            CollectionFooter("HSET · HDEL", "8 fields", dirty = true)
        }
    }
}
