package com.czh.redis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czh.redis.ui.components.AppButton
import com.czh.redis.ui.components.ButtonSize
import com.czh.redis.ui.components.ButtonVariant

@Composable
fun WorkspaceTabBar(
    vm: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(colors.tabBg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        vm.tabs.forEach { tab ->
            val active = vm.activeTabId == tab.id
            Row(
                modifier =
                    Modifier
                        .height(38.dp)
                        .background(if (active) colors.panel else colors.tabBg)
                        .clickable { vm.activeTabId = tab.id }
                        .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = tab.title,
                    color = if (active) colors.text else colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier =
                        Modifier
                            .size(16.dp)
                            .clickable { vm.closeTab(tab.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "×",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        AppButton(
            text = "＋ 控制台",
            variant = ButtonVariant.GHOST,
            size = ButtonSize.SMALL,
            onClick = {
                val connection = vm.sessions.keys.firstOrNull()
                if (connection != null) vm.openTab(connection, TabType.CONSOLE)
            },
        )
        Spacer(modifier = Modifier.width(4.dp))
        AppButton(
            text = "⚙ 设置",
            variant = ButtonVariant.GHOST,
            size = ButtonSize.SMALL,
            onClick = { vm.openSettings() },
        )
        Spacer(modifier = Modifier.width(4.dp))
        AppButton(
            text = if (vm.settings.theme == "dark") "☀ 浅色" else "🌙 深色",
            variant = ButtonVariant.GHOST,
            size = ButtonSize.SMALL,
            onClick = {
                val next = if (vm.settings.theme == "dark") "light" else "dark"
                vm.updateSettings(vm.settings.copy(theme = next))
            },
        )
        Spacer(modifier = Modifier.width(8.dp))
    }
}
