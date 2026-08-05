package com.czh.redis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czh.redis.data.AppSettings
import com.czh.redis.ui.components.AppButton
import com.czh.redis.ui.components.AppSelect
import com.czh.redis.ui.components.AppTextField
import com.czh.redis.ui.components.ButtonSize
import com.czh.redis.ui.components.ButtonVariant

@Composable
fun SettingsPage(vm: AppViewModel, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.panel)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "设置", color = colors.text, fontSize = 14.sp)
            Spacer(modifier = Modifier.weight(1f))
            AppButton(
                text = "恢复默认",
                variant = ButtonVariant.GHOST,
                size = ButtonSize.SMALL,
                onClick = { vm.updateSettings(AppSettings()) },
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            SettingsSection(
                title = "外观",
                description = "主题、字体与显示偏好",
            ) {
                SettingRow("应用主题", "浅色 / 深色 / 跟随系统") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeOption("浅色", vm.settings.theme == "light", colors.panel) { vm.updateSettings(vm.settings.copy(theme = "light")) }
                        ThemeOption("深色", vm.settings.theme == "dark", colors.panelAlt) { vm.updateSettings(vm.settings.copy(theme = "dark")) }
                        ThemeOption("跟随系统", vm.settings.theme == "system", colors.tabBg) { vm.updateSettings(vm.settings.copy(theme = "system")) }
                    }
                }
                SettingRow("值/控制台字体", "等宽字体展示") {
                    AppSelect(
                        options = listOf("JetBrains Mono", "SF Mono", "Menlo", "Consolas"),
                        selected = vm.settings.fontFamily,
                        onSelect = { vm.updateSettings(vm.settings.copy(fontFamily = it)) },
                        height = 30.dp,
                        modifier = Modifier.width(200.dp),
                    )
                }
            }

            SettingsSection(
                title = "浏览",
                description = "Key 加载与展示参数",
            ) {
                SettingRow("每批加载数量", "SCAN 单次 COUNT 值") {
                    NumberInput(
                        value = vm.settings.batchSize,
                        onChange = { vm.updateSettings(vm.settings.copy(batchSize = it)) },
                    )
                }
                SettingRow("值截断阈值", "超长值只展示前段") {
                    NumberInput(
                        value = vm.settings.valueTruncateKb,
                        onChange = { vm.updateSettings(vm.settings.copy(valueTruncateKb = it)) },
                    )
                }
                SettingRow("默认视图", "Key 列表初始视图") {
                    AppSelect(
                        options = listOf("tree", "flat"),
                        selected = vm.settings.defaultView,
                        onSelect = { vm.updateSettings(vm.settings.copy(defaultView = it)) },
                        height = 30.dp,
                        modifier = Modifier.width(160.dp),
                    )
                }
            }

            SettingsSection(
                title = "安全",
                description = "全局行为控制",
            ) {
                SwitchSetting(
                    label = "只读模式",
                    description = "全局拦截所有写命令（SET / DEL / FLUSHDB 等）",
                    checked = vm.settings.readonly,
                    onChange = { vm.updateSettings(vm.settings.copy(readonly = it)) },
                )
                SwitchSetting(
                    label = "密码加密存储",
                    description = "连接密码使用 AES 加密后保存到本地",
                    checked = vm.settings.encryptPassword,
                    onChange = { vm.updateSettings(vm.settings.copy(encryptPassword = it)) },
                )
            }

            SettingsSection(
                title = "通用",
                description = "语言与快捷方式",
            ) {
                SettingRow("语言", "") {
                    AppSelect(
                        options = listOf("简体中文", "English"),
                        selected = if (vm.settings.language == "en") "English" else "简体中文",
                        onSelect = { vm.updateSettings(vm.settings.copy(language = if (it == "English") "en" else "zh-CN")) },
                        height = 30.dp,
                        modifier = Modifier.width(200.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(text = title, color = colors.text, fontSize = 16.sp)
        Text(text = description, color = colors.textSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.panel, RoundedCornerShape(AppDimens.cornerLarge)),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    description: String,
    content: @Composable () -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = colors.text, fontSize = 13.sp)
            if (description.isNotEmpty()) {
                Text(text = description, color = colors.textSecondary, fontSize = 11.sp)
            }
        }
        content()
    }
}

@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    previewColor: Color,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .width(84.dp)
            .background(
                if (selected) colors.selectedBg else Color.Transparent,
                RoundedCornerShape(AppDimens.cornerLarge),
            )
            .clickable { onClick() }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(previewColor, RoundedCornerShape(AppDimens.cornerMedium)),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (selected) colors.primary else colors.textSecondary,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun NumberInput(
    value: Int,
    onChange: (Int) -> Unit,
) {
    AppTextField(
        value = value.toString(),
        onValueChange = { onChange(it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0) },
        mono = true,
        height = 30.dp,
        modifier = Modifier.width(120.dp),
    )
}

@Composable
private fun SwitchSetting(
    label: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = colors.text, fontSize = 13.sp)
            Text(text = description, color = colors.textSecondary, fontSize = 11.sp)
        }
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(20.dp)
                .background(if (checked) colors.success else colors.border, RoundedCornerShape(50))
                .padding(2.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(16.dp)
                    .background(Color.White, RoundedCornerShape(50)),
            )
        }
    }
}
