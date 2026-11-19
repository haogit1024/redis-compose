package com.czh.rediscompose.feature.connections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppButton
import com.czh.rediscompose.design.component.AppButtonSize
import com.czh.rediscompose.design.component.AppButtonVariant
import com.czh.rediscompose.design.component.AppCard
import com.czh.rediscompose.design.component.AppDialog
import com.czh.rediscompose.design.component.AppEmptyState
import com.czh.rediscompose.design.component.AppIconButton
import com.czh.rediscompose.design.component.AppIconButtonSize
import com.czh.rediscompose.design.component.AppMenu
import com.czh.rediscompose.design.component.AppMenuItem
import com.czh.rediscompose.design.component.AppSearchField
import com.czh.rediscompose.design.component.AppSegmented
import com.czh.rediscompose.design.component.AppSegmentedSize
import com.czh.rediscompose.design.component.AppVerticalScrollbar
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.connection.ConnectionEnvironment

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConnectionsScreen(
    state: ConnectionsState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val scrollState = rememberScrollState()
    val filteredConnections = state.filteredConnections

    Box(modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        start = dimens.connectionsPagePaddingH,
                        end = dimens.connectionsPagePaddingH,
                        top = dimens.connectionsPagePaddingV,
                        bottom = dimens.space3Xl,
                    ),
        ) {
            Column(
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = dimens.connectionsMaxWidth)
                        .fillMaxWidth(),
            ) {
                ConnectionsHeader(
                    onlineCount = state.onlineCount,
                    totalCount = state.totalCount,
                    onNew = state::openNew,
                )
                if (state.totalCount > 0) {
                    Spacer(Modifier.height(dimens.spaceMd))
                    ConnectionToolbar(state = state, visibleCount = filteredConnections.size)
                }
                Spacer(Modifier.height(dimens.spaceMd))
                when {
                    state.filteredConnections.isEmpty() && state.hasActiveFilters -> {
                        AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                            AppEmptyState(
                                icon = AppIcons.Filter,
                                title = "没有匹配的连接",
                                description = "尝试清空搜索或切换环境筛选。",
                                action = {
                                    AppButton(
                                        text = "清除筛选",
                                        onClick = state::clearFilters,
                                        variant = AppButtonVariant.Secondary,
                                        icon = AppIcons.Close,
                                    )
                                },
                            )
                        }
                    }
                    state.filteredConnections.isEmpty() -> {
                        AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                            AppEmptyState(
                                icon = AppIcons.Connections,
                                title = "添加你的第一个 Redis 连接",
                                description = "连接信息保存在 ~/.redis-compose/connections.json，密码会加密存储。",
                                action = {
                                    AppButton(
                                        text = "新建连接",
                                        onClick = state::openNew,
                                        variant = AppButtonVariant.Primary,
                                        icon = AppIcons.Add,
                                    )
                                },
                            )
                        }
                    }
                    else -> {
                        ConnectionGrid(state = state, connections = filteredConnections)
                        Spacer(Modifier.height(dimens.spaceLg))
                        RecentConnectionsTable(
                            connections = state.recentConnections,
                            runtimeFor = state::runtimeFor,
                            onOpen = state::open,
                        )
                    }
                }
                if (state.totalCount == 0) {
                    Text(
                        text = "支持的配置：直连、TLS、SSH 隧道；Sentinel / Cluster 将在后续版本提供。",
                        style = typography.caption,
                        color = colors.textTertiary,
                        modifier = Modifier.padding(top = dimens.spaceMd),
                    )
                }
            }
        }
        AppVerticalScrollbar(
            scrollState = scrollState,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        )

        ConnectionSheet(state = state)
        DeleteConnectionDialog(state = state)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConnectionToolbar(
    state: ConnectionsState,
    visibleCount: Int,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val searchAndFilters: @Composable () -> Unit = {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            AppSearchField(
                value = state.search,
                onValueChange = state::updateSearch,
                modifier = Modifier.width(dimens.connectionSearchWidth),
                placeholder = "搜索名称、Host 或分组…",
                kbdText = null,
            )
            AppSegmented(
                options = listOf<ConnectionEnvironment?>(null) + ConnectionEnvironment.entries,
                selected = state.environmentFilter,
                onSelect = state::selectEnvironment,
                size = AppSegmentedSize.Small,
                label = { it?.displayName() ?: "全部" },
            )
        }
    }
    val rightControls: @Composable () -> Unit = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$visibleCount 个连接",
                style = typography.monoMeta,
                color = colors.textQuaternary,
                modifier = Modifier.padding(end = dimens.spaceXs + 2.dp),
            )
            AppIconButton(
                icon = AppIcons.Grid,
                contentDescription = "网格视图",
                onClick = { state.chooseView(ConnectionView.GRID) },
                active = state.view == ConnectionView.GRID,
                size = AppIconButtonSize.Small,
            )
            AppIconButton(
                icon = AppIcons.List,
                contentDescription = "列表视图",
                onClick = { state.chooseView(ConnectionView.LIST) },
                active = state.view == ConnectionView.LIST,
                size = AppIconButtonSize.Small,
            )
            Box {
                AppButton(
                    text = state.sort.label,
                    onClick = { sortMenuExpanded = !sortMenuExpanded },
                    size = AppButtonSize.Small,
                    variant = AppButtonVariant.Ghost,
                    icon = AppIcons.Sort,
                )
                AppMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    items =
                        ConnectionSort.entries.map { option ->
                            AppMenuItem(
                                label = option.label,
                                icon = AppIcons.Sort,
                                onClick = { state.chooseSort(option) },
                            )
                        },
                )
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 900.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            ) {
                searchAndFilters()
                Spacer(Modifier.weight(1f))
                rightControls()
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
                searchAndFilters()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    rightControls()
                }
            }
        }
    }
}

