package com.czh.rediscompose.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.app.SettingsSaveStatus
import com.czh.rediscompose.design.component.AppBadge
import com.czh.rediscompose.design.component.AppBadgeTone
import com.czh.rediscompose.design.component.AppButton
import com.czh.rediscompose.design.component.AppButtonSize
import com.czh.rediscompose.design.component.AppButtonVariant
import com.czh.rediscompose.design.component.AppDialog
import com.czh.rediscompose.design.component.AppVerticalScrollbar
import com.czh.rediscompose.design.component.ToastKind
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.settings.Accent
import com.czh.rediscompose.domain.settings.AppSettings
import com.czh.rediscompose.domain.settings.ThemeMode
import com.czh.rediscompose.domain.settings.UiDensity
import com.czh.rediscompose.feature.settings.sections.AboutSection
import com.czh.rediscompose.feature.settings.sections.AppearanceSection
import com.czh.rediscompose.feature.settings.sections.BrowserSection
import com.czh.rediscompose.feature.settings.sections.EditorSection
import com.czh.rediscompose.feature.settings.sections.SecuritySection
import com.czh.rediscompose.feature.settings.sections.ShortcutsSection
import kotlinx.coroutines.launch

/**
 * Full settings page from `design/settings.html`.
 *
 * Every control writes through [onSettingsChange] immediately; persistence and the
 * auto-saved indicator are owned by `AppState`.
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    saveStatus: SettingsSaveStatus,
    onSettingsChange: (AppSettings) -> Unit,
    onClearLocalCache: () -> Unit,
    onToast: (String, ToastKind) -> Unit,
    modifier: Modifier = Modifier,
    requestedSection: SettingsSection? = null,
    onSectionRequestConsumed: () -> Unit = {},
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val state = remember { SettingsState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val scrollToSection: (SettingsSection) -> Unit = { section ->
        state.select(section)
        scope.launch { listState.animateScrollToItem(section.ordinal + 1) }
    }

    LaunchedEffect(requestedSection) {
        val target = requestedSection ?: return@LaunchedEffect
        state.select(target)
        listState.scrollToItem(target.ordinal + 1)
        onSectionRequestConsumed()
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { index ->
            val current =
                when {
                    index <= 0 -> SettingsSection.APPEARANCE
                    index > SettingsSection.entries.size -> SettingsSection.ABOUT
                    else -> SettingsSection.entries[index - 1]
                }
            if (current != state.activeSection) state.select(current)
        }
    }

    Row(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.background),
    ) {
        SettingsNav(active = state.activeSection, onSelect = scrollToSection)
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
        ) {
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(
                            max =
                                dimens.settings.contentMaxWidth +
                                    dimens.settings.contentPaddingH * 2,
                        )
                        .fillMaxWidth(),
                contentPadding =
                    PaddingValues(
                        start = dimens.settings.contentPaddingH,
                        end = dimens.settings.contentPaddingH,
                        top = dimens.settings.contentPaddingTop,
                        bottom = dimens.settings.contentPaddingBottom,
                    ),
            ) {
                item(key = "header") { SettingsHeader(saveStatus = saveStatus) }
                items(SettingsSection.entries, key = { it.name }) { section ->
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = dimens.settings.sectionGap),
                    ) {
                        SectionContent(
                            section = section,
                            settings = settings,
                            onSettingsChange = onSettingsChange,
                            onClearCacheRequest = state::requestClearCache,
                            onToast = onToast,
                        )
                    }
                }
                item(key = "footer") {
                    SettingsFooter(saveStatus = saveStatus)
                    Spacer(Modifier.height(dimens.spaceLg))
                }
            }
            AppVerticalScrollbar(
                scrollState = listState,
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(top = dimens.settings.contentPaddingTop, bottom = dimens.spaceLg),
            )
        }
    }

    AppDialog(
        visible = state.clearCacheDialogVisible,
        onDismissRequest = state::dismissClearCache,
        title = "清除本地缓存？",
        subtitle = "该操作不会影响 Redis 服务器或已保存的连接配置。",
        footer = {
            AppButton(
                text = "取消",
                onClick = state::dismissClearCache,
                variant = AppButtonVariant.Ghost,
                size = AppButtonSize.Small,
            )
            AppButton(
                text = "清除缓存",
                onClick = {
                    state.dismissClearCache()
                    onClearLocalCache()
                },
                variant = AppButtonVariant.Danger,
                size = AppButtonSize.Small,
                icon = AppIcons.Delete,
            )
        },
    ) {
        Text(
            text = "将删除命令历史、最近命令、Key 筛选条件与本次会话的洞察指标样本。连接与设置会保留。",
            style = LocalAppTypography.current.body,
            color = LocalAppColors.current.textSecondary,
        )
    }
}

@Composable
private fun SectionContent(
    section: SettingsSection,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onClearCacheRequest: () -> Unit,
    onToast: (String, ToastKind) -> Unit,
) {
    when (section) {
        SettingsSection.APPEARANCE -> AppearanceSection(settings = settings, onSettingsChange = onSettingsChange)
        SettingsSection.EDITOR -> EditorSection(settings = settings, onSettingsChange = onSettingsChange)
        SettingsSection.BROWSER -> BrowserSection(settings = settings, onSettingsChange = onSettingsChange)
        SettingsSection.SECURITY ->
            SecuritySection(
                settings = settings,
                onSettingsChange = onSettingsChange,
                onClearCacheRequest = onClearCacheRequest,
            )
        SettingsSection.SHORTCUTS -> ShortcutsSection()
        SettingsSection.ABOUT -> AboutSection(onToast = onToast)
    }
}

@Composable
private fun SettingsHeader(saveStatus: SettingsSaveStatus) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = dimens.spaceXl),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceLg),
    ) {
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.iconTextGap),
            ) {
                Icon(
                    imageVector = AppIcons.Settings,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(dimens.iconXs),
                )
                Text(
                    text = "PREFERENCES",
                    style = typography.microLabel,
                    color = colors.accent,
                )
            }
            Text(
                text = "偏好设置",
                style = typography.pageTitle,
                color = colors.text,
                modifier = Modifier.padding(top = dimens.spaceXs),
            )
            Text(
                text = "按你的习惯调整界面、编辑器与安全策略。修改会即时生效。",
                style = typography.body,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = dimens.spaceXs),
            )
        }
        SaveStateBadge(saveStatus = saveStatus)
    }
}

@Composable
private fun SaveStateBadge(saveStatus: SettingsSaveStatus) {
    val (text, tone, icon) =
        when (saveStatus) {
            SettingsSaveStatus.SAVED -> Triple("已自动保存", AppBadgeTone.Success, AppIcons.Check)
            SettingsSaveStatus.SAVING -> Triple("保存中…", AppBadgeTone.Warning, AppIcons.Refresh)
            SettingsSaveStatus.FAILED -> Triple("未保存", AppBadgeTone.Danger, AppIcons.ErrorCircle)
        }
    AppBadge(text = text, tone = tone, icon = icon)
}

@Composable
private fun SettingsFooter(saveStatus: SettingsSaveStatus) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val (icon, text, color) =
        when (saveStatus) {
            SettingsSaveStatus.SAVED -> Triple(AppIcons.Check, "所有更改已保存 · 设置实时写入本地", colors.success)
            SettingsSaveStatus.SAVING -> Triple(AppIcons.Refresh, "正在保存更改…", colors.warning)
            SettingsSaveStatus.FAILED -> Triple(AppIcons.ErrorCircle, "保存失败，请检查本地文件权限", colors.danger)
        }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(dimens.iconXs),
        )
        Text(
            text = text,
            style = typography.caption,
            color = color,
            modifier = Modifier.padding(start = dimens.spaceXs + 1.dp),
        )
    }
}

@Preview(widthDp = 1120, heightDp = 720)
@Composable
private fun SettingsScreenPreview() {
    AppTheme(dark = true) {
        var settings by remember {
            mutableStateOf(
                AppSettings(
                    theme = ThemeMode.DARK,
                    accent = Accent.IRIS,
                    density = UiDensity.COMFORTABLE,
                ),
            )
        }
        SettingsScreen(
            settings = settings,
            saveStatus = SettingsSaveStatus.SAVED,
            onSettingsChange = { settings = it },
            onClearLocalCache = {},
            onToast = { _, _ -> },
        )
    }
}

@Preview(widthDp = 1120, heightDp = 720)
@Composable
private fun SettingsScreenLightPreview() {
    AppTheme(dark = false) {
        SettingsScreen(
            settings = AppSettings(theme = ThemeMode.LIGHT, accent = Accent.MINT, density = UiDensity.COMPACT),
            saveStatus = SettingsSaveStatus.SAVING,
            onSettingsChange = {},
            onClearLocalCache = {},
            onToast = { _, _ -> },
        )
    }
}
