package com.czh.rediscompose.feature.connections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppButton
import com.czh.rediscompose.design.component.AppButtonSize
import com.czh.rediscompose.design.component.AppButtonVariant
import com.czh.rediscompose.design.component.AppIconButton
import com.czh.rediscompose.design.component.AppIconButtonSize
import com.czh.rediscompose.design.component.AppSegmented
import com.czh.rediscompose.design.component.AppSegmentedSize
import com.czh.rediscompose.design.component.AppSheet
import com.czh.rediscompose.design.component.AppSwitch
import com.czh.rediscompose.design.component.AppTextField
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.connection.ConnectionField
import com.czh.rediscompose.domain.connection.ConnectionTestResult
import com.czh.rediscompose.feature.connections.sheet.ConnectionAdvancedTab
import com.czh.rediscompose.feature.connections.sheet.ConnectionBasicTab
import com.czh.rediscompose.feature.connections.sheet.ConnectionSshTab
import com.czh.rediscompose.feature.connections.sheet.ConnectionTlsTab
import kotlinx.coroutines.delay

@Composable
fun ConnectionSheet(state: ConnectionsState) {
    val draft = state.draft ?: return
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val nameFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(draft.id, draft.mode) {
        delay(120)
        runCatching { nameFocusRequester.requestFocus() }
    }
    AppSheet(
        visible = true,
        onDismissRequest = state::closeSheet,
        title = draft.title,
        subtitle = draft.subtitle,
        headerLeading = {
            ConnectionLogo(
                initials = draft.name.initialsFallback(),
                color = draft.color.toColor(colors),
            )
        },
        tabs = {
            AppSegmented(
                options = ConnectionSheetTab.entries,
                selected = state.sheetTab,
                onSelect = state::selectSheetTab,
                size = AppSegmentedSize.Small,
                label = { it.label },
            )
        },
        footerLeading = {
            TestResultText(
                result = state.testResult,
                testing = state.isTesting,
            )
        },
        footer = {
            AppButton(
                text = "取消",
                onClick = state::closeSheet,
                size = AppButtonSize.Small,
                variant = AppButtonVariant.Ghost,
            )
            AppButton(
                text = "测试连接",
                onClick = state::testDraft,
                size = AppButtonSize.Small,
                variant = AppButtonVariant.Secondary,
                icon = AppIcons.Wifi,
                loading = state.isTesting,
            )
            AppButton(
                text = "保存连接",
                onClick = state::save,
                size = AppButtonSize.Small,
                variant = AppButtonVariant.Primary,
                icon = AppIcons.Check,
                enabled = !state.isTesting,
            )
        },
    ) {
        if (state.validationErrors.isNotEmpty()) {
            InfoCallout(
                text = "请修正 ${state.validationErrors.size} 个字段后再保存。",
                tone = InfoTone.Danger,
                modifier = Modifier.padding(bottom = dimens.spaceMd),
            )
        }
        when (state.sheetTab) {
            ConnectionSheetTab.BASIC ->
                ConnectionBasicTab(
                    draft = draft,
                    errors = state.validationErrors,
                    onDraftChange = state::updateDraft,
                    nameFocusRequester = nameFocusRequester,
                )
            ConnectionSheetTab.TLS ->
                ConnectionTlsTab(
                    draft = draft,
                    errors = state.validationErrors,
                    onDraftChange = state::updateDraft,
                )
            ConnectionSheetTab.SSH ->
                ConnectionSshTab(
                    draft = draft,
                    errors = state.validationErrors,
                    onDraftChange = state::updateDraft,
                )
            ConnectionSheetTab.ADVANCED ->
                ConnectionAdvancedTab(
                    draft = draft,
                    errors = state.validationErrors,
                    onDraftChange = state::updateDraft,
                )
        }
    }
}