@Composable
private fun ConnectionGrid(
    state: ConnectionsState,
    connections: List<com.czh.rediscompose.domain.connection.ConnectionProfile>,
) {
    val dimens = LocalAppDimens.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (state.view == ConnectionView.LIST) {
            Column(verticalArrangement = Arrangement.spacedBy(dimens.connectionsGridGap)) {
                connections.forEach { profile ->
                    ConnectionCard(
                        profile = profile,
                        runtime = state.runtimeFor(profile.id),
                        testing = state.testingConnectionId == profile.id,
                        onOpen = { state.open(profile.id) },
                        onEdit = { state.openEdit(profile.id) },
                        onDuplicate = { state.openDuplicate(profile.id) },
                        onTest = { state.testExisting(profile.id) },
                        onDelete = { state.requestDelete(profile.id) },
                    )
                }
                AddConnectionRow(onClick = state::openNew)
            }
        } else {
            val columns =
                when {
                    maxWidth >= 1_150.dp -> 3
                    maxWidth >= 850.dp -> 2
                    else -> 1
                }
            Column(verticalArrangement = Arrangement.spacedBy(dimens.connectionsGridGap)) {
                connections.chunked(columns).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dimens.connectionsGridGap),
                    ) {
                        row.forEach { profile ->
                            ConnectionCard(
                                profile = profile,
                                runtime = state.runtimeFor(profile.id),
                                testing = state.testingConnectionId == profile.id,
                                modifier = Modifier.weight(1f),
                                onOpen = { state.open(profile.id) },
                                onEdit = { state.openEdit(profile.id) },
                                onDuplicate = { state.openDuplicate(profile.id) },
                                onTest = { state.testExisting(profile.id) },
                                onDelete = { state.requestDelete(profile.id) },
                            )
                        }
                        repeat(columns - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                AddConnectionRow(onClick = state::openNew)
            }
        }
    }
}

@Composable
private fun DeleteConnectionDialog(state: ConnectionsState) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val target = state.deleteTarget
    AppDialog(
        visible = target != null,
        onDismissRequest = state::dismissDelete,
        title = "删除连接",
        subtitle = target?.let { "${it.name} · ${it.host}:${it.port}" },
        footer = {
            AppButton("取消", state::dismissDelete)
            AppButton(
                text = "删除连接",
                onClick = state::confirmDelete,
                variant = AppButtonVariant.Danger,
                icon = AppIcons.Delete,
            )
        },
    ) {
        Text(
            text = "删除后会关闭该连接的所有会话与标签页，并从 connections.json 中移除。此操作不可撤销。",
            style = typography.body,
            color = colors.textSecondary,
        )
    }
}
