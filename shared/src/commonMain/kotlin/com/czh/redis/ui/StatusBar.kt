package com.czh.redis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czh.redis.ui.components.formatCount

@Composable
fun StatusBar(
    vm: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val activeTab = vm.tabs.firstOrNull { it.id == vm.activeTabId }
    val session = activeTab?.connectionId?.let { vm.sessions[it] }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(colors.panel)
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (session != null && session.status == ConnectionStatus.CONNECTED) {
            Text(text = "●", color = colors.success, fontSize = 10.sp)
            Text(text = "已连接", color = colors.text, fontSize = 11.sp)
            Text(
                text = "${session.connection.host}:${session.connection.port}",
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
            Text(text = "db${session.currentDb}", color = colors.textSecondary, fontSize = 11.sp)
            val browser = vm.keyBrowsers.values.firstOrNull { it.session.connectionId == session.connectionId }
            if (browser != null) {
                Text(
                    text = "已加载 ${formatCount(browser.keys.size.toLong())} / ${formatCount(browser.total)} keys",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
            if (session.isReadonly || vm.settings.readonly) {
                Text(text = "只读", color = colors.warn, fontSize = 11.sp)
            }
        } else {
            Text(text = "未连接", color = colors.textSecondary, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(text = "RedisCompose", color = colors.textSecondary, fontSize = 11.sp)
    }
}
