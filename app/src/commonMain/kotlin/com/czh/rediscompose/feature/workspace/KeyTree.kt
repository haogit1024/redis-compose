package com.czh.rediscompose.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppButton
import com.czh.rediscompose.design.component.AppButtonSize
import com.czh.rediscompose.design.component.AppButtonVariant
import com.czh.rediscompose.design.component.AppEmptyState
import com.czh.rediscompose.design.component.AppIconButton
import com.czh.rediscompose.design.component.AppIconButtonSize
import com.czh.rediscompose.design.component.AppLoading
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.key.KeyNode
import com.czh.rediscompose.domain.settings.KeyView
import com.czh.rediscompose.util.formatCount
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

/** Flattened tree entry rendered by the lazy key list. */
internal sealed interface KeyTreeRow {
    val depth: Int

    data class Directory(
        val node: KeyNode,
        override val depth: Int,
    ) : KeyTreeRow

    data class Leaf(
        val node: KeyNode,
        override val depth: Int,
    ) : KeyTreeRow
}

/**
 * Depth-first walk of the built key tree. Directories only expose children when their path
 * is in [expanded]; leaf nodes always render.
 */
internal fun flattenKeyTree(
    nodes: List<KeyNode>,
    expanded: Set<String>,
    depth: Int = 0,
): List<KeyTreeRow> =
    buildList {
        nodes.forEach { node ->
            if (node.isDirectory) {
                add(KeyTreeRow.Directory(node, depth))
                if (node.path in expanded) {
                    addAll(flattenKeyTree(node.children, expanded, depth + 1))
                }
            } else {
                add(KeyTreeRow.Leaf(node, depth))
            }
        }
    }

@Composable
internal fun KeyTree(
    state: WorkspaceState,
    onCopyKey: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalAppDimens.current
    val listState = rememberLazyListState()
    val pinnedKeys by remember(state) { derivedStateOf { state.pinnedKeyList } }
    val treeRows by remember(state) { derivedStateOf { flattenKeyTree(state.tree, state.expandedDirs) } }
    val resultKeys by remember(state) { derivedStateOf { state.filteredKeys } }
    val flatKeys by remember(state) { derivedStateOf { state.keys.toList() } }

    LaunchedEffect(listState, state) {
        snapshotLoadMore(listState) { state.loadMore() }
    }

    val hasContent = state.keys.isNotEmpty() || pinnedKeys.isNotEmpty()
    if (!hasContent) {
        EmptyKeyTree(state = state, modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding =
            PaddingValues(
                start = dimens.treeListPadding,
                end = dimens.treeListPadding,
                bottom = dimens.spaceMd,
            ),
    ) {
        if (state.loadError != null) {
            item(key = "load-error") { InlineLoadError(state = state) }
        }
        if (pinnedKeys.isNotEmpty()) {
            item(key = "pinned-header") {
                TreeSection(title = "置顶", icon = AppIcons.Star)
            }
            item(key = "pinned-list") {
                PinnedKeyList(
                    keys = pinnedKeys,
                    selectedKey = state.selectedKey,
                    typeOf = state::typeOf,
                    ttlOf = state::ttlOf,
                    showTtl = state.showTtl,
                    onSelect = state::selectKey,
                    onUnpin = state::togglePinned,
                    onCopy = onCopyKey,
                    onEnsurePreview = state::ensurePreview,
                )
            }
        }
        if (!state.hasFilters) {
            item(key = "keyspace-header") {
                TreeSection(title = "键空间", trailing = "db${state.databaseIndex}")
            }
            if (state.keyView == KeyView.FLAT) {
                items(items = flatKeys, key = { key -> "flat:$key" }) { key ->
                    KeyTreeLeafRow(state = state, key = key, displayName = key, depth = 0, onCopyKey = onCopyKey)
                }
            } else {
                items(
                    items = treeRows,
                    key = { row ->
                        when (row) {
                            is KeyTreeRow.Directory -> "dir:${row.node.path}"
                            is KeyTreeRow.Leaf -> "key:${row.node.path}"
                        }
                    },
                ) { row ->
                    when (row) {
                        is KeyTreeRow.Directory ->
                            KeyDirectoryRow(
                                node = row.node,
                                depth = row.depth,
                                expanded = row.node.path in state.expandedDirs,
                                onToggle = { state.toggleDirectory(row.node.path) },
                            )
                        is KeyTreeRow.Leaf ->
                            KeyTreeLeafRow(
                                state = state,
                                key = row.node.path,
                                displayName = row.node.name,
                                depth = row.depth,
                                onCopyKey = onCopyKey,
                            )
                    }
                }
            }
        } else {
            item(key = "results-header") {
                ResultsSection(state = state, onClear = state::clearFilters)
            }
            items(items = resultKeys, key = { key -> "result:$key" }) { key ->
                KeyTreeLeafRow(state = state, key = key, displayName = key, depth = 0, onCopyKey = onCopyKey)
            }
            if (resultKeys.isEmpty() && !state.isLoading) {
                item(key = "no-match") {
                    NoMatchState(onClear = state::clearFilters)
                }
            }
        }
        if (state.isLoading) {
            item(key = "loading-more") {
                AppLoading(
                    label = if (state.keys.isEmpty()) "正在 SCAN Keyspace…" else "SCAN 加载中…",
                    inline = true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimens.spaceSm, vertical = dimens.spaceMd),
                )
            }
        } else if (!state.loadedAll) {
            item(key = "load-more") { LoadMoreRow(state = state) }
        }
        item(key = "bottom-space") { Spacer(Modifier.height(dimens.spaceMd)) }
    }
}

