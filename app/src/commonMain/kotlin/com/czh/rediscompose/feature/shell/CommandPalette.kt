package com.czh.rediscompose.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.app.AppPage
import com.czh.rediscompose.app.AppState
import com.czh.rediscompose.app.WorkspaceTabKind
import com.czh.rediscompose.design.component.AppKbd
import com.czh.rediscompose.design.component.AppVerticalScrollbar
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

private data class PaletteEntry(
    val label: String,
    val icon: ImageVector,
    val group: String,
    val meta: String? = null,
    val enabled: Boolean = true,
    val action: () -> Unit,
)

@Composable
fun CommandPalette(
    visible: Boolean,
    state: AppState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    CommandPaletteContent(state = state, onDismiss = onDismiss, modifier = modifier)
}

@Composable
private fun CommandPaletteContent(
    state: AppState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    var query by remember { mutableStateOf("") }
    var selectedIndex by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    val activeConnection = state.activeConnection

    val entries =
        remember(state.connections, activeConnection?.id) {
            buildList {
                add(
                    PaletteEntry(
                        label = "工作区",
                        icon = AppIcons.Key,
                        group = "页面",
                        meta = "Workspace",
                    ) { state.setPage(AppPage.WORKSPACE) },
                )
                add(
                    PaletteEntry(
                        label = "命令控制台",
                        icon = AppIcons.Console,
                        group = "页面",
                        meta = if (activeConnection == null) "需要连接" else "Console",
                        enabled = activeConnection != null,
                    ) {
                        activeConnection?.let { state.openTab(it.id, WorkspaceTabKind.CONSOLE) }
                    },
                )
                add(
                    PaletteEntry(
                        label = "服务器洞察",
                        icon = AppIcons.Insights,
                        group = "页面",
                        meta = if (activeConnection == null) "需要连接" else "Insights",
                        enabled = activeConnection != null,
                    ) {
                        activeConnection?.let { state.openTab(it.id, WorkspaceTabKind.INSIGHTS) }
                    },
                )
                add(
                    PaletteEntry(
                        label = "连接库",
                        icon = AppIcons.Connections,
                        group = "页面",
                        meta = "Connections",
                    ) { state.setPage(AppPage.CONNECTIONS) },
                )
                add(
                    PaletteEntry(
                        label = "设置",
                        icon = AppIcons.Settings,
                        group = "页面",
                        meta = "Settings",
                    ) { state.setPage(AppPage.SETTINGS) },
                )
                state.connections.forEach { connection ->
                    add(
                        PaletteEntry(
                            label = connection.name,
                            icon = AppIcons.Database,
                            group = "连接",
                            meta = "${connection.host}:${connection.port}",
                        ) { state.openConnection(connection.id) },
                    )
                }
            }
        }

    val filtered =
        remember(entries, query) {
            if (query.isBlank()) {
                entries
            } else {
                entries.filter {
                    it.label.contains(query, ignoreCase = true) ||
                        it.meta?.contains(query, ignoreCase = true) == true
                }
            }
        }
    val safeIndex = selectedIndex.coerceIn(0, (filtered.size - 1).coerceAtLeast(0))

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(query) { selectedIndex = 0 }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> {
                            if (filtered.isNotEmpty()) {
                                selectedIndex = (safeIndex + 1) % filtered.size
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            if (filtered.isNotEmpty()) {
                                selectedIndex = if (safeIndex == 0) filtered.lastIndex else safeIndex - 1
                            }
                            true
                        }
                        Key.Enter, Key.NumPadEnter -> {
                            filtered.getOrNull(safeIndex)?.takeIf { it.enabled }?.let { entry ->
                                onDismiss()
                                entry.action()
                            }
                            true
                        }
                        Key.Escape -> {
                            onDismiss()
                            true
                        }
                        else -> false
                    }
                },
        contentAlignment = Alignment.TopCenter,
    ) {
        val shape = RoundedCornerShape(dimens.radiusXl)
        Column(
            modifier =
                Modifier
                    .padding(top = maxHeight * 0.13f, start = dimens.spaceLg, end = dimens.spaceLg)
                    .widthIn(max = dimens.paletteWidth)
                    .fillMaxWidth()
                    .shadow(elevation = 30.dp, shape = shape)
                    .background(colors.surface, shape)
                    .border(1.dp, colors.borderStrong, shape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(dimens.paletteSearchHeight)
                        .padding(horizontal = dimens.spaceLg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.fieldPadding),
            ) {
                Icon(
                    imageVector = AppIcons.Search,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(dimens.iconLg),
                )
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier =
                        Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                    singleLine = true,
                    textStyle = typography.body.copy(color = colors.text),
                    cursorBrush = SolidColor(colors.accent),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "搜索页面或连接…",
                                    style = typography.body,
                                    color = colors.textTertiary,
                                )
                            }
                            inner()
                        }
                    },
                )
                AppKbd("⌘K")
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border),
            )
            Row(
                modifier =
                    Modifier
                        .heightIn(max = dimens.paletteListMaxHeight),
            ) {
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(dimens.iconTextGap),
                ) {
                    if (filtered.isEmpty()) {
                        Text(
                            text = "没有匹配的页面或连接",
                            style = typography.body,
                            color = colors.textTertiary,
                            modifier = Modifier.padding(horizontal = dimens.fieldPadding, vertical = dimens.spaceXl),
                        )
                    } else {
                        filtered.groupBy { it.group }.forEach { (group, entriesInGroup) ->
                            Text(
                                text = group,
                                style = typography.microLabel,
                                color = colors.textQuaternary,
                                modifier = Modifier.padding(start = dimens.spaceSm, top = dimens.spaceSm, bottom = dimens.spaceXs),
                            )
                            entriesInGroup.forEach { entry ->
                                val globalIndex = filtered.indexOf(entry)
                                PaletteRow(
                                    entry = entry,
                                    selected = globalIndex == safeIndex,
                                    onClick = {
                                        if (entry.enabled) {
                                            onDismiss()
                                            entry.action()
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                AppVerticalScrollbar(scrollState)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border),
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(colors.surface2)
                        .padding(horizontal = dimens.spaceLg, vertical = dimens.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceLg),
            ) {
                PaletteHint(keys = "↑↓", label = "选择")
                PaletteHint(keys = "↵", label = "打开")
                PaletteHint(keys = "Esc", label = "关闭")
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${filtered.size} 项",
                    style = typography.caption,
                    color = colors.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun PaletteRow(
    entry: PaletteEntry,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val shape = RoundedCornerShape(dimens.radiusControl)
    val contentColor =
        when {
            !entry.enabled -> colors.textTertiary.copy(alpha = 0.45f)
            selected -> colors.text
            else -> colors.textSecondary
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(dimens.paletteItemHeight)
                .background(if (selected) colors.accentSoft else androidx.compose.ui.graphics.Color.Transparent, shape)
                .clickable(enabled = entry.enabled, onClick = onClick)
                .padding(horizontal = dimens.fieldPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.fieldPadding),
    ) {
        Icon(
            imageVector = entry.icon,
            contentDescription = null,
            tint = if (selected) colors.accent else contentColor,
            modifier = Modifier.size(dimens.iconButtonIcon),
        )
        Text(
            text = entry.label,
            style = typography.body,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (entry.meta != null) {
            Text(
                text = entry.meta,
                style = typography.monoMeta,
                color = colors.textQuaternary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PaletteHint(
    keys: String,
    label: String,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 1.dp),
    ) {
        AppKbd(keys)
        Text(
            text = label,
            style = typography.caption,
            color = colors.textTertiary,
        )
    }
}

@Preview(widthDp = 1000, heightDp = 700)
@Composable
private fun CommandPalettePreview() {
    AppTheme(dark = true) {
        CommandPalette(
            visible = true,
            state = previewStateWithConnections(),
            onDismiss = {},
        )
    }
}
