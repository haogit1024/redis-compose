package com.czh.rediscompose.feature.shell

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.app.AppPage
import com.czh.rediscompose.app.AppState
import com.czh.rediscompose.app.RedisSession
import com.czh.rediscompose.app.SessionStatus
import com.czh.rediscompose.app.WorkspaceTabKind
import com.czh.rediscompose.design.component.AppButton
import com.czh.rediscompose.design.component.AppButtonSize
import com.czh.rediscompose.design.component.AppButtonVariant
import com.czh.rediscompose.design.component.AppIconButton
import com.czh.rediscompose.design.component.AppIconButtonSize
import com.czh.rediscompose.design.component.AppLoading
import com.czh.rediscompose.design.component.AppSearchField
import com.czh.rediscompose.design.component.AppVerticalScrollbar
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.connection.ConnectionEnvironment
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.key.DatabaseInfo
import com.czh.rediscompose.util.formatBytes
import com.czh.rediscompose.util.formatCount
import kotlin.math.roundToInt

@Composable
fun ConnectionSidebar(
    state: AppState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    var filter by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val visibleConnections =
        remember(state.connections, filter) {
            if (filter.isBlank()) {
                state.connections
            } else {
                state.connections.filter {
                    it.name.contains(filter, ignoreCase = true) || it.host.contains(filter, ignoreCase = true)
                }
            }
        }
    val groups =
        remember(visibleConnections) {
            ConnectionEnvironment.entries.mapNotNull { environment ->
                val entries =
                    visibleConnections
                        .filter { it.environment == environment }
                        .sortedWith(
                            compareByDescending<ConnectionProfile> { it.lastOpenedAtEpochMillis ?: 0L }
                                .thenBy { it.name.lowercase() },
                        )
                if (entries.isEmpty()) null else environment to entries
            }
        }

    Column(
        modifier =
            modifier
                .width(dimens.sidebarWidth)
                .fillMaxHeight()
                .background(colors.surface)
                .drawBehind {
                    drawRect(
                        color = colors.border,
                        topLeft = Offset(size.width - 1.dp.toPx(), 0f),
                        size = Size(1.dp.toPx(), size.height),
                    )
                },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = dimens.spaceLg, top = dimens.spaceLg, end = dimens.spaceMd, bottom = dimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        ) {
            Text(
                text = "连接",
                style = typography.microLabel,
                color = colors.textTertiary,
            )
            Text(
                text = state.connections.size.toString(),
                style = typography.monoMeta,
                color = colors.textQuaternary,
            )
            Spacer(Modifier.weight(1f))
            AppIconButton(
                icon = AppIcons.Add,
                contentDescription = "新建连接",
                onClick = { state.setPage(AppPage.CONNECTIONS) },
                size = AppIconButtonSize.Small,
            )
        }
        AppSearchField(
            value = filter,
            onValueChange = { filter = it },
            placeholder = "筛选连接…",
            kbdText = null,
            modifier = Modifier.padding(horizontal = dimens.spaceMd),
        )

        if (state.connections.isEmpty()) {
            SidebarEmpty(
                modifier = Modifier.weight(1f),
                onOpenConnections = { state.setPage(AppPage.CONNECTIONS) },
            )
        } else {
            Row(Modifier.weight(1f)) {
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(scrollState)
                            .padding(start = dimens.spaceSm, end = dimens.spaceXs, top = dimens.spaceXs, bottom = dimens.spaceMd),
                ) {
                    groups.forEach { (environment, entries) ->
                        EnvironmentGroup(environment)
                        entries.forEach { profile ->
                            ConnectionCard(
                                state = state,
                                profile = profile,
                                session = state.sessions[profile.id],
                                active = state.activeConnectionId == profile.id,
                            )
                            if (state.activeConnectionId == profile.id) {
                                val session = state.sessions[profile.id]
                                if (session != null && session.isConnected) {
                                    DatabaseList(
                                        databases = session.databases,
                                        current = session.currentDatabase,
                                        onSelect = { state.selectDatabase(profile.id, it) },
                                    )
                                }
                            }
                        }
                    }
                    if (groups.isEmpty()) {
                        Text(
                            text = "没有匹配的连接",
                            style = typography.caption,
                            color = colors.textTertiary,
                            modifier = Modifier.padding(horizontal = dimens.spaceMd, vertical = dimens.spaceLg),
                        )
                    }
                }
                AppVerticalScrollbar(scrollState)
            }
        }
        SidebarHealth(state)
    }
}