@Composable
private fun KeyTreeLeafRow(
    state: WorkspaceState,
    key: String,
    displayName: String,
    depth: Int,
    onCopyKey: (String) -> Unit,
) {
    KeyRow(
        key = key,
        displayName = displayName,
        depth = depth,
        type = state.typeOf(key),
        ttlSeconds = state.ttlOf(key),
        showTtl = state.showTtl,
        pinned = state.isPinned(key),
        selected = state.selectedKey == key,
        onSelect = { state.selectKey(key) },
        onTogglePinned = { state.togglePinned(key) },
        onCopy = { onCopyKey(key) },
        onEnsurePreview = { state.ensurePreview(key) },
    )
}

@Composable
private fun EmptyKeyTree(
    state: WorkspaceState,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalAppDimens.current
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        when {
            state.loadError != null ->
                AppEmptyState(
                    icon = AppIcons.ErrorCircle,
                    title = "加载 Key 失败",
                    description = state.loadError,
                    action = {
                        AppButton(
                            text = "重试",
                            onClick = state::retry,
                            variant = AppButtonVariant.Primary,
                            size = AppButtonSize.Small,
                            icon = AppIcons.Refresh,
                        )
                    },
                )
            !state.loadedAll || state.isLoading ->
                AppLoading(
                    label = "正在 SCAN Keyspace…",
                    modifier = Modifier.padding(top = dimens.space3Xl),
                )
            state.hasFilters ->
                NoMatchState(onClear = state::clearFilters)
            else ->
                AppEmptyState(
                    icon = AppIcons.Key,
                    title = "当前 DB 没有 Key",
                    description = "db${state.databaseIndex} 中还没有键。写入后刷新即可看到。",
                    action = {
                        AppButton(
                            text = "刷新",
                            onClick = state::refresh,
                            variant = AppButtonVariant.Secondary,
                            size = AppButtonSize.Small,
                            icon = AppIcons.Refresh,
                        )
                    },
                )
        }
    }
}

@Composable
private fun NoMatchState(onClear: () -> Unit) {
    AppEmptyState(
        icon = AppIcons.Search,
        title = "没有匹配的 Key",
        description = "已加载的 Key 中没有符合当前模式或类型筛选的项；SCAN 只覆盖已加载部分。",
        action = {
            AppButton(
                text = "清除筛选",
                onClick = onClear,
                variant = AppButtonVariant.Secondary,
                size = AppButtonSize.Small,
                icon = AppIcons.Close,
            )
        },
    )
}

@Composable
private fun ResultsSection(
    state: WorkspaceState,
    onClear: () -> Unit,
) {
    val suffix = if (state.isLoading) " · 扫描中…" else ""
    TreeSection(
        title = "${formatCount(state.filteredKeys.size.toLong())} 个结果$suffix",
        icon = AppIcons.Filter,
        trailingContent = {
            AppIconButton(
                icon = AppIcons.Close,
                contentDescription = "清除筛选",
                onClick = onClear,
                size = AppIconButtonSize.Small,
            )
        },
    )
}

