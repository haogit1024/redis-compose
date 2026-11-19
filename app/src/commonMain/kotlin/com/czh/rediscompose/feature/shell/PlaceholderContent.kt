package com.czh.rediscompose.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.app.AppPage
import com.czh.rediscompose.app.AppState
import com.czh.rediscompose.app.WorkspaceTabKind
import com.czh.rediscompose.design.component.AppButton
import com.czh.rediscompose.design.component.AppButtonVariant
import com.czh.rediscompose.design.component.AppCard
import com.czh.rediscompose.design.component.AppEmptyState
import com.czh.rediscompose.design.component.AppLoading
import com.czh.rediscompose.design.component.ToastKind
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.icon.BrandMark
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.feature.console.ConsoleScreen
import com.czh.rediscompose.feature.insights.InsightsScreen
import com.czh.rediscompose.feature.workspace.WorkspaceScreen

@Composable
fun AppPageContent(
    state: AppState,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(LocalAppColors.current.background)) {
        when (state.activePage) {
            AppPage.WORKSPACE -> WorkspaceContent(state)
            AppPage.CONNECTIONS -> ConnectionsPlaceholder(state)
            AppPage.SETTINGS -> SettingsPlaceholder(state)
        }
    }
}

@Composable
private fun WorkspaceContent(state: AppState) {
    val tab = state.activeTab()
    if (tab == null) {
        EmptyWorkspace(state)
        return
    }
    when (tab.kind) {
        WorkspaceTabKind.KEYS -> {
            val workspace = state.workspaceFor(tab.connectionId)
            if (workspace != null) {
                WorkspaceScreen(workspace = workspace)
            } else {
                AppLoading(label = "正在准备 Key 浏览器…")
            }
        }
        WorkspaceTabKind.CONSOLE -> {
            val console = state.consoleFor(tab.connectionId)
            if (console != null) {
                ConsoleScreen(console = console)
            } else {
                AppLoading(label = "正在准备命令控制台…")
            }
        }
        WorkspaceTabKind.INSIGHTS -> {
            val insights = state.insightsFor(tab.connectionId)
            if (insights != null) {
                InsightsScreen(
                    state = insights,
                    onOpenConsole = { state.openTab(tab.connectionId, WorkspaceTabKind.CONSOLE) },
                )
            } else {
                AppLoading(label = "正在准备服务器洞察…")
            }
        }
    }
}

@Composable
private fun EmptyWorkspace(state: AppState) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(dimens.space2Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BrandMark(size = dimens.brandMarkLarge)
        Text(
            text = "从左侧选择一个连接开始",
            style = typography.pageTitle,
            color = colors.text,
            modifier = Modifier.padding(top = dimens.spaceLg),
        )
        Text(
            text =
                if (state.connections.isEmpty()) {
                    "还没有连接。打开连接库查看连接配置，或使用 ⌘K 打开命令面板。"
                } else {
                    "选择连接后，这里会打开键浏览、控制台与服务器洞察。"
                },
            style = typography.body,
            color = colors.textTertiary,
            modifier =
                Modifier
                    .padding(top = dimens.spaceSm)
                    .widthIn(max = dimens.emptyDescriptionMaxWidth),
        )
        if (state.connections.isEmpty()) {
            AppButton(
                text = "打开连接库",
                onClick = { state.setPage(AppPage.CONNECTIONS) },
                modifier = Modifier.padding(top = dimens.spaceXl),
                variant = AppButtonVariant.Primary,
                icon = AppIcons.Connections,
            )
        }
    }
}