@Composable
private fun TestResultText(
    result: ConnectionTestResult?,
    testing: Boolean,
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val text =
        when {
            testing -> "测试中…"
            result == null -> ""
            result.success -> "✓ ${ConnectionsState.testSuccessText(result)}"
            else -> "✕ ${result.errorMessage ?: "连接失败"}"
        }
    val color =
        when {
            testing -> colors.textTertiary
            result?.success == true -> colors.success
            result != null -> colors.danger
            else -> colors.textQuaternary
        }
    if (text.isNotEmpty()) {
        Text(
            text = text,
            style = typography.monoMicro,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 210.dp),
        )
    }
}

@Composable
internal fun ConnectionLogo(
    initials: String,
    color: Color,
    size: androidx.compose.ui.unit.Dp = 36.dp,
) {
    val dimens = LocalAppDimens.current
    val shape = RoundedCornerShape(dimens.connectionLogoRadius)
    Box(
        modifier =
            Modifier
                .size(size)
                .background(color.copy(alpha = 0.11f), shape)
                .border(1.dp, color.copy(alpha = 0.22f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = LocalAppTypography.current.monoValue,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
internal fun FormSection(title: String) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceSm, bottom = dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        Text(
            text = title.uppercase(),
            style = typography.microLabel,
            color = colors.textQuaternary,
        )
        Box(Modifier.weight(1f).height(1.dp).background(colors.border))
    }
}

@Composable
internal fun FormField(
    label: String,
    required: Boolean = false,
    error: String? = null,
    hint: String? = null,
    content: @Composable () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Column(Modifier.fillMaxWidth().padding(bottom = dimens.spaceMd)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = label,
                style = typography.tooltip,
                color = colors.textTertiary,
            )
            if (required) {
                Text(
                    text = "*",
                    style = typography.tooltip,
                    color = colors.danger,
                )
            }
        }
        Box(Modifier.padding(top = dimens.spaceXs)) { content() }
        if (error != null) {
            Text(
                text = error,
                style = typography.monoMicro,
                color = colors.danger,
                modifier = Modifier.padding(top = dimens.spaceXs),
            )
        } else if (hint != null) {
            Text(
                text = hint,
                style = typography.monoMicro,
                color = colors.textQuaternary,
                modifier = Modifier.padding(top = dimens.spaceXs),
            )
        }
    }
}

@Composable
internal fun ToggleRow(
    title: String,
    hint: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = dimens.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = typography.bodySmall,
                color = if (enabled) colors.text else colors.textTertiary,
            )
            Text(
                text = hint,
                style = typography.monoMicro,
                color = colors.textQuaternary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        AppSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

internal enum class InfoTone { Info, Warning, Danger }

@Composable
internal fun InfoCallout(
    text: String,
    tone: InfoTone,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val color =
        when (tone) {
            InfoTone.Info -> colors.info
            InfoTone.Warning -> colors.warning
            InfoTone.Danger -> colors.danger
        }
    val shape = RoundedCornerShape(dimens.radiusSm)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.09f), shape)
                .border(1.dp, color.copy(alpha = 0.22f), shape)
                .padding(horizontal = dimens.spaceSm + 2.dp, vertical = dimens.spaceSm),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        Icon(
            imageVector = if (tone == InfoTone.Info) AppIcons.Info else AppIcons.Warning,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(dimens.iconSm),
        )
        Text(
            text = text,
            style = typography.tooltip,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String? = null,
    enabled: Boolean = true,
) {
    var visible by remember { mutableStateOf(false) }
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        isPassword = !visible,
        isError = error != null,
        enabled = enabled,
        trailingContent = {
            AppIconButton(
                icon = if (visible) AppIcons.EyeOff else AppIcons.Eye,
                contentDescription = if (visible) "隐藏密码" else "显示密码",
                onClick = { visible = !visible },
                size = AppIconButtonSize.Small,
                enabled = enabled,
            )
        },
    )
}

internal fun String.initialsFallback(): String {
    val trimmed = trim()
    if (trimmed.isEmpty()) return "NC"
    val words = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        words.size >= 2 -> words.take(2).map { it.first() }.joinToString("").uppercase()
        else -> words.first().take(2).uppercase()
    }
}

/** Used by the sheet tabs to look up a field error. */
internal fun Map<ConnectionField, String>.error(field: ConnectionField): String? = this[field]