@Composable
private fun TreeSection(
    title: String,
    icon: ImageVector? = null,
    trailing: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = dimens.spaceXs,
                    end = dimens.spaceXs,
                    top = dimens.spaceSm,
                    bottom = dimens.spaceXs,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.iconTextGap),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textQuaternary,
                modifier = Modifier.size(dimens.treeSectionIconSize),
            )
        }
        Text(
            text = title,
            style = typography.microLabel,
            color = colors.textQuaternary,
            maxLines = 1,
        )
        Box(
            Modifier
                .weight(1f)
                .padding(horizontal = dimens.spaceSm)
                .height(1.dp)
                .background(colors.border),
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = typography.monoMicro,
                color = colors.textQuaternary,
                maxLines = 1,
            )
        }
        trailingContent?.invoke()
    }
}

@Composable
private fun LoadMoreRow(state: WorkspaceState) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(dimens.radiusControlSmall)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = dimens.spaceSm, start = dimens.spaceXs, end = dimens.spaceXs)
                .clip(shape)
                .background(colors.surface2)
                .drawBehind {
                    drawRoundRect(
                        color = if (hovered) colors.borderStrong else colors.border,
                        cornerRadius = CornerRadius(dimens.radiusControlSmall.toPx()),
                        style =
                            Stroke(
                                width = 1.dp.toPx(),
                                pathEffect =
                                    PathEffect.dashPathEffect(
                                        floatArrayOf(4.dp.toPx(), 4.dp.toPx()),
                                    ),
                            ),
                    )
                }
                .hoverable(interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                ) { state.loadMore() }
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(vertical = dimens.treeMorePadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = AppIcons.ChevronDown,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(dimens.iconXs),
        )
        Spacer(Modifier.size(dimens.spaceXs))
        Text(
            text = "SCAN 加载更多 · cursor ${state.cursor}",
            style = typography.monoMicro,
            color = if (hovered) colors.textSecondary else colors.textTertiary,
            maxLines = 1,
        )
    }
}

@Composable
private fun InlineLoadError(state: WorkspaceState) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val shape = RoundedCornerShape(dimens.radiusXs)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.spaceXs, vertical = dimens.spaceXs)
                .background(colors.danger.copy(alpha = 0.10f), shape)
                .border(1.dp, colors.danger.copy(alpha = 0.26f), shape)
                .padding(horizontal = dimens.spaceSm, vertical = dimens.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        Icon(
            imageVector = AppIcons.Warning,
            contentDescription = null,
            tint = colors.danger,
            modifier = Modifier.size(dimens.iconSm),
        )
        Text(
            text = state.loadError.orEmpty(),
            style = typography.caption,
            color = colors.danger,
            maxLines = 2,
            modifier = Modifier.weight(1f),
        )
        AppButton(
            text = "重试",
            onClick = state::retry,
            variant = AppButtonVariant.Ghost,
            size = AppButtonSize.Small,
            icon = AppIcons.Refresh,
        )
    }
}

private suspend fun snapshotLoadMore(
    listState: LazyListState,
    onLoadMore: () -> Unit,
) {
    snapshotFlow {
        val info = listState.layoutInfo
        info.totalItemsCount to (info.visibleItemsInfo.lastOrNull()?.index ?: -1)
    }.distinctUntilChanged().collect { (total, last) ->
        if (total > 0 && last >= total - NEAR_BOTTOM_ITEMS) {
            onLoadMore()
        }
    }
}

private const val NEAR_BOTTOM_ITEMS = 2

@Preview(widthDp = 320, heightDp = 640)
@Composable
private fun KeyTreePreview() {
    AppTheme(dark = true) {
        val workspace = remember { previewWorkspaceState() }
        KeyTree(state = workspace, onCopyKey = {})
    }
}

@Preview(widthDp = 320, heightDp = 640)
@Composable
private fun KeyTreeSearchPreview() {
    AppTheme(dark = false) {
        val workspace =
            remember {
                previewWorkspaceState().also { state ->
                    state.searchInput = "app:*"
                    state.applySearch("app:*")
                }
            }
        KeyTree(state = workspace, onCopyKey = {})
    }
}

@Preview(widthDp = 320, heightDp = 400)
@Composable
private fun KeyTreeEmptyPreview() {
    AppTheme(dark = true) {
        val workspace = remember { previewWorkspaceState(empty = true) }
        KeyTree(state = workspace, onCopyKey = {})
    }
}
