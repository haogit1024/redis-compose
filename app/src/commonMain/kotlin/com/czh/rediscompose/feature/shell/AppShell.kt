package com.czh.rediscompose.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalWindowInfo
import com.czh.rediscompose.app.AppPage
import com.czh.rediscompose.app.AppState
import com.czh.rediscompose.design.component.AppToastHost
import com.czh.rediscompose.design.component.ToastData
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalTextInputFocus

@Composable
fun AppShell(
    state: AppState,
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val textInputFocus = LocalTextInputFocus.current
    val windowInfo = LocalWindowInfo.current
    val rootFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { rootFocus.requestFocus() }
    LaunchedEffect(windowInfo.isWindowFocused) {
        state.setAppActive(windowInfo.isWindowFocused)
    }
    LaunchedEffect(state.commandPaletteVisible) {
        if (!state.commandPaletteVisible) rootFocus.requestFocus()
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.background)
                .focusRequester(rootFocus)
                .focusable()
                .globalShortcuts(
                    state = state,
                    dark = dark,
                    isTextInputFocused = { textInputFocus.isTextInputFocused },
                ),
    ) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(state = state, dark = dark)
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
            ) {
                AppRail(state = state)
                if (state.activePage == AppPage.WORKSPACE) {
                    ConnectionSidebar(state = state)
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                    ) {
                        WorkspaceTabBar(state = state)
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                        ) {
                            AppPageContent(state = state)
                        }
                    }
                } else {
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                    ) {
                        AppPageContent(state = state)
                    }
                }
            }
            AppStatusBar(state = state)
        }

        CommandPalette(
            visible = state.commandPaletteVisible,
            state = state,
            dark = dark,
            onDismiss = { state.setPaletteVisible(false) },
        )

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = dimens.statusBarHeight),
        ) {
            AppToastHost(
                toasts =
                    state.toasts.map { toast ->
                        ToastData(id = toast.id, message = toast.message, kind = toast.kind)
                    },
                onDismiss = state::dismissToast,
            )
        }
    }
}
