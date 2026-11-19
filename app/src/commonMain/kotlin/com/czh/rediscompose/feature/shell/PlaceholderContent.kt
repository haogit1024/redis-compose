package com.czh.rediscompose.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.czh.rediscompose.app.AppPage
import com.czh.rediscompose.app.AppState
import com.czh.rediscompose.app.WorkspaceTabKind
import com.czh.rediscompose.design.component.AppButton
import com.czh.rediscompose.design.component.AppButtonVariant
import com.czh.rediscompose.design.component.AppLoading
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.icon.BrandMark
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.feature.connections.ConnectionsScreen
import com.czh.rediscompose.feature.console.ConsoleScreen
import com.czh.rediscompose.feature.insights.InsightsScreen
import com.czh.rediscompose.feature.settings.SettingsScreen
import com.czh.rediscompose.feature.workspace.WorkspaceScreen
import com.czh.rediscompose.util.ShortcutLabels

@Composable
fun AppPageContent(
    state: AppState,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(LocalAppColors.current.background)) {
        when (state.activePage) {
            AppPage.WORKSPACE -> WorkspaceContent(state)
            AppPage.CONNECTIONS -> ConnectionsScreen(state = state.connectionLibrary)
            AppPage.SETTINGS ->
                SettingsScreen(
                    settings = state.settings,
                    saveStatus = state.settingsSaveStatus,
                    onSettingsChange = state::updateSettings,
                    onClearLocalCache = state::clearLocalCache,
                    onToast = { message, kind -> state.showToast(message, kind) },
                    requestedSection = state.requestedSettingsSection,
                    onSectionRequestConsumed = state::consumeSettingsSectionRequest,
                )
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
                    "还没有连接。打开连接库查看连接配置，或使用 ${ShortcutLabels.commandKey("K")} 打开命令面板。"
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
private fun SettingsPagePreview() {
    AppTheme(dark = false) {
        Box(Modifier.fillMaxSize()) {
            AppPageContent(state = previewEmptyState().also { it.setPage(AppPage.SETTINGS) })
        }
    }
}