@Composable
private fun ConnectionsPlaceholder(state: AppState) {
    val colors = LocalAppColors.current
    val dimensions = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = dimensions.pageMargin,
                    end = dimensions.pageMargin,
                    top = dimensions.space2Xl,
                    bottom = dimensions.space3Xl,
                ),
    ) {
        PageHeading(
            eyebrow = "CONNECTIONS",
            title = "连接库",
            description = "集中管理 Redis 连接。当前阶段先提供空状态与导航，完整表单将在 Phase 8 实现。",
        )
        if (state.connections.isEmpty()) {
            AppCard(modifier = Modifier.padding(top = dimensions.spaceXl), contentPadding = 0.dp) {
                AppEmptyState(
                    icon = AppIcons.Connections,
                    title = "添加你的第一个 Redis 连接",
                    description = "连接信息会保存在 ~/.redis-compose/connections.json。",
                    action = {
                        AppButton(
                            text = "添加你的第一个 Redis 连接",
                            onClick = {
                                state.showToast("新建连接表单将在 Phase 8 提供", ToastKind.Info)
                            },
                            variant = AppButtonVariant.Primary,
                            icon = AppIcons.Add,
                        )
                    },
                )
            }
        } else {
            AppCard(
                modifier = Modifier.padding(top = dimensions.spaceXl),
                title = "已有连接",
                description = "${state.connections.size} 个连接",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(dimensions.spaceSm)) {
                    state.connections.forEach { connection ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(dimensions.radiusXs))
                                    .clickable { state.openConnection(connection.id) }
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .padding(vertical = dimensions.spaceXs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = connection.name,
                                style = typography.bodyStrong,
                                color = colors.text,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${connection.host}:${connection.port}",
                                style = typography.monoMeta,
                                color = colors.textTertiary,
                            )
                        }
                    }
                }
            }
            Text(
                text = "从左侧连接列表或命令面板打开连接；完整的增删改表单将在 Phase 8 提供。",
                style = typography.caption,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = dimensions.spaceMd),
            )
        }
    }
}

@Composable
private fun SettingsPlaceholder(state: AppState) {
    val colors = LocalAppColors.current
    val dimensions = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = dimensions.pageMargin,
                    end = dimensions.pageMargin,
                    top = dimensions.space2Xl,
                    bottom = dimensions.space3Xl,
                ),
    ) {
        PageHeading(
            eyebrow = "SETTINGS",
            title = "设置",
            description = "主题、强调色、编辑器与安全策略。完整设置页将在 Phase 9 实现。",
        )
        AppCard(
            modifier =
                Modifier
                    .padding(top = dimensions.spaceXl)
                    .widthIn(max = dimensions.settingsCardMaxWidth),
            title = "外观",
            description = "当前生效的设置",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(dimensions.fieldPadding)) {
                ReadOnlySetting("主题", state.settings.theme.name)
                ReadOnlySetting("强调色", state.settings.accent.name)
                ReadOnlySetting("密度", state.settings.density.name)
                ReadOnlySetting("编辑器字体", state.settings.editorFont)
                ReadOnlySetting("SCAN 批量", state.settings.scanBatchSize.toString())
            }
            Text(
                text = "主题切换请使用顶栏按钮；设置会立即写入 settings.json。",
                style = typography.caption,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = dimensions.spaceMd),
            )
        }
    }
}

@Composable
private fun ReadOnlySetting(
    label: String,
    value: String,
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = typography.body,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = typography.monoValue,
            color = colors.text,
        )
    }
}

@Composable
private fun PageHeading(
    eyebrow: String,
    title: String,
    description: String,
) {
    val colors = LocalAppColors.current
    val dimensions = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Text(
        text = eyebrow,
        style = typography.microLabel,
        color = colors.accent,
    )
    Text(
        text = title,
        style = typography.pageTitle,
        color = colors.text,
        modifier = Modifier.padding(top = dimensions.spaceXs),
    )
    Text(
        text = description,
        style = typography.body,
        color = colors.textTertiary,
        modifier =
            Modifier
                .padding(top = dimensions.spaceXs + 1.dp)
                .widthIn(max = dimensions.pageDescriptionMaxWidth),
    )
}

@Preview(widthDp = 1120, heightDp = 720)
@Composable
private fun EmptyWorkspacePreview() {
    AppTheme(dark = true) {
        Box(Modifier.fillMaxSize()) {
            AppPageContent(state = previewEmptyState())
        }
    }
}

@Preview(widthDp = 1120, heightDp = 720)
@Composable
private fun SettingsPlaceholderPreview() {
    AppTheme(dark = false) {
        Box(Modifier.fillMaxSize()) {
            AppPageContent(state = previewEmptyState().also { it.setPage(AppPage.SETTINGS) })
        }
    }
}
