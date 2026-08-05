package com.czh.redis

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czh.redis.ui.AppTheme
import com.czh.redis.ui.AppViewModel
import com.czh.redis.ui.ConnectionPanel
import com.czh.redis.ui.KeyBrowserPage
import com.czh.redis.ui.LocalAppColors
import com.czh.redis.ui.NewConnectionDialog
import com.czh.redis.ui.NewKeyDialog
import com.czh.redis.ui.ServerInfoPage
import com.czh.redis.ui.SettingsPage
import com.czh.redis.ui.StatusBar
import com.czh.redis.ui.TabType
import com.czh.redis.ui.WorkspaceTabBar
import com.czh.redis.ui.ConsolePage
import com.czh.redis.ui.resizeHPointerIcon
import com.czh.redis.ui.components.ConfirmDialog
import com.czh.redis.ui.components.ContextMenuHost
import com.czh.redis.ui.components.RenameDialog
import com.czh.redis.ui.components.ToastHost
import com.czh.redis.ui.components.TtlEditDialog

@Composable
@Preview
fun App() {
    val isSystemDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val vm = remember { AppViewModel(scope) }
    val dark = vm.settings.isDark(isSystemDark)

    AppTheme(dark = dark) {
        AppRoot(vm)
    }
}

@Composable
fun AppRoot(vm: AppViewModel) {
    val colors = LocalAppColors.current
    var leftPanelWidth by remember { mutableStateOf(280f) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            WorkspaceTabBar(vm)

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                ConnectionPanel(
                    vm = vm,
                    modifier = Modifier
                        .width(leftPanelWidth.dp)
                        .fillMaxHeight(),
                )

                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(colors.border)
                        .pointerHoverIcon(resizeHPointerIcon())
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val deltaDp = with(density) { dragAmount.x.toDp().value }
                                leftPanelWidth = (leftPanelWidth + deltaDp).coerceIn(200f, 480f)
                            }
                        },
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    WorkspaceContent(vm)
                }
            }

            StatusBar(vm)
        }

        ToastHost(
            toasts = vm.toasts.toList(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
        ContextMenuHost(vm.contextMenu)

        NewConnectionDialog(vm)
        NewKeyDialog(vm, vm.activeBrowser()?.session)
        ConfirmDialog(
            request = vm.confirmRequest,
            onDismiss = { vm.confirmRequest = null },
        )
        vm.renameRequest?.let { key ->
            RenameDialog(
                title = "重命名 Key",
                initialName = key,
                onConfirm = { newName ->
                    vm.renameKeyTo(key, newName)
                    vm.renameRequest = null
                },
                onDismiss = { vm.renameRequest = null },
            )
        }
        vm.ttlRequest?.let { key ->
            TtlEditDialog(
                title = "修改 TTL",
                initialTtl = vm.ttlRequestInitial,
                onConfirm = { ttl ->
                    vm.setTtl(key, ttl)
                    vm.ttlRequest = null
                },
                onDismiss = { vm.ttlRequest = null },
            )
        }
    }
}

@Composable
fun EmptyWorkspace() {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .background(colors.primary, androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
            ) {
                Text(
                    text = "R",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 18.dp).align(Alignment.Center),
                )
            }
            Text(
                text = "从左侧选择一个连接开始",
                color = colors.textSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

@Composable
fun WorkspaceContent(vm: AppViewModel) {
    val activeTab = vm.tabs.firstOrNull { it.id == vm.activeTabId }
    if (activeTab == null) {
        EmptyWorkspace()
        return
    }
    when (activeTab.type) {
        TabType.KEYS -> {
            val browser = vm.keyBrowsers[activeTab.id]
            val session = activeTab.connectionId?.let { vm.sessions[it] }
            if (browser == null || session == null) {
                ConnectingWorkspace(vm, activeTab)
                return
            }
            KeyBrowserPage(vm, browser, session)
        }
        TabType.CONSOLE -> {
            val console = vm.consoles[activeTab.id]
            if (console == null) {
                EmptyWorkspace()
                return
            }
            ConsolePage(console)
        }
        TabType.SERVER_INFO -> {
            val info = vm.serverInfos[activeTab.id]
            if (info == null) {
                EmptyWorkspace()
                return
            }
            ServerInfoPage(info)
        }
        TabType.SETTINGS -> SettingsPage(vm)
    }
}

@Composable
fun ConnectingWorkspace(vm: AppViewModel, tab: com.czh.redis.ui.WorkspaceTab) {
    val colors = LocalAppColors.current
    val session = tab.connectionId?.let { vm.sessions[it] }
    val name = tab.connectionId?.let { vm.connectionById(it)?.name } ?: "连接"
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "正在连接 $name ...", color = colors.textSecondary, fontSize = 13.sp)
            if (session?.status == com.czh.redis.ui.ConnectionStatus.ERROR) {
                Text(
                    text = session.statusError?.lineSequence()?.firstOrNull() ?: "连接失败",
                    color = colors.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