@Composable
private fun EnvironmentGroup(environment: ConnectionEnvironment) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 7.dp, end = 5.dp, top = 12.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.iconTextGap),
    ) {
        Text(
            text = environment.displayName().uppercase(),
            style = typography.microLabel,
            color = colors.textQuaternary,
        )
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.border),
        )
    }
}

@Composable
private fun ConnectionCard(
    state: AppState,
    profile: ConnectionProfile,
    session: RedisSession?,
    active: Boolean,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(dimens.radiusMd)
    val background =
        when {
            active -> Brush.linearGradient(listOf(colors.accentSoft, colors.surface2))
            hovered -> Brush.linearGradient(listOf(colors.surface2, colors.surface2))
            else -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
        }
    val connectionColor = profile.color.toColor(colors)
    val status = session?.status ?: SessionStatus.DISCONNECTED

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp)
                .clip(shape)
                .background(background)
                .border(
                    width = 1.dp,
                    color = if (active) colors.accent.copy(alpha = 0.26f) else Color.Transparent,
                    shape = shape,
                )
                .hoverable(interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = { state.openConnection(profile.id) },
                )
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = dimens.spaceSm + 1.dp, vertical = dimens.spaceSm),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        Box(
            modifier =
                Modifier
                    .size(dimens.sidebarAvatarSize)
                    .background(connectionColor.copy(alpha = 0.13f), RoundedCornerShape(dimens.radiusSm))
                    .border(1.dp, connectionColor.copy(alpha = 0.24f), RoundedCornerShape(dimens.radiusSm)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = profile.initials(),
                style = typography.monoValue,
                color = connectionColor,
                maxLines = 1,
            )
        }
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.iconTextGap),
            ) {
                Text(
                    text = profile.name,
                    style = typography.bodyStrong,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = profile.environment.displayName().uppercase(),
                    style = typography.microLabel,
                    color = colors.textTertiary,
                    modifier =
                        Modifier
                            .background(colors.surface3, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
            Text(
                text = "${profile.host}:${profile.port}",
                style = typography.monoMeta,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            ConnectionMeta(
                status = status,
                session = session,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun ConnectionMeta(
    status: SessionStatus,
    session: RedisSession?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val (dotColor, label) =
        when (status) {
            SessionStatus.CONNECTED -> {
                val parts =
                    buildList {
                        session?.latencyMillis?.let { add("$it ms") }
                        session?.summary?.version?.let { add(it) }
                        session?.summary?.connectedClients?.let { add("$it clients") }
                    }
                colors.success to parts.joinToString(" · ").ifBlank { "已连接" }
            }
            SessionStatus.CONNECTING -> colors.warning to "连接中"
            SessionStatus.ERROR -> colors.danger to (session?.errorMessage ?: "连接失败")
            SessionStatus.DISCONNECTED -> colors.textQuaternary to "未连接"
        }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        Box(
            Modifier
                .size(dimens.tinyDotSize)
                .background(dotColor, CircleShape),
        )
        Text(
            text = label,
            style = typography.monoMeta,
            color = if (status == SessionStatus.ERROR) colors.danger else colors.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DatabaseList(
    databases: List<DatabaseInfo>,
    current: Int,
    onSelect: (Int) -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val databaseScroll = rememberScrollState()
    Row(
        modifier =
            Modifier
                .padding(start = dimens.space2Xl, bottom = dimens.spaceXs + 2.dp)
                .heightIn(max = dimens.dbListMaxHeight)
                .drawBehind {
                    drawLine(
                        color = colors.borderStrong,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                },
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(databaseScroll)
                    .padding(start = dimens.fieldPadding),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            databases.forEach { database ->
                val selected = database.index == current
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(dimens.dbRowHeight)
                            .clip(RoundedCornerShape(dimens.radiusXs))
                            .background(if (selected) colors.accentSoft else Color.Transparent)
                            .clickable { onSelect(database.index) }
                            .pointerHoverIcon(PointerIcon.Hand)
                            .padding(horizontal = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "db${database.index}",
                        style = typography.monoMeta,
                        color = if (selected) colors.accent else colors.textTertiary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatCount(database.keyCount),
                        style = typography.monoMeta,
                        color = if (selected) colors.accent else colors.textQuaternary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarHealth(state: AppState) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val session = state.activeSession()
    val summary = session?.summary
    val connected = session?.isConnected == true

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        color = colors.border,
                        topLeft = Offset.Zero,
                        size = Size(size.width, 1.dp.toPx()),
                    )
                }
                .padding(start = dimens.spaceMd, end = dimens.spaceMd, top = dimens.spaceSm + 2.dp, bottom = dimens.spaceMd),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(colors.surface2, RoundedCornerShape(dimens.healthCardRadius))
                    .border(1.dp, colors.border, RoundedCornerShape(dimens.healthCardRadius))
                    .padding(start = 11.dp, end = 11.dp, top = 11.dp, bottom = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SERVER HEALTH",
                    style = typography.microLabel,
                    color = colors.textTertiary,
                )
                Spacer(Modifier.weight(1f))
                if (connected) {
                    Box(
                        Modifier
                            .size(dimens.tinyDotSize)
                            .background(colors.success, CircleShape),
                    )
                }
            }
            if (connected && summary != null) {
                val used = summary.usedMemoryBytes
                val max = summary.maxMemoryBytes
                val fraction =
                    if (used != null && max != null && max > 0) {
                        (used.toDouble() / max).coerceIn(0.0, 1.0)
                    } else {
                        0.0
                    }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 9.dp, bottom = 7.dp)
                        .height(dimens.healthBarHeight)
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.surface4),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction.toFloat())
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(listOf(colors.accent, colors.success)),
                            ),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text =
                            "内存 " +
                                if (max != null && max > 0) {
                                    "${formatBytes(used)} / ${formatBytes(max)}"
                                } else {
                                    formatBytes(used)
                                },
                        style = typography.monoMeta,
                        color = colors.textTertiary,
                    )
                    if (max != null && max > 0 && used != null) {
                        Text(
                            text = "${(fraction * 100).roundToInt()}%",
                            style = typography.monoMeta,
                            color = colors.textTertiary,
                        )
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs - 1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppButton(
                        text = "洞察",
                        onClick = { state.openTab(session.profile.id, WorkspaceTabKind.INSIGHTS) },
                        modifier = Modifier.weight(1f),
                        variant = AppButtonVariant.Ghost,
                        size = AppButtonSize.Small,
                        icon = AppIcons.Insights,
                    )
                    AppButton(
                        text = "控制台",
                        onClick = { state.openTab(session.profile.id, WorkspaceTabKind.CONSOLE) },
                        modifier = Modifier.weight(1f),
                        variant = AppButtonVariant.Ghost,
                        size = AppButtonSize.Small,
                        icon = AppIcons.Console,
                    )
                    AppIconButton(
                        icon = AppIcons.Settings,
                        contentDescription = "偏好设置",
                        onClick = { state.setPage(AppPage.SETTINGS) },
                        size = AppIconButtonSize.Small,
                    )
                }
            } else if (session?.status == SessionStatus.CONNECTING) {
                AppLoading(label = "正在读取服务器信息…", modifier = Modifier.padding(top = 9.dp))
            } else {
                Text(
                    text = "未连接",
                    style = typography.bodyStrong,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 9.dp),
                )
                Text(
                    text = "选择连接后显示服务器状态。",
                    style = typography.caption,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun SidebarEmpty(
    modifier: Modifier = Modifier,
    onOpenConnections: () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.spaceLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(dimens.brandMarkLarge)
                    .background(colors.accentSoft, RoundedCornerShape(dimens.radiusMd)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.Connections,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(dimens.iconLg),
            )
        }
        Text(
            text = "还没有连接",
            style = typography.bodyStrong,
            color = colors.text,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            text = "打开连接库查看可用连接。",
            style = typography.caption,
            color = colors.textTertiary,
            modifier = Modifier.padding(top = 2.dp),
        )
        AppButton(
            text = "打开连接库",
            onClick = onOpenConnections,
            modifier = Modifier.padding(top = 10.dp),
            variant = AppButtonVariant.Primary,
            size = AppButtonSize.Small,
            icon = AppIcons.Add,
        )
    }
}

@Preview(widthDp = 258, heightDp = 720)
@Composable
private fun ConnectionSidebarPreview() {
    AppTheme(dark = true) {
        ConnectionSidebar(state = previewStateWithConnections())
    }
}
